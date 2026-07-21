package org.lwjgl.openal;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;

public class OggDecoder {
    private static final Logger logger = LogManager.getLogger("OggDecoder");

    public static int loadOgg(ByteBuffer vorbisData) {
        if (vorbisData.position() != 0) vorbisData.rewind();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            long decoder = STBVorbis.stb_vorbis_open_memory(vorbisData, error, null);
            if (decoder == 0) {
                logger.error("Failed to open Ogg Vorbis file: Error {}", error.get(0));
                return 0;
            }
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);
            int sampleRate = info.sample_rate();
            int channels = info.channels();
            int format = (channels == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
            int samples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
            if (samples <= 0) {
                STBVorbis.stb_vorbis_close(decoder);
                return 0;
            }
            int bufferSize = samples * channels * 2;
            ShortBuffer pcm = BufferUtils.createShortBuffer(bufferSize);
            int samplesDecoded = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            if (samplesDecoded != samples) logger.warn("Decoded sample count mismatch: expected {}, got {}", samples, samplesDecoded);
            pcm.limit(samplesDecoded * channels);
            int bufferId = alGenBuffers();
            if (!alIsBuffer(bufferId)) {
                STBVorbis.stb_vorbis_close(decoder);
                return 0;
            }
            alBufferData(bufferId, format, pcm, sampleRate);
            int alError = alGetError();
            if (alError != AL_NO_ERROR) {
                logger.error("OpenAL error after alBufferData: {}", alError);
                alDeleteBuffers(bufferId);
                STBVorbis.stb_vorbis_close(decoder);
                return 0;
            }
            STBVorbis.stb_vorbis_close(decoder);
            return bufferId;
        } catch (Exception e) {
            logger.error("Exception in OggDecoder.loadOgg", e);
            return 0;
        }
    }
}
