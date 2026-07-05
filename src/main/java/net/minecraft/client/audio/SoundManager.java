package net.minecraft.client.audio;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import io.netty.util.internal.ThreadLocalRandom;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.openal.*;

import static org.lwjgl.openal.AL10.*;

public class SoundManager implements IResourceManagerReloadListener {
    private static final Marker LOG_MARKER = MarkerManager.getMarker("SOUNDS");
    private static final Logger logger = LogManager.getLogger();
    private final SoundHandler sndHandler;
    private final GameSettings options;
    private SoundSystemStarterThread sndSystem;
    private boolean loaded;
    private int playTime = 0;

    private final ConcurrentMap<String, ISound> playingSounds = Maps.newConcurrentMap();
    private final BiMap<String, ISound> playingSoundsBiMap = HashBiMap.create();
    private final Map<ISound, String> invPlayingSounds = playingSoundsBiMap.inverse();
    private final ConcurrentMap<ISound, SoundPoolEntry> playingSoundPoolEntries = Maps.newConcurrentMap();
    private final Multimap<SoundCategory, String> categorySounds = HashMultimap.create();
    private final CopyOnWriteArrayList<ITickableSound> tickableSounds = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<ISound, Integer> delayedSounds = Maps.newConcurrentMap();
    private final ConcurrentMap<String, SoundBuffer> soundBuffers = Maps.newConcurrentMap();

    public SoundManager(SoundHandler soundHandler, GameSettings gameSettings) {
        this.sndHandler = soundHandler;
        this.options = gameSettings;
    }

    public void reloadSoundSystem() {
        unloadSoundSystem();
        loadSoundSystem();
    }

    private synchronized void loadSoundSystem() {
        if (!loaded) {
            try {
                SoundSystemOpenAL.create();
                sndSystem = new SoundSystemStarterThread();
                loaded = true;
                sndSystem.setMasterVolume(options.getSoundLevel(SoundCategory.MASTER));
                logger.info(LOG_MARKER, "Sound engine started");
            } catch (RuntimeException e) {
                logger.error(LOG_MARKER, "Error starting SoundSystem", e);
                options.setSoundLevel(SoundCategory.MASTER, 0.0F);
                options.saveOptions();
            }
        }
    }

    private float getSoundCategoryVolume(SoundCategory category) {
        return (category == null || category == SoundCategory.MASTER) ? 1.0F : options.getSoundLevel(category);
    }

    public void setSoundCategoryVolume(SoundCategory category, float volume) {
        if (loaded && category != null) {
            if (category == SoundCategory.MASTER) {
                sndSystem.setMasterVolume(volume);
            } else {
                synchronized(categorySounds) {
                    for (String s : categorySounds.get(category)) {
                        ISound isound = playingSounds.get(s);
                        if (isound != null) {
                            float f = getNormalizedVolume(isound, playingSoundPoolEntries.get(isound), category);
                            sndSystem.setVolume(s, f);
                        }
                    }
                }
            }
        }
    }

    public void unloadSoundSystem() {
        if (loaded) {
            stopAllSounds();
            sndSystem.cleanup();
            loaded = false;
        }
    }

    public void stopAllSounds() {
        if (loaded) {
            for (String s : playingSounds.keySet()) {
                sndSystem.stop(s);
            }
            playingSounds.clear();
            playingSoundsBiMap.clear();
            delayedSounds.clear();
            tickableSounds.clear();
            synchronized(categorySounds) {
                categorySounds.clear();
            }
            playingSoundPoolEntries.clear();

            for (SoundBuffer buffer : soundBuffers.values()) {
                alDeleteBuffers(buffer.getBufferID());
            }
            soundBuffers.clear();
        }
    }

    public void updateAllSounds() {
        if (!loaded) return;
        ++playTime;

        Iterator<ITickableSound> tickableIterator = tickableSounds.iterator();
        while (tickableIterator.hasNext()) {
            ITickableSound sound = tickableIterator.next();
            sound.update();
            if (sound.isDonePlaying()) {
                stopSound(sound);
            } else {
                String s = invPlayingSounds.get(sound);
                if (s != null) {
                    SoundEventAccessorComposite soundEvent = sndHandler.getSound(sound.getSoundLocation());
                    if (soundEvent != null) {
                        float volume = getNormalizedVolume(sound, playingSoundPoolEntries.get(sound), soundEvent.getSoundCategory());
                        float pitch = getNormalizedPitch(sound, playingSoundPoolEntries.get(sound));
                        sndSystem.setVolume(s, volume);
                        sndSystem.setPitch(s, pitch);
                        sndSystem.setPosition(s, sound.getXPosF(), sound.getYPosF(), sound.getZPosF());
                    }
                }
            }
        }

        Iterator<Entry<String, ISound>> playingIterator = playingSounds.entrySet().iterator();
        while (playingIterator.hasNext()) {
            Entry<String, ISound> entry = playingIterator.next();
            String sourceName = entry.getKey();
            ISound sound = entry.getValue();

            if (!sndSystem.playing(sourceName)) {
                if (sound.canRepeat() && sound.getRepeatDelay() > 0) {
                    delayedSounds.put(sound, this.playTime + sound.getRepeatDelay());
                }

                playingIterator.remove();
                playingSoundsBiMap.remove(sourceName);
                playingSoundPoolEntries.remove(sound);

                SoundEventAccessorComposite soundEvent = sndHandler.getSound(sound.getSoundLocation());
                if (soundEvent != null) {
                    synchronized(categorySounds) {
                        categorySounds.remove(soundEvent.getSoundCategory(), sourceName);
                    }
                }
                if (sound instanceof ITickableSound) tickableSounds.remove(sound);

                SoundPoolEntry poolEntry = playingSoundPoolEntries.get(sound);
                if (poolEntry != null) {
                    releaseBuffer(poolEntry.getSoundPoolEntryLocation());
                }
                sndSystem.removeSource(sourceName);
            }
        }

        Iterator<Entry<ISound, Integer>> delayedIterator = delayedSounds.entrySet().iterator();
        while (delayedIterator.hasNext()) {
            Entry<ISound, Integer> entry = delayedIterator.next();
            if (playTime >= entry.getValue()) {
                ISound sound = entry.getKey();
                if (sound instanceof ITickableSound) {
                    ((ITickableSound) sound).update();
                }
                playSound(sound);
                delayedIterator.remove();
            }
        }
    }

    public boolean isSoundPlaying(ISound sound) {
        return loaded && sound != null && invPlayingSounds.containsKey(sound);
    }

    public void stopSound(ISound sound) {
        if (loaded && sound != null) {
            String sourceName = invPlayingSounds.get(sound);
            if (sourceName != null) {
                sndSystem.stop(sourceName);
                sndSystem.removeSource(sourceName);

                playingSounds.remove(sourceName);
                playingSoundsBiMap.remove(sourceName);
                playingSoundPoolEntries.remove(sound);

                SoundEventAccessorComposite soundEvent = sndHandler.getSound(sound.getSoundLocation());
                if (soundEvent != null) {
                    synchronized(categorySounds) {
                        categorySounds.remove(soundEvent.getSoundCategory(), sourceName);
                    }
                }

                tickableSounds.remove(sound);

                SoundPoolEntry poolEntry = playingSoundPoolEntries.get(sound);
                if (poolEntry != null) {
                    releaseBuffer(poolEntry.getSoundPoolEntryLocation());
                }
            }
        }
    }

    public void playSound(ISound sound) {
        if (!loaded || sound == null || sndSystem.getMasterVolume() <= 0.0F) return;

        SoundEventAccessorComposite soundEvent = sndHandler.getSound(sound.getSoundLocation());
        if (soundEvent == null) {
            logger.warn(LOG_MARKER, "Unable to play unknown soundEvent: {}", sound.getSoundLocation());
            return;
        }

        SoundPoolEntry soundEntry = soundEvent.cloneEntry();
        if (soundEntry == SoundHandler.missing_sound) {
            logger.warn(LOG_MARKER, "Unable to play empty soundEvent: {}", sound.getSoundLocation());
            return;
        }

        ResourceLocation bufferLocation = soundEntry.getSoundPoolEntryLocation();
        SoundBuffer soundBuffer = getOrLoadBuffer(bufferLocation);

        if (soundBuffer == null) {
            logger.warn(LOG_MARKER, "Failed to load buffer for sound: {}", bufferLocation);
            return;
        }

        float volume = getNormalizedVolume(sound, soundEntry, soundEvent.getSoundCategory());
        float pitch = getNormalizedPitch(sound, soundEntry);
        boolean looping = sound.canRepeat() && sound.getRepeatDelay() == 0;

        String sourceName = MathHelper.getRandomUuid(ThreadLocalRandom.current()).toString();
        sndSystem.newSource(false, sourceName, null, bufferLocation.toString(), looping, sound.getXPosF(), sound.getYPosF(), sound.getZPosF(), sound.getAttenuationType().getTypeInt(), volume, false);

        if (sndSystem.soundSources.containsKey(sourceName)) {
            sndSystem.setPitch(sourceName, pitch);
            sndSystem.setVolume(sourceName, volume);

            alSourcei(sndSystem.soundSources.get(sourceName), AL_BUFFER, soundBuffer.getBufferID());
            sndSystem.play(sourceName);

            soundBuffer.acquire();

            playingSounds.put(sourceName, sound);
            playingSoundsBiMap.put(sourceName, sound);
            playingSoundPoolEntries.put(sound, soundEntry);

            if (soundEvent.getSoundCategory() != SoundCategory.MASTER) {
                synchronized(categorySounds) {
                    categorySounds.put(soundEvent.getSoundCategory(), sourceName);
                }
            }

            if (sound instanceof ITickableSound) {
                tickableSounds.add((ITickableSound) sound);
            }
        }
    }

    private SoundBuffer getOrLoadBuffer(ResourceLocation location) {
        SoundBuffer buffer = soundBuffers.get(location.toString());
        if (buffer != null) {
            return buffer;
        }

        synchronized (soundBuffers) {
            buffer = soundBuffers.get(location.toString());
            if (buffer != null) {
                return buffer;
            }

            try (InputStream is = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream()) {
                byte[] soundBytes = IOUtils.toByteArray(is);
                ByteBuffer vorbisData = ByteBuffer.allocateDirect(soundBytes.length);
                vorbisData.put(soundBytes);
                vorbisData.flip();

                int bufferId = OggDecoder.loadOgg(vorbisData);
                if (bufferId != 0) {
                    SoundBuffer newBuffer = new SoundBuffer(bufferId);
                    soundBuffers.put(location.toString(), newBuffer);
                    return newBuffer;
                }
            } catch (IOException e) {
                logger.warn(LOG_MARKER, "Could not load sound icon", e);
            }
        }
        return null;
    }

    private void releaseBuffer(ResourceLocation location) {
        synchronized (soundBuffers) {
            SoundBuffer buffer = soundBuffers.get(location.toString());
            if (buffer != null) {
                buffer.release();
                if (buffer.isUnused()) {
                    alDeleteBuffers(buffer.getBufferID());
                    soundBuffers.remove(location.toString());
                }
            }
        }
    }

    private float getNormalizedPitch(ISound sound, SoundPoolEntry entry) {
        return (float) MathHelper.clamp_double(sound.getPitch() * entry.getPitch(), 0.5D, 2.0D);
    }

    private float getNormalizedVolume(ISound sound, SoundPoolEntry entry, SoundCategory category) {
        return (float) MathHelper.clamp_double(sound.getVolume() * entry.getVolume(), 0.0D, 1.0D) * getSoundCategoryVolume(category);
    }

    public void pauseAllSounds() {
        if (loaded) {
            for (String s : playingSounds.keySet()) {
                sndSystem.pause(s);
            }
        }
    }

    public void resumeAllSounds() {
        if (loaded) {
            for (String s : playingSounds.keySet()) {
                sndSystem.play(s);
            }
        }
    }

    public void playDelayedSound(ISound sound, int delay) {
        if (loaded && sound != null) {
            delayedSounds.put(sound, playTime + delay);
        }
    }

    public void setListener(EntityPlayer player, float renderPartialTicks) {
        if (loaded && player != null) {
            float f = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * renderPartialTicks;
            float f1 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * renderPartialTicks;
            double x = player.prevPosX + (player.posX - player.prevPosX) * (double)renderPartialTicks;
            double y = player.prevPosY + (player.posY - player.prevPosY) * (double)renderPartialTicks + (double)player.getEyeHeight();
            double z = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)renderPartialTicks;
            float f2 = MathHelper.cos((f1 + 90.0F) * 0.017453292F);
            float f3 = MathHelper.sin((f1 + 90.0F) * 0.017453292F);
            float f4 = MathHelper.cos(-f * 0.017453292F);
            float atY = MathHelper.sin(-f * 0.017453292F);
            float f6 = MathHelper.cos((-f + 90.0F) * 0.017453292F);
            float upY = MathHelper.sin((-f + 90.0F) * 0.017453292F);
            float atX = f2 * f4;
            float atZ = f3 * f4;
            float upX = f2 * f6;
            float upZ = f3 * f6;
            sndSystem.setListenerPosition((float) x, (float) y, (float) z);
            sndSystem.setListenerOrientation(atX, atY, atZ, upX, upY, upZ);
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        unloadSoundSystem();
        loadSoundSystem();
    }

    private static class SoundBuffer {
        private final int bufferID;
        private int referenceCount;

        public SoundBuffer(int bufferId) {
            this.bufferID = bufferId;
            this.referenceCount = 0;
        }

        public int getBufferID() {
            return bufferID;
        }

        public synchronized void acquire() {
            this.referenceCount++;
        }

        public synchronized void release() {
            this.referenceCount--;
        }

        public synchronized boolean isUnused() {
            return this.referenceCount <= 0;
        }
    }

    static class SoundSystemStarterThread extends SoundSystemOpenAL {}
}