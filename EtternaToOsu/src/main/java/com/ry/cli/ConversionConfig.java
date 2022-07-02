package com.ry.cli;

import com.ry.converter.BasicChartFinaliser;
import com.ry.converter.OsuConverterImpl;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.util.MSDChart;
import com.ry.ffmpeg.FFMPEG;
import com.ry.ffmpeg.FFMPEGUtils;
import com.ry.osu.builderRedone.sound.HitSound;
import com.ry.osu.builderRedone.sound.HitType;
import com.ry.osu.builderRedone.sound.SampleSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Java class created on 30/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Value
@Builder
public class ConversionConfig implements CFGOperations {

    // todo long notes are not being converted correctly

    private static final double VERSION = 1.1D;
    private static final MathContext C = MathContext.DECIMAL64;
    private static final int TOTAL_THREADS = Runtime.getRuntime().availableProcessors();

    ///////////////////////////////////////////////////////////////////////////
    // All fields are 'private final foo' and should contain a default value,
    // unless required.
    ///////////////////////////////////////////////////////////////////////////

    @Builder.Default
    @CLI(value = {"--max-deviation"}, mappingFunction = "mapToBigDecimal")
    @Desc("The maximum deviation between the 1.0 MSD and some rated MSD; Default value is 10")
    BigDecimal maxMsdDeviation = new BigDecimal("10");

    @Builder.Default
    @CLI(value = {"--min-down-rate"}, mappingFunction = "mapToBigDecimal")
    @Desc("The minimum 1.0 MSD to allow down rates; Default value is 26.5")
    BigDecimal minDownRateMsd = new BigDecimal("26.5");

    @Builder.Default
    @CLI(value = {"--min-rate-msd"}, mappingFunction = "mapToBigDecimal")
    @Desc("The minimum rated MSD value; Default is 22.5")
    BigDecimal minRateMsd = new BigDecimal("22.5");

    @Builder.Default
    @CLI(value = {"--max-rate-msd"}, mappingFunction = "mapToBigDecimal")
    @Desc(value = "The maximum rated MSD value; Default is 38", insertNewLine = true)
    BigDecimal maxRateMsd = new BigDecimal("38");

    @Builder.Default
    @CLI(value = {"--enable-log", "--el"}, mappingFunction = "mapToBoolean")
    @Desc("States if standard output messages should be sent; Default is true.")
    boolean isLogEnabled = true;

    @Builder.Default
    @CLI(value = {"--log-mode", "--lm"}, mappingFunction = "mapToInt")
    @Desc(value = "The logging mode used. 0, or 1; Default is 0 logging only stats; 1 Logs a lot more than that", insertNewLine = true)
    int logMode = 0;

    @Builder.Default
    @CLI(value = {"--od", "--overall-difficulty"}, mappingFunction = "mapToFloat0To10")
    @Desc("The overall difficulty (OD) for all converts; Default is 8.5")
    float overallDifficulty = 8.5F;

    @Builder.Default
    @CLI(value = {"--hp", "--health-drain", "--drain"}, mappingFunction = "mapToFloat0To10")
    @Desc("The health drain for all converts; Default is 8")
    float healthDrain = 8F;

    @Builder.Default
    @CLI(value = {"--tp-vol"}, mappingFunction = "mapToInt")
    @Desc("The volume for all timing points; Default is 100")
    int timingPointVolume = 100;

    @Builder.Default
    @CLI(value = {"--tp-ss", "--tp-sample-set"}, mappingFunction = "mapToInt")
    @Desc("The sample set for all timing points; Default is 3; Options are Auto:0, Normal:1, Soft:2, Drum:3")
    int sampleSet = SampleSet.DRUM.getId();

    @Builder.Default
    @CLI(value = {"--tp-si", "--tp-sample-index"}, mappingFunction = "mapToInt")
    @Desc("The sample index for all timing points; Default is 0; Options are Hit:0, Whistle:2, Finish:4, Clap:8")
    int sampleIndex = HitSound.HIT.getId();

    @Builder.Default
    @CLI(value = {"--min-ln-length", "--min-ln", "--min-hold"}, mappingFunction = "mapToBigDecimal")
    @Desc(value = "The minimum length of any Hold note in milliseconds; Default is null (any-length is allowed)", insertNewLine = true)
    BigDecimal minLongNoteLength = null;

    @Builder.Default
    @CLI(value = {"--use-cache"}, mappingFunction = "mapToBoolean")
    @Desc("States if the conversion should utilise some cache.db file to acquire MSD values for charts; Default is false")
    boolean isUseCache = false;

    @Builder.Default
    @CLI(value = {"--cache-path", "--cache"}, mappingFunction = "mapToPath")
    @Desc("The absolute path to some cache.db file containing Etterna MSD info; default is null")
    String cachePath = null;

    @Builder.Default
    @CLI(value = {"--use-calculated"}, mappingFunction = "mapToBoolean")
    @Desc("States if the conversion should utilise the internal MinaCalc natives to acquire MSD values; default is true")
    boolean isUseCalculated = true;

    @Builder.Default
    @CLI(value = {"--ffmpeg-path", "--ffmpeg"}, mappingFunction = "mapToPath")
    @Desc(value = "The path to some ffmpeg executable; default is null, meaning it will attempt to find Path ffmpeg or Working Directory ffmpeg.", insertNewLine = true)
    String ffmpegPath = null;

    @Builder.Default
    @CLI(value = {"--song-threads"}, mappingFunction = "mapToPosInt")
    @Desc("The number of threads to allocate to the Song conversion process; Default is `max (MAX_THREADS / 2) 1`")
    int songThreads = Math.max(TOTAL_THREADS / 2, 1);

    @Builder.Default
    @CLI(value = {"--rate-threads"}, mappingFunction = "mapToPosInt")
    @Desc("The number of threads to allocate to the Rate conversion process; Default is `max TOTAL_THREADS 1`")
    int rateThreads = Math.max(TOTAL_THREADS, 1);

    @CLI(value = {"--input", "--input-file", "--songs", "--i"}, isRequired = true, mappingFunction = "mapToPath")
    @Desc("The absolute path to the root directory containing files to convert; There is no default, this is required.")
    @NonNull
    String convertInputPath;

    @CLI(value = {"--output", "--o", "--output-dir", "--out"}, isRequired = true, mappingFunction = "mapToPath")
    @Desc("The absolute path to the directory to save/place converted files; There is no default, this is required.")
    @NonNull
    String outputPath;

    ///////////////////////////////////////////////////////////////////////////
    // Class methods.
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public ExecutorService getBaseRateService() {
        if (getSongThreads() == 1) {
            return Executors.newSingleThreadExecutor();
        }
        if (TOTAL_THREADS < getSongThreads()) {
            throw new Err(
                    "Max threads '%s' yet '%s' requested.",
                    TOTAL_THREADS, getSongThreads()
            );
        } else {
            return Executors.newWorkStealingPool(getSongThreads());
        }
    }

    @Override
    public ExecutorService getRatedService() {
        if (getRateThreads() == 1) {
            return Executors.newSingleThreadExecutor();
        }
        if (TOTAL_THREADS < getRateThreads()) {
            throw new Err(
                    "Max threads is '%s' yet '%s' threads requested.",
                    TOTAL_THREADS, getRateThreads()
            );
        } else {
            return Executors.newWorkStealingPool(getSongThreads());
        }
    }

    @Override
    public FFMPEG getFfmpeg() throws RuntimeException {

        if (getFfmpegPath() == null) {
            return FFMPEGUtils.getFfmpegFromPath()
                    .or(FFMPEGUtils::getFfmpegFromWorkingDir)
                    .orElseThrow(() -> new Err("Could not find ffmpeg on the PATH or in the Working directory."));
        } else {
            final File ffmpeg = new File(getFfmpegPath());
            if (ffmpeg.isFile() && ffmpeg.getName().matches("(?i)ffmpeg\\.exe")) {
                return new FFMPEG(ffmpeg.getAbsolutePath(), true);
            } else {
                throw new Err("The ffmpeg file '%s' is invalid.", getFfmpegPath());
            }
        }
    }

    @Override
    public CacheDB getCacheDb() throws RuntimeException {
        try {
            return new CacheDB(new File(getCachePath()));
        } catch (final SQLException e) {
            throw new Err("The cache db file '%s' is invalid; Extra: '%s'.", e.toString());
        }
    }

    @Override
    public File getInput() throws RuntimeException {
        final File f = new File(getConvertInputPath());
        if (f.isDirectory()) {
            return f;
        } else {
            throw new Err("Input directory '%s' must be an existing directory.", f);
        }
    }

    @Override
    public File getOutput() throws RuntimeException {
        final File f = new File(getOutputPath());
        if (f.isDirectory()) {
            return f;
        } else {
            throw new Err("Output directory '%s' must be an existing directory.", f);
        }
    }

    @Override
    public Function<EtternaNoteInfo, Stream<MSDChart>> getChartMappingFunction() throws RuntimeException {
        if (isUseCache() && isUseCalculated()) {
            return OsuConverterImpl.initEither(this::msdFilter, getCacheDb());

        } else if (isUseCache()) {
            return OsuConverterImpl.initCached(this::msdFilter, getCacheDb());

        } else if (isUseCalculated()) {
            return OsuConverterImpl.initCalculated(this::msdFilter);

        } else {
            throw new Error("This shouldn't happen...");
        }
    }

    @Override
    public BasicChartFinaliser getChartFinaliser() throws RuntimeException {
        return new BasicChartFinaliser(
                // Maybe I will add support for changing the Author/Creator; Not sure tbh.
                x -> x.setOverallDifficulty(getOverallDifficulty())
                        .setHpDrain(getHealthDrain()),

                // Excess timing points should be inherited
                x -> x.setVolume(getTimingPointVolume())
                        // This doesn't allow mixing of the values unfortunately, but eh.
                        .setSampleSetInt(getSampleSet())
                        .setSampleIndexInt(getSampleIndex()),

                // Optional HitObject mutations
                x -> {
                    if ((this.getMinLongNoteLength() == null)
                            || (x.getEndTime() == null)
                            || (x.getType() != HitType.MANIA_HOLD)) {
                        return x;
                    }
                    // Clamp short holds to single tap notes
                    final var diff = x.getEndTime().subtract(x.getEndTime(), MathContext.DECIMAL64).abs();
                    if (diff.compareTo(this.getMinLongNoteLength()) <= 0) {
                        return x.setType(HitType.HIT);
                    } else {
                        return x;
                    }
                }
        );
    }

    private boolean msdFilter(final MSD baseMsd, final MSDChart ratedMsd) {
        // todo this might be wrong...
        final BigDecimal base = baseMsd.getSkill(SkillSet.OVERALL);
        final BigDecimal rated = ratedMsd.getMsd().getSkill(SkillSet.OVERALL);

        // MinaCalc on junk files produces 0.0 so any files whose MSD is not > 1 are ignored.
        if (rated.compareTo(new BigDecimal("1.0")) <= 0) {
            return false;
        }

        // abs (B - R) <= K
        if (base.subtract(rated, C).abs().compareTo(getMaxMsdDeviation()) >= 0) {
            return false;
        }

        // B >= MIN_DOWN_RATE
        final boolean isDownRate = base.compareTo(getMinDownRateMsd()) >= 0;

        // (R >= MIN) && (R <= MAX)
        final boolean inRange = (rated.compareTo(getMinRateMsd()) >= 0)
                && (rated.compareTo(getMaxRateMsd()) <= 0);

        // If rate < 1.0
        if (ratedMsd.getRate().startsWith("0.")) {
            return inRange && isDownRate;

            // If rate >= 1.0 (1.0 isn't ever actually passed here)
        } else {
            return inRange;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Below is just boilerplate.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return This configuration as a multi-line string.
     */
    @SneakyThrows
    public String getConfigAsString() {
        final StringJoiner sj = new StringJoiner(System.lineSeparator());
        for (final Field f : ConversionConfig.class.getDeclaredFields()) {
            if (f.isAnnotationPresent(CLI.class)) {
                final CLI cli = f.getAnnotation(CLI.class);
                sj.add(String.format(
                        "%-18s = %s",
                        cli.value()[0],
                        f.get(this)
                ));
            }
        }
        return String.format("[CONFIGURATION]%n%s", sj.toString());
    }

    /**
     * Creates from all params a singular help string.
     *
     * @return Help string.
     */
    public static String getHelp() {
        // Longest value string combined
        final int maxLen = Arrays.stream(ConversionConfig.class.getDeclaredFields())
                .filter(x -> x.isAnnotationPresent(CLI.class))
                .map(x -> x.getAnnotation(CLI.class))
                .map(CLI::value)
                .map(Arrays::toString)
                .mapToInt(String::length)
                .max().orElseThrow();

        final StringJoiner sj = new StringJoiner(System.lineSeparator());
        sj.add(String.format("Ry's EtternaToOsu! CLI Implementation. Version: %s%n", VERSION));
        sj.add(String.format(
                "%-" + maxLen + "s | %-9s | Description",
                "Command Aliases",
                "Required?"
        ));

        for (final Field f : ConversionConfig.class.getDeclaredFields()) {
            if (f.isAnnotationPresent(Desc.class) && f.isAnnotationPresent(CLI.class)) {
                final CLI cli = f.getAnnotation(CLI.class);
                final Desc desc = f.getAnnotation(Desc.class);

                sj.add(String.format(
                        "%-" + maxLen + "s | %-9s | %s",
                        Arrays.toString(cli.value()),
                        cli.isRequired(),
                        desc.value()
                ));

                if (desc.insertNewLine()) {
                    sj.add("");
                }
            }
        }

        sj.add("")
                .add("Not all options apply appropriate sanity checks and thus "
                        + "on bad input data can raise unchecked exceptions or "
                        + "create unusable files.")
                .add("")
                .add("If both '--use-cache' and '--use-calculated' are set then a mixture "
                        + "of the two are used; Firstly, cached is assessed and then its "
                        + "calculated iff the MSD is not found in the cache.")
                .add("")
                .add("-Ry :: -Ry#5879");
        return sj.toString();
    }

    public static Field getFieldFor(final String cmd) {
        return Arrays.stream(ConversionConfig.class.getDeclaredFields())
                .filter(x -> x.isAnnotationPresent(CLI.class))
                .filter(x -> Stream.of(x.getAnnotation(CLI.class))
                        .map(CLI::value)
                        .flatMap(Arrays::stream)
                        .anyMatch(cmd::equalsIgnoreCase))
                .findFirst()
                .orElseThrow(() -> new Err("Command '%s' couldn't be found.", cmd));
    }

    public static CLI getCliFor(final String cmd) {
        return getFieldFor(cmd).getAnnotation(CLI.class);
    }

    public static Desc getDescFor(final String cmd) {
        return getFieldFor(cmd).getAnnotation(Desc.class);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Builder context to initialise the class
    ///////////////////////////////////////////////////////////////////////////

    public static class ConversionConfigBuilder {
        public ConversionConfigBuilder loadFromArgs(final String[] args) {
            final Method[] methods = ConversionConfigBuilder.class.getDeclaredMethods();

            for (int i = 0; i < args.length - 1; i += 2) {
                final String key = args[i];
                final String value = args[i + 1];

                // Find target data handler
                final Field target;
                final Method mapper;
                try {
                    target = getFieldFor(key);
                    mapper = Optional.of(getCliFor(key))
                            .flatMap(x -> Arrays.stream(methods)
                                    .filter(y -> y.getName().equalsIgnoreCase(x.mappingFunction()))
                                    .findFirst())
                            .orElseThrow();
                } catch (final Exception ex) {
                    throw new Err("Command '%s' was not found.", key);
                }

                // Update this builder
                try {
                    mapper.setAccessible(true);
                    // Gets a method with the signature: 'fieldName(fieldType)'
                    final Method m = ConversionConfigBuilder.class.getDeclaredMethod(target.getName(), target.getType());
                    m.setAccessible(true);
                    m.invoke(this, mapper.invoke(null, value));

                } catch (final Exception ex) {
                    throw new Error(ex);
                }
            }

            return this;
        }

        ///////////////////////////////////////////////////////////////////////////
        // String -> Complex Type mappers.
        ///////////////////////////////////////////////////////////////////////////

        private static BigDecimal mapToBigDecimal(final String arg) {
            try {
                return new BigDecimal(arg);
            } catch (final Exception ex) {
                throw new Err("Argument '%s' couldn't be processed to decimal value. Reason '%s'", arg, ex.getMessage());
            }
        }

        private static float mapToFloat0To10(final String arg) {
            final var value = mapToBigDecimal(arg);

            if ((value.compareTo(BigDecimal.TEN) <= 0)
                    && (value.compareTo(BigDecimal.ONE) >= 0)) {
                return value.floatValue();
            }

            throw new Err("Value '%s' not in range 0 -> 10", arg);
        }

        private static boolean mapToBoolean(final String arg) {
            final String[] yes = {"y", "yes", "true"};
            final String[] no = {"n", "no", "false"};

            if (Arrays.stream(yes).anyMatch(arg::equalsIgnoreCase)) {
                return true;
            }

            if (Arrays.stream(no).anyMatch(arg::equalsIgnoreCase)) {
                return false;
            }

            throw new Err("Argument '%s' isn't a processable boolean; Choices were '%s' or '%s'", arg, Arrays.toString(yes), Arrays.toString(no));
        }

        private static String mapToPath(final String arg) {
            final File f = new File(arg);
            if (f.isFile() || f.isDirectory()) {
                return f.getAbsolutePath();
            } else {
                throw new Err("Path '%s' isn't a file or a directory.", arg);
            }
        }

        private static int mapToPosInt(final String arg) {
            try {
                final int i = Integer.parseInt(arg);

                if (i > 0) {
                    return i;
                } else {
                    throw new RuntimeException("Argument must be positive");
                }

            } catch (final Exception ex) {
                throw new Err("Argument '%s' could not be loaded into integer for reason '%s'", arg, ex.getMessage());
            }
        }

        private static int mapToInt(final String arg) {
            try {
                return Integer.parseInt(arg);
            } catch (final Exception ex) {
                throw new Err("Argument '%s' isn't a valid number. More info: '%s'", ex.getMessage());
            }
        }
    }

    private static class Err extends RuntimeException {
        public Err(String message, Object... args) {
            super(String.format(message, args));
        }
    }
}
