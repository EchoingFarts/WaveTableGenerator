package Generator;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;

/**
 * Utility class for reading WAV audio files and converting them into
 * {@link Waveform} objects.
 *
 * <p>This class supports reading mono 16-bit PCM WAV files directly,
 * as well as converting arbitrary WAV formats (including stereo)
 * into mono 16-bit PCM data.</p>
 *
 * <p>All samples are normalized to the range {@code [-1.0, 1.0]}.</p>
 */
public class WaveReader {

    /**
     * Reads a WAV file that must already be mono and 16-bit PCM.
     *
     * <p>This method performs strict validation and will reject files
     * that are not single-channel or not 16-bit PCM encoded.</p>
     *
     * @param path the file system path to the WAV file
     * @return a {@code Waveform} containing normalized sample data
     *         and the original sample rate
     * @throws IllegalArgumentException if the WAV file is not mono
     *                                  or not 16-bit PCM
     * @throws Exception if the file cannot be read or decoded
     */
    public static Waveform readMonoWav(String path) throws Exception {
        File file = new File(path);
        AudioInputStream ais = AudioSystem.getAudioInputStream(file);
        AudioFormat format = ais.getFormat();

        if (format.getChannels() != 1) {
            throw new IllegalArgumentException("WAV must be mono");
        }
        if (format.getSampleSizeInBits() != 16) {
            throw new IllegalArgumentException("WAV must be 16-bit PCM");
        }

        int sampleRate = (int) format.getSampleRate();
        byte[] raw = ais.readAllBytes();

        float[] samples = new float[raw.length / 2];
        int idx = 0;

        for (int i = 0; i < raw.length; i += 2) {
            int lo = raw[i] & 0xff;
            int hi = raw[i + 1];
            short val = (short) ((hi << 8) | lo);
            samples[idx++] = val / 32768f;
        }

        return new Waveform(samples, sampleRate);
    }

    /**
     * Reads a WAV file of any supported format and converts it into
     * a mono 16-bit PCM {@link Waveform}.
     *
     * <p>If the source audio contains multiple channels, the channels
     * are averaged to produce mono output.</p>
     *
     * @param path the file system path to the WAV file
     * @return a {@code Waveform} containing normalized mono sample data
     *         and the converted sample rate
     * @throws Exception if the file cannot be read, decoded, or converted
     */
    public static Waveform readWavAsMono16(String path) throws Exception {
        File file = new File(path);

        try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
            AudioFormat pcm16 = getAudioFormat(in);

            try (AudioInputStream pcmStream =
                         AudioSystem.getAudioInputStream(pcm16, in)) {

                byte[] raw = pcmStream.readAllBytes();
                int channels = pcm16.getChannels();
                int frames = raw.length / (2 * channels);

                float[] mono = new float[frames];
                int idx = 0;

                for (int frame = 0; frame < frames; frame++) {
                    float sum = 0f;

                    for (int ch = 0; ch < channels; ch++) {
                        int lo = raw[idx++] & 0xff;
                        int hi = raw[idx++];
                        short s = (short) ((hi << 8) | lo);
                        sum += s / 32768f;
                    }

                    mono[frame] = sum / channels;
                }

                return new Waveform(mono, (int) pcm16.getSampleRate());
            }
        }
    }

    /**
     * Creates a 16-bit PCM signed {@link AudioFormat} based on the
     * source audio stream.
     *
     * <p>The resulting format preserves the original sample rate
     * and channel count, but enforces 16-bit signed PCM encoding
     * in little-endian byte order.</p>
     *
     * @param in the source {@code AudioInputStream}
     * @return an {@code AudioFormat} representing 16-bit PCM audio
     */
    private static AudioFormat getAudioFormat(AudioInputStream in) {
        AudioFormat src = in.getFormat();

        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                src.getSampleRate(),
                16,
                src.getChannels(),
                src.getChannels() * 2,
                src.getSampleRate(),
                false
        );
    }
}
