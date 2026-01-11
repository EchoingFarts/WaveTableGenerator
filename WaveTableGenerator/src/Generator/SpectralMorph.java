package Generator;

/**
 * Utility class that performs spectral-domain morphing between two
 * single-cycle waveforms.
 *
 * <p>The morph process computes the FFT of both input signals, interpolates
 * per-bin magnitudes, selects or interpolates phase according to a
 * {@link PhaseMode}, applies an optional high-frequency rolloff, and then
 * reconstructs a real-valued time-domain frame via inverse FFT.</p>
 *
 * <p>This class cannot be instantiated.</p>
 */
public final class SpectralMorph {

    /**
     * Prevents instantiation of this utility class.
     */
    private SpectralMorph() {
    }

    /**
     * Phase handling strategy for spectral morphing.
     */
    public enum PhaseMode {
        /**
         * Uses phase from input A for all bins.
         */
        KEEP_A,

        /**
         * Uses phase from input B for all bins.
         */
        KEEP_B,

        /**
         * Linearly interpolates phase using the shortest angular distance.
         */
        LERP
    }

    /**
     * Generates a single wavetable frame by spectrally morphing between two
     * real-valued signals {@code a} and {@code b}.
     *
     * <p>Both input arrays must have identical length {@code N}. The algorithm
     * assumes FFT-friendly sizing and requires that the underlying FFT routine
     * supports the provided size (this implementation expects {@code N} to be a
     * power of two).</p>
     *
     * <p>Processing steps:</p>
     * <ul>
     *   <li>Convert inputs to complex interleaved form and compute forward FFTs.</li>
     *   <li>For bins {@code 0..N/2}, interpolate magnitude by {@code t}.</li>
     *   <li>Apply a cosine-taper high-frequency rolloff defined by {@code cutoffFrac}
     *       and {@code rollFrac} relative to the Nyquist bin.</li>
     *   <li>Choose phase according to {@code phaseMode} and rebuild the spectrum.</li>
     *   <li>Mirror bins to enforce conjugate symmetry for a real signal.</li>
     *   <li>Compute inverse FFT, then remove DC offset and normalize.</li>
     * </ul>
     *
     * <p>The rolloff is defined in bins relative to Nyquist ({@code N/2}):</p>
     * <ul>
     *   <li>{@code cutoffFrac} sets the start of attenuation (kept at 1.0 below this).</li>
     *   <li>{@code rollFrac} sets the width of the cosine taper (attenuates to 0.0 across this span).</li>
     * </ul>
     *
     * @param a the first input signal (time-domain), length {@code N}
     * @param b the second input signal (time-domain), length {@code N}
     * @param t interpolation factor where {@code 0.0} yields A and {@code 1.0} yields B
     * @param phaseMode strategy used to select or interpolate phase per bin
     * @param cutoffFrac fraction of the Nyquist bin where rolloff begins (typically {@code 0.0..1.0})
     * @param rollFrac fraction of the Nyquist bin over which attenuation tapers to zero
     * @return the morphed, normalized time-domain frame, length {@code N}
     * @throws IllegalArgumentException if {@code a} and {@code b} have different lengths
     */
    public static float[] morphFrame(float[] a, float[] b, float t,
                                     PhaseMode phaseMode,
                                     double cutoffFrac, double rollFrac) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("A/B length mismatch");
        }
        int n = a.length;

        // FFT(A), FFT(B)
        float[] A = FFTUtil.realToComplex(a);
        float[] B = FFTUtil.realToComplex(b);

        FFTUtil.fft(A, false);
        FFTUtil.fft(B, false);

        // Build output spectrum OUT from bins
        float[] OUT = new float[2 * n];

        // Bin 0 (DC) and Nyquist (n/2) are special in real FFT symmetry.
        // We'll treat them like normal bins but keep imag near 0 after symmetry.

        for (int k = 0; k <= n / 2; k++) {
            int idx = 2 * k;

            float ar = A[idx];
            float ai = A[idx + 1];
            float br = B[idx];
            float bi = B[idx + 1];

            double magA = Math.hypot(ar, ai);
            double magB = Math.hypot(br, bi);
            double mag = magA + (magB - magA) * t;

            // ---- High-bin rolloff (cosine taper) ----
            double nyquistBin = n / 2.0;
            double cutoff = cutoffFrac * nyquistBin;
            double roll = rollFrac * nyquistBin;

            double keep;
            if (k <= cutoff) {
                keep = 1.0;
            } else if (k >= cutoff + roll) {
                keep = 0.0;
            } else {
                double x = (k - cutoff) / roll;              // 0..1
                keep = 0.5 * (1.0 + Math.cos(Math.PI * x));  // 1..0 smooth
            }

            mag *= keep;
            // ---- End rolloff ----

            double phaseA = Math.atan2(ai, ar);
            double phaseB = Math.atan2(bi, br);

            double phase;
            switch (phaseMode) {
                case KEEP_B -> phase = phaseB;
                case LERP -> phase = lerpAngle(phaseA, phaseB, t);
                default -> phase = phaseA; // KEEP_A
            }

            double outR = mag * Math.cos(phase);
            double outI = mag * Math.sin(phase);

            OUT[idx] = (float) outR;
            OUT[idx + 1] = (float) outI;

            if (k == 0 || k == n / 2) {
                OUT[idx + 1] = 0f;
            }


            // Mirror to negative frequencies (complex conjugate) for real signal
            if (k != 0 && k != n / 2) {
                int mk = n - k;
                int midx = 2 * mk;
                OUT[midx] = (float) outR;
                OUT[midx + 1] = (float) (-outI);
            }
        }



        // IFFT back to time domain
        FFTUtil.fft(OUT, true);
        float[] frame = FFTUtil.complexToReal(OUT);

        // Clean up frame (helps stability)
        DspUtil.removeDC(frame);
        DspUtil.normalize(frame, 0.99f);

        return frame;
    }

    /**
     * Linearly interpolates between two angles (radians) using the shortest
     * angular distance, handling wraparound at {@code +/-pi}.
     *
     * @param a the starting angle in radians
     * @param b the ending angle in radians
     * @param t interpolation factor where {@code 0.0} yields {@code a} and {@code 1.0} yields {@code b}
     * @return the interpolated angle in radians
     */
    private static double lerpAngle(double a, double b, double t) {
        double diff = b - a;
        while (diff > Math.PI) {
            diff -= 2.0 * Math.PI;
        }
        while (diff < -Math.PI) {
            diff += 2.0 * Math.PI;
        }
        return a + diff * t;
    }
}
