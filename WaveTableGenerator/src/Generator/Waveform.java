package Generator;

/**
 * Immutable data container representing a sampled audio waveform.
 *
 * <p>A {@code Waveform} consists of an array of floating-point samples
 * and an associated sample rate. Sample values are typically normalized
 * to the range {@code [-1.0, 1.0]}.</p>
 *
 * @param samples    The audio samples representing the waveform.
 * @param sampleRate The sample rate of the waveform in Hertz.
 */
public record Waveform(float[] samples, int sampleRate) {

    /**
     * Constructs a {@code Waveform} with the specified samples and
     * sample rate.
     *
     * @param samples    the audio sample data
     * @param sampleRate the sample rate in Hertz
     */
    public Waveform {
    }
}
