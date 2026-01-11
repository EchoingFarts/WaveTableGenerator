package Generator;

import java.io.File;
import java.util.Locale;
import java.util.Scanner;

/**
 * Command-line wavetable generator that performs spectral morphing
 * between two input WAV files.
 *
 * <p>The program reads two source waveforms, resamples them to a
 * single-cycle length, removes DC offset, normalizes amplitude,
 * and generates a multi-frame wavetable using spectral interpolation.</p>
 *
 * <p>The resulting wavetable is written as a mono 16-bit PCM WAV file,
 * suitable for import into wavetable synthesizers.</p>
 */
public class WavetableGen {

    /**
     * Entry point for the wavetable generator.
     *
     * <p>This method prompts the user for input WAV files, wavetable
     * parameters, and output file name, then performs spectral morphing
     * to generate the final wavetable.</p>
     *
     * @param args command-line arguments (not used)
     * @throws Exception if file I/O, parsing, or DSP processing fails
     */
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        // Base folder that contains your Wave1.wav / Wave2.wav etc.
        System.out.print("Please enter Base folder: (e.g. C:\\Users\\barte\\Desktop\\WavetableGenStuff): ");
        String baseDir = sc.nextLine().trim();
        baseDir += "\\";

        System.out.print("Wave A file (e.g., Wave1.wav or Wave1): ");
        String aName = sc.nextLine().trim();
        String pathA = inputWavPath(baseDir, aName);

        System.out.print("Wave B file (e.g., Wave2.wav or Wave2): ");
        String bName = sc.nextLine().trim();
        String pathB = inputWavPath(baseDir, bName);

        System.out.print("Frames (default 64): ");
        String framesIn = sc.nextLine().trim();
        int frames = framesIn.isEmpty() ? 64 : Integer.parseInt(framesIn);

        System.out.print("Table size (default 2048): ");
        String sizeIn = sc.nextLine().trim();
        int size = sizeIn.isEmpty() ? 2048 : Integer.parseInt(sizeIn);
        if (Integer.bitCount(size) != 1) {
            throw new IllegalArgumentException(
                    "Table size must be power of two (512/1024/2048/etc). Got: " + size
            );
        }

        System.out.print("Phase mode (KEEP_A / KEEP_B / LERP) default KEEP_A: ");
        String modeIn = sc.nextLine().trim();
        SpectralMorph.PhaseMode mode = modeIn.isEmpty()
                ? SpectralMorph.PhaseMode.KEEP_A
                : SpectralMorph.PhaseMode.valueOf(modeIn.toUpperCase(Locale.ROOT));

        System.out.print("Cutoff (0–1, default 0.85): ");
        String cutIn = sc.nextLine().trim();
        double cut = cutIn.isEmpty() ? 0.85 : Double.parseDouble(cutIn);

        System.out.print("Roll (0–1, default 0.10): ");
        String rollIn = sc.nextLine().trim();
        double roll = rollIn.isEmpty() ? 0.10 : Double.parseDouble(rollIn);

        System.out.print("Output file name (e.g., Wave3.wav or Wave3): ");
        String outName = sc.nextLine().trim();
        if (outName.isEmpty()) {
            outName = "wave3";
        }
        String outPath = outputWavPath(baseDir, outName);

        // ---- Actual generation starts here ----
        System.out.println("\nReading:");
        System.out.println("  A: " + pathA);
        System.out.println("  B: " + pathB);

        Waveform waveA = WaveReader.readWavAsMono16(pathA);
        Waveform waveB = WaveReader.readWavAsMono16(pathB);

        System.out.println("Loaded wave A: " + waveA.samples().length + " samples");
        System.out.println("Loaded wave B: " + waveB.samples().length + " samples");

        float[] aN = DspUtil.resampleCycleLinear(waveA.samples(), size);
        float[] bN = DspUtil.resampleCycleLinear(waveB.samples(), size);

        DspUtil.removeDC(aN);
        DspUtil.removeDC(bN);

        System.out.println("Resampled A: " + aN.length);
        System.out.println("Resampled B: " + bN.length);

        float[] wt = new float[frames * size];

        for (int f = 0; f < frames; f++) {
            float t = (frames == 1) ? 0f : (f / (float) (frames - 1));
            float[] frame = SpectralMorph.morphFrame(aN, bN, t, mode, cut, roll);
            System.arraycopy(frame, 0, wt, f * size, size);
        }

        DspUtil.normalize(wt, 0.99f);  // normalize once for the entire table


        int sr = (waveA.sampleRate() > 0) ? waveA.sampleRate() : 44100;
        WaveWriter.writeMono16(outPath, wt, sr);

        System.out.println("\nSettings:");
        System.out.println(
                "  frames=" + frames +
                        " size=" + size +
                        " mode=" + mode +
                        " cut=" + cut +
                        " roll=" + roll
        );
        System.out.println("Wrote: " + outPath);
    }

    /**
     * Resolves and validates an input WAV file path.
     *
     * <p>The path may be absolute or relative to the base directory.
     * The {@code .wav} extension is appended if missing.</p>
     *
     * @param baseDir the base directory for relative paths
     * @param nameOrPath the user-provided file name or path
     * @return the fully qualified WAV file path
     * @throws IllegalArgumentException if the file does not exist
     */
    private static String inputWavPath(String baseDir, String nameOrPath) {
        String full = qualifyWavPath(baseDir, nameOrPath);
        File f = new File(full);
        if (!f.exists()) {
            throw new IllegalArgumentException("Input file not found: " + full);
        }
        return full;
    }

    /**
     * Resolves and validates an output WAV file path.
     *
     * <p>The parent directory must already exist. The output file
     * itself is not required to exist.</p>
     *
     * @param baseDir the base directory for relative paths
     * @param nameOrPath the user-provided file name or path
     * @return the fully qualified output WAV file path
     * @throws IllegalArgumentException if the parent directory does not exist
     */
    private static String outputWavPath(String baseDir, String nameOrPath) {
        String full = qualifyWavPath(baseDir, nameOrPath);

        File f = new File(full);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            throw new IllegalArgumentException(
                    "Output folder not found: " + parent.getAbsolutePath()
            );
        }
        return full;
    }

    /**
     * Resolves a WAV file path relative to the base directory and
     * ensures the {@code .wav} extension is present.
     *
     * @param baseDir the base directory for relative paths
     * @param nameOrPath the raw file name or path
     * @return a fully qualified WAV file path
     */
    private static String qualifyWavPath(String baseDir, String nameOrPath) {
        String s = nameOrPath.trim();

        boolean looksAbsolute =
                s.contains(":\\") || s.startsWith("\\\\") ||
                        s.contains(":/")  || s.startsWith("/");

        if (!looksAbsolute) {
            s = baseDir + s;
        }

        if (!s.toLowerCase(Locale.ROOT).endsWith(".wav")) {
            s += ".wav";
        }
        return s;
    }
}
