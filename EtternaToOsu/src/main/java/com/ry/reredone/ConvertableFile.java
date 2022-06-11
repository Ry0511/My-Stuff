package com.ry.reredone;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.reader.EtternaProperty;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.ffmpeg.FFMPEGUtils;
import com.ry.osu.builder.BuildUtils;
import com.ry.osu.builder.BuildableOsuFile;
import com.ry.useful.StringUtils;
import com.ry.vsrg.sequence.TimingSequence;
import lombok.ToString;
import lombok.Value;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * Java class created on 25/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Value
public class ConvertableFile {

    //
    // Just a copy and paste from redone, this file has its usages and a
    // clear purpose however it is a bit closed atm making control changes hard.
    //

    /**
     * The base output song directory, that is the root of this file. For 1.0
     * this is just OutputDir / Song however for Rates it is OutputDir / Song /
     * Rate.
     */
    File songDir;

    /**
     * The output osu file destination.
     */
    File osuFile;

    /**
     * The output audio file exists regardless of if the Input file has an audio
     * file or not.
     */
    File audioFile;

    /**
     * The output background file exists regardless of if the input file has a
     * background file or not.
     */
    File bgFile;

    /**
     * The notes that are being converted.
     */
    @ToString.Exclude
    CachedNoteInfo notes;

    /**
     * The rate of this chart.
     */
    String rate;

    /**
     * The msd information for the notes.
     */
    MSD msd;

    /**
     * Creates the convertable chart from the base output directory, the cached
     * file to convert, the rate of which to make, and the MSD information for
     * the rate.
     *
     * @param outputDir The output directory to base paths on, that is, this
     * will be the root of the Pack/Song/.osu
     * @param cache The cached chart to convert.
     * @param rate The rate of the chart.
     * @param msd The cached msd information.
     */
    public ConvertableFile(final File outputDir,
                           final CachedNoteInfo cache,
                           final String rate,
                           final MSD msd) {
        this.msd = msd;
        this.rate = rate;
        this.notes = cache;
        this.songDir = append(
                outputDir,
                getEtternaFile().getPackFolder().getName(),
                getEtternaFile().getSongFolder().getName()
        );
        //
        // Uses the song directory created above: outputdir/pack/song
        //
        this.osuFile = append(songDir, String.format(
                "(%s - %sx) - ([%s] - %s).osu",
                getAsciiID(),
                getRate(),
                msd.getSkill(SkillSet.OVERALL).toPlainString(),
                StringUtils.getFileName(getEtternaFile().getSmFile().getName())
                        .trim()
                        .replaceAll("[^\\w ]+", "")
        ));
        this.bgFile = append(songDir, "BG.jpg");
        this.audioFile = append(songDir, getRate() + "x-Audio.mp3");
    }

    /**
     * Appends to the root path the path extensions.
     *
     * @param root The root path.
     * @param ext The extensions to the root.
     * @return Root/ext[0]/ext[1]/.../ext[k]
     */
    private static File append(final File root, final String... ext) {
        final StringJoiner sj = new StringJoiner("/");
        sj.add(root.getAbsolutePath());
        Arrays.stream(ext).forEach(sj::add);
        return new File(sj.toString());
    }

    /**
     * @return The etterna file of the cached chart.
     */
    public EtternaFile getEtternaFile() {
        return this.notes.getEtternaFile();
    }

    /**
     * @return The ascii offset ID, that is,
     */
    public char getAsciiID() {
        final int asciiOffset = 65;
        return (char) (getNotes().getInfo().getDifficultyIndex() + asciiOffset);
    }

    /**
     * @return Unicode ID for the chart difficulty, that is, Alpha, Beta, Gamma,
     * etc.
     */
    public char getUnicodeID() {
        final int offset = 0x03B1;
        return (char) (getNotes().getInfo().getDifficultyIndex() + offset);
    }

    /**
     * @return The 1.0 Audio file destination.
     */
    public File getBaseRateAudio() {
        return append(songDir, "1.00x-Audio.mp3");
    }

    /**
     * @return {@code true} if the rate of this file is 1.0
     */
    public boolean isNormalRate() {
        return getRate().matches("1(.?[0]*)?");
    }

    ///////////////////////////////////////////////////////////////////////////
    // Creating output structure
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return {@code true} if the Song directory exists, or was created, else
     * {@code false}.
     */
    public boolean createSongDir() {
        final File d = getSongDir();
        return d.isDirectory() || d.mkdirs();
    }

    /**
     * Creates the appropriate FFMPEG CLI command to create the desired audio
     * file in the subject output directory.
     *
     * @return String array of CLI arguments.
     */
    public String[] getAudioConvertCommand() {
        if (getAudioFile().isFile()) {
            throw new IllegalStateException(String.format(
                    "Audio file '%s' already exists!",
                    getAudioFile().getAbsolutePath()
            ));
        }

        final Optional<File> audio = getEtternaFile().getAudioFile();
        if (audio.isPresent()) {

            // 1.0 Delayed audio
            if (isNormalRate()) {
                final var delay = getEtternaFile().getOffset()
                        .orElse(BigDecimal.ZERO);

                // The offsets are all offbeat by a single k/m this offset
                // has to be applied to the audio file as doing time - offset
                // will result in negative values.
                final var timingData = getNotes().getInfo().getCurTimingInfo();
                final var bpm = timingData.getBpms().get(0);
                final var measure
                        = getNotes().getInfo().getMeasures()[0].size();
                final var timePerNote = TimingSequence.calcTimePerNote(
                        measure,
                        bpm.getValue().doubleValue()
                ).divide(BigDecimal.valueOf(measure), MathContext.DECIMAL64);

                final BigDecimal offset
                        = delay.add(timePerNote, MathContext.DECIMAL64);

                return FFMPEGUtils.delayAudio(
                        offset,
                        audio.get().getAbsolutePath(),
                        getAudioFile().getAbsolutePath()
                );

                // Rated 1.0 audio
            } else {
                return FFMPEGUtils.rateAudio(
                        getBaseRateAudio().getAbsolutePath(),
                        getRate(),
                        false,
                        getAudioFile().getAbsolutePath()
                );
            }
        }

        // Default exit is error
        throw new IllegalStateException(
                "Audio file doesn't exist for chart: "
                        + getEtternaFile().getSmFile().getAbsolutePath()
        );
    }

    /**
     * Attempts to create the Background file for this chart.
     *
     * @return {@code true} if the bg file already exists, or if the process to
     * create the background file completes normally and the bg file exists.
     * Else if the bg file doesn't exist, or the chart doesn't have a bg file
     * then {@code false} is returned.
     */
    public boolean createBackgroundFile() {

        // If the file exists already just return
        if (getBgFile().isFile()) {
            return true;
        }

        // Line length issue so lambda
        final Function<String, String> fn = StringUtils::getFileName;

        // Run task map result
        return getEtternaFile().getBackgroundFile().map(bgFile -> {
                    try {
                        return FFMPEGUtils.compressImage(
                                bgFile.getAbsolutePath(),
                                fn.apply(getBgFile().getAbsolutePath())
                        ).get();
                    } catch (final Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }).map(p -> (p.exitValue() == 0) && getBgFile().isFile())
                .orElse(false);
    }

    /**
     * Creates an osu file builder from the stored Etterna cache. Applies some
     * default parameters for the Title, Version, Author, Background file, and
     * Audio file.
     *
     * @return Builder which can be written to.
     */
    public BuildableOsuFile.BuildableOsuFileBuilder asOsuBuilder() {
        final var v = BuildUtils.fromEtternaCache(getNotes());

        v.setBackgroundFile(getBgFile().getName());
        v.setAudioFile(getAudioFile().getName());
        v.setTitle(getEtternaFile().getPackFolder().getName());
        v.setCreator("Overcomplicated Conversion Tool");
        v.setSource(getMsd().sourceStr());

        // Filter search tags
        final StringJoiner tags = new StringJoiner(",");
        tags.add(getMsd().getMsdFilterTag("18", "35", "1"));
        tags.add(isNormalRate() ? "rate:no" : "rate:yes");
        getEtternaFile().getOffset().ifPresent(x -> {
            if (x.compareTo(BigDecimal.ZERO) > 0) {
                tags.add("delay:positive");
            } else {
                tags.add("delay:negative");
            }
        });

        // JS:25 TECH:23 etc
        getMsd().forEachSkill((skill, val) -> {
            tags.add(skill.getAcronym()
                    + ":"
                    + val.setScale(0, RoundingMode.FLOOR).toPlainString()
            );
        });
        v.setTags(tags.toString());

        // If the title is missing use the filename
        final String title;
        final var element = getEtternaFile().getProperty(EtternaProperty.TITLE);
        if (element.isEmpty()) {
            title = StringUtils.getFileName(
                    getEtternaFile().getSmFile().getName()
            );
        } else {
            title = element.getProcessed();
        }

        // Version
        v.setVersion(String.format(
                "%s - [%s] %s (%sx)",
                getUnicodeID(),
                getMsd().getSkill(SkillSet.OVERALL).toPlainString(),
                title,
                getRate()
        ));

        return v;
    }

    /**
     * Writes the provided file content to the osu file, creates the osu file if
     * it doesn't exist.
     *
     * @param content The content to write.
     * @throws IOException If the write fails.
     */
    public void createOsuFile(final String content) throws IOException {
        FileUtils.writeStringToFile(
                getOsuFile(),
                content,
                StandardCharsets.UTF_8
        );
    }
}
