package Generator;

/**
 * Utility class providing common digital signal processing (DSP)
 * operations used throughout the wavetable generation pipeline.
 *
 * <p>This class includes helpers for cyclic resampling, DC offset
 * removal, and amplitude normalization.</p>
 *
 * <p>This class cannot be instantiated.</p>
 */
public final class DspUtil {

    /**
     * Prevents instantiation of this utility class.
     */
    private DspUtil() {
    }

    /**
     * Resamples a single-cycle waveform to a new length using linear
     * interpolation.
     *
     * <p>The input waveform is treated as cyclic; interpolation wraps
     * from the end of the array back to the beginning to preserve
     * periodic continuity.</p>
     *
     * @param in the input waveform representing one full cycle
     * @param newSize the desired output length
     * @return a resampled waveform of length {@code newSize}
     * @throws IllegalArgumentException if the input length is less than 2
     */
    public static float[] resampleCycleLinear(float[] in, int newSize) {
        if (in.length < 2) {
            throw new IllegalArgumentException("Input too short");
        }

        float[] out = new float[newSize];
        int n = in.length;

        for (int i = 0; i < newSize; i++) {
            // Position in input cycle [0, n)
            double pos = (i * (double) n) / newSize;
            int i0 = (int) Math.floor(pos);
            double frac = pos - i0;

            int i1 = (i0 + 1) % n; // wrap for cyclic waveform
            float a = in[i0];
            float b = in[i1];

            out[i] = (float) (a + (b - a) * frac);
        }
        return out;
    }

    /**
     * Removes DC offset from a signal by subtracting its mean value.
     *
     * @param x the input signal to be modified in place
     */
    public static void removeDC(float[] x) {
        double mean = 0.0;
        for (float v : x) {
            mean += v;
        }
        mean /= x.length;

        for (int i = 0; i < x.length; i++) {
            x[i] -= (float) mean;
        }
    }

    /**
     * Normalizes a signal so that its peak absolute amplitude matches
     * the specified target.
     *
     * <p>If the signal is effectively silent, normalization is skipped.</p>
     *
     * @param x the input signal to be modified in place
     * @param peakTarget the desired peak amplitude (e.g., {@code 0.99})
     */
    public static void normalize(float[] x, float peakTarget) {
        float max = 0f;
        for (float v : x) {
            max = Math.max(max, Math.abs(v));
        }

        if (max < 1e-9f) {
            return;
        }

        float g = peakTarget / max;
        for (int i = 0; i < x.length; i++) {
            x[i] *= g;
        }
    }


    public static void rotateInPlace(float[] x, int shift) {
        int n = x.length;
        if (n == 0) return;

        shift %= n;
        if (shift < 0) shift += n;
        if (shift == 0) return;

        float[] tmp = x.clone();
        System.arraycopy(tmp, shift, x, 0, n - shift);
        System.arraycopy(tmp, 0, x, n - shift, shift);
    }

}
