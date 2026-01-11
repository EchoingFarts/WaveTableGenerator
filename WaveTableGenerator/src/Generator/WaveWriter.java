package Generator;

import javax.sound.sampled.*;
import java.io.*;

/**
 * Utility class for writing floating-point audio data to WAV files.
 *
 * <p>This class supports writing mono 16-bit PCM WAV files from
 * normalized floating-point samples in the range {@code [-1.0, 1.0]}.</p>
 *
 * <p>This class cannot be instantiated.</p>
 */
public final class WaveWriter {

    /**
     * Prevents instantiation of this utility class.
     */
    private WaveWriter() {
    }

    /**
     * Writes a mono 16-bit PCM WAV file from normalized floating-point samples.
     *
     * <p>Each input sample is clamped to {@code [-1.0, 1.0]}, converted
     * to signed 16-bit PCM, and written in little-endian byte order.</p>
     *
     * @param filename the output WAV file path
     * @param audio the mono audio samples, expected in the range {@code [-1.0, 1.0]}
     * @param sampleRate the sample rate in Hz (e.g., {@code 44100})
     * @throws Exception if the file cannot be written or the audio system fails
     */
    public static void writeMono16(String filename, float[] audio, int sampleRate)
            throws Exception {

        byte[] pcm = new byte[audio.length * 2];
        int idx = 0;

        for (float s : audio) {
            float x = Math.max(-1f, Math.min(1f, s));
            short v = (short) Math.round(x * 32767.0);
            pcm[idx++] = (byte) (v & 0xff);
            pcm[idx++] = (byte) ((v >>> 8) & 0xff);
        }

        AudioFormat format = new AudioFormat(
                sampleRate,
                16,
                1,
                true,
                false
        );

        try (ByteArrayInputStream bais = new ByteArrayInputStream(pcm);
             AudioInputStream ais =
                     new AudioInputStream(bais, format, audio.length)) {

            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
        }
    }
}
