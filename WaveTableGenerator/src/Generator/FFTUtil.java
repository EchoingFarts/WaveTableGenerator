package Generator;

/**
 * Utility class providing Fast Fourier Transform (FFT) operations
 * on complex-valued signals.
 *
 * <p>This implementation uses an in-place radix-2 Cooley–Tukey FFT
 * algorithm operating on interleaved complex data stored in a
 * {@code float[]} array.</p>
 *
 * <p>Complex values are represented as:</p>
 * <pre>
 *     data[2*i]     = real component
 *     data[2*i + 1] = imaginary component
 * </pre>
 *
 * <p>This class cannot be instantiated.</p>
 */
public final class FFTUtil {

    /**
     * Prevents instantiation of this utility class.
     */
    private FFTUtil() {
    }

    /**
     * Performs an in-place radix-2 Fast Fourier Transform (FFT)
     * or inverse FFT (IFFT) on interleaved complex data.
     *
     * <p>The input array length must be {@code 2 * N}, where {@code N}
     * is a power of two. The transform modifies the input array
     * directly.</p>
     *
     * <p>If {@code inverse} is {@code true}, the inverse FFT is
     * computed and the result is scaled by {@code 1 / N}.</p>
     *
     * @param data the interleaved complex data array
     * @param inverse {@code true} to compute the inverse FFT;
     *                {@code false} to compute the forward FFT
     * @throws IllegalArgumentException if {@code N} is not a power of two
     */
    public static void fft(float[] data, boolean inverse) {
        int n = data.length / 2;
        if (Integer.bitCount(n) != 1) {
            throw new IllegalArgumentException(
                    "FFT length must be power of two. Got " + n
            );
        }

        // Bit-reversal permutation
        int j = 0;
        for (int i = 0; i < n; i++) {
            if (i < j) {
                int i2 = i << 1;
                int j2 = j << 1;
                float tr = data[i2];
                float ti = data[i2 + 1];
                data[i2] = data[j2];
                data[i2 + 1] = data[j2 + 1];
                data[j2] = tr;
                data[j2 + 1] = ti;
            }
            int m = n >> 1;
            while (m >= 1 && j >= m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }

        // Cooley–Tukey FFT
        double sign = inverse ? 1.0 : -1.0;

        for (int len = 2; len <= n; len <<= 1) {
            double ang = sign * (2.0 * Math.PI / len);
            double wlenR = Math.cos(ang);
            double wlenI = Math.sin(ang);

            for (int i = 0; i < n; i += len) {
                double wR = 1.0;
                double wI = 0.0;

                int half = len >> 1;
                for (int k = 0; k < half; k++) {
                    int even = (i + k) << 1;
                    int odd = (i + k + half) << 1;

                    double uR = data[even];
                    double uI = data[even + 1];

                    double vR = data[odd];
                    double vI = data[odd + 1];

                    // v *= w
                    double tR = vR * wR - vI * wI;
                    double tI = vR * wI + vI * wR;

                    // Butterfly operation
                    data[even]     = (float) (uR + tR);
                    data[even + 1] = (float) (uI + tI);
                    data[odd]      = (float) (uR - tR);
                    data[odd + 1]  = (float) (uI - tI);

                    // w *= wlen
                    double nextWR = wR * wlenR - wI * wlenI;
                    double nextWI = wR * wlenI + wI * wlenR;
                    wR = nextWR;
                    wI = nextWI;
                }
            }
        }

        // Scale result for inverse FFT
        if (inverse) {
            float invN = 1.0f / n;
            for (int i = 0; i < data.length; i++) {
                data[i] *= invN;
            }
        }
    }

    /**
     * Converts a real-valued signal into an interleaved complex array.
     *
     * <p>The imaginary components are initialized to zero.</p>
     *
     * @param real the real-valued input signal
     * @return a complex array with interleaved real and imaginary parts
     */
    public static float[] realToComplex(float[] real) {
        int n = real.length;
        float[] c = new float[2 * n];
        for (int i = 0; i < n; i++) {
            c[2 * i] = real[i];
            c[2 * i + 1] = 0f;
        }
        return c;
    }

    /**
     * Extracts the real components from an interleaved complex array.
     *
     * @param complex the interleaved complex data
     * @return an array containing only the real components
     */
    public static float[] complexToReal(float[] complex) {
        int n = complex.length / 2;
        float[] r = new float[n];
        for (int i = 0; i < n; i++) {
            r[i] = complex[2 * i];
        }
        return r;
    }
}
