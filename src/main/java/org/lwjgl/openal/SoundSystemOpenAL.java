package org.lwjgl.openal;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.ALC_MONO_SOURCES;
import static org.lwjgl.openal.ALC11.ALC_STEREO_SOURCES;
import static org.lwjgl.openal.EXTEfx.*;
import static org.lwjgl.openal.EXTSourceDistanceModel.AL_SOURCE_DISTANCE_MODEL;
import static org.lwjgl.openal.SOFTHRTF.ALC_HRTF_SOFT;

public class SoundSystemOpenAL {
    public static long audioContext;
    public static long audioDevice;
    public final Map<String, Integer> soundSources = new ConcurrentHashMap<>();
    private final List<Integer> freeSources = new CopyOnWriteArrayList<>();
    private static final int MAX_SOURCES = 255;

    public static void create() {
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = alcOpenDevice(defaultDeviceName);
        if (audioDevice == 0L) throw new RuntimeException("Failed to open OpenAL device");

        int[] attribs = {
                ALC_FREQUENCY, 44100,
                ALC_MONO_SOURCES, 255,
                ALC_STEREO_SOURCES, 1,
                ALC_HRTF_SOFT, ALC_TRUE,
                ALC_REFRESH, 60,
                0
        };
        audioContext = alcCreateContext(audioDevice, attribs);
        if (audioContext == 0L) throw new RuntimeException("Failed to create OpenAL context");

        alcMakeContextCurrent(audioContext);
        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        if (!alCapabilities.OpenAL10) throw new RuntimeException("OpenAL 1.0 not supported");

        alEnable(AL_SOURCE_DISTANCE_MODEL);
        alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);

        alListenerf(AL_GAIN, 1.0f);
        checkOpenALError("Initialization");
    }

    public void newSource(boolean priority, String name, URL url, String identifier, boolean looping, float x, float y, float z, int attenuation, float volume, boolean applyReverb) {
        Integer sourceId;
        if (!freeSources.isEmpty()) {
            sourceId = freeSources.remove(freeSources.size() - 1);
        } else if (soundSources.size() < MAX_SOURCES) {
            sourceId = alGenSources();
            if (!alIsSource(sourceId)) {
                System.err.println("Failed to generate new source");
                return;
            }
        } else {
            return;
        }

        alSourceStop(sourceId);
        alSourcei(sourceId, AL_BUFFER, 0);
        alSourcef(sourceId, AL_PITCH, 1.0f);
        alSourcef(sourceId, AL_GAIN, 1.0f);
        alSource3f(sourceId, AL_POSITION, 0f, 0f, 0f);
        alSourcei(sourceId, AL_LOOPING, AL_FALSE);

        soundSources.put(name, sourceId);
        alSource3f(sourceId, AL_POSITION, x, y, z);
        alSourcef(sourceId, AL_GAIN, Math.max(0.0f, Math.min(volume, 1.0f)));
        alSourcei(sourceId, AL_LOOPING, looping ? AL_TRUE : AL_FALSE);

        alSourcef(sourceId, AL_ROLLOFF_FACTOR, 1.0f);
        alSourcef(sourceId, AL_REFERENCE_DISTANCE, 1.0f);
        alSourcef(sourceId, AL_MAX_DISTANCE, 16.0f * 4.0f);

        checkOpenALError("setting up source properties");
    }

    public boolean playing(String name) {
        Integer sourceId = soundSources.get(name);
        return sourceId != null && alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING;
    }

    public void cleanup() {
        for (int source : soundSources.values()) {
            alSourceStop(source);
            alDeleteSources(source);
        }
        for (int source : freeSources) {
            alDeleteSources(source);
        }
        soundSources.clear();
        freeSources.clear();

        if (audioContext != 0L) {
            alcMakeContextCurrent(0);
            alcDestroyContext(audioContext);
            audioContext = 0L;
        }
        if (audioDevice != 0L) {
            alcCloseDevice(audioDevice);
            audioDevice = 0L;
        }
    }

    public void play(String name) {
        Integer sourceId = soundSources.get(name);
        if (sourceId != null && alGetSourcei(sourceId, AL_SOURCE_STATE) != AL_PLAYING) {
            alSourcePlay(sourceId);
            checkOpenALError("alSourcePlay");
        }
    }

    public void pause(String name) {
        Integer sourceId = soundSources.get(name);
        if (sourceId != null && alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING) {
            alSourcePause(sourceId);
        }
    }

    public void stop(String name) {
        Integer sourceId = soundSources.get(name);
        if (sourceId != null) {
            alSourceStop(sourceId);
        }
    }

    public void removeSource(String name) {
        Integer sourceId = soundSources.remove(name);
        if (sourceId != null) {
            alSourceStop(sourceId);
            alSourcei(sourceId, AL_BUFFER, 0);
            freeSources.add(sourceId);
        }
    }

    public void setPitch(String name, float pitch) {
        Integer sourceId = soundSources.get(name);
        if (sourceId != null) {
            alSourcef(sourceId, AL_PITCH, Math.max(0.5f, Math.min(pitch, 2.0f)));
        }
    }

    public void setVolume(String name, float volume) {
        Integer sourceId = soundSources.get(name);
        if (sourceId != null) {
            alSourcef(sourceId, AL_GAIN, Math.max(0.0f, Math.min(volume, 1.0f)));
        }
    }

    public void setMasterVolume(float volume) {
        alListenerf(AL_GAIN, Math.max(0.0f, Math.min(volume, 1.0f)));
    }


    public float getMasterVolume() {
        float[] volume = new float[1];
        alGetListenerf(AL_GAIN, volume);
        return volume[0];
    }
    public void setPosition(String name, float x, float y, float z) {
        Integer sourceId = soundSources.get(name);
        if (sourceId != null) {
            alSource3f(sourceId, AL_POSITION, x, y, z);
        }
    }

    public void setListenerPosition(float x, float y, float z) {
        alListener3f(AL_POSITION, x, y, z);
    }

    public void setListenerOrientation(float atX, float atY, float atZ, float upX, float upY, float upZ) {
        float[] orientation = {atX, atY, atZ, upX, upY, upZ};
        alListenerfv(AL_ORIENTATION, orientation);
    }

    private static void checkOpenALError(String operation) {
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            System.err.println("OpenAL error during " + operation + ": " + alGetString(error));
        }
    }
}
