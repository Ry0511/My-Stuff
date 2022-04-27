package com.ry.redone;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.reader.EtternaProperty;
import com.ry.etterna.util.CachedNoteInfo;
import com.ry.ffmpeg.FFMPEG;
import com.ry.ffmpeg.FFMPEGUtils;
import com.ry.ffmpeg.Task;
import com.ry.osu.builder.BuildUtils;
import com.ry.osu.builder.BuildableOsuFile;
import com.ry.useful.StringUtils;
import lombok.ToString;
import lombok.Value;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Java class created on 25/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Value
public class ConvertableFile {

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
     * The output audio file, nullable.
     */
    File audioFile;

    /**
     * The output background file, nullable.
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
        return append(songDir, "1.0x-Audio.mp3");
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
     * Creates a normal audio file blocking until it exists, or an error is
     * thrown.
     *
     * @param mpeg The ffmpeg instance to use to create the audio file.
     * @return {@code true} if the Audio file exists.
     */
    public boolean createNormalAudio(final FFMPEG mpeg) {

        // Return immediately
        if (getAudioFile().isFile()) {
            return true;
        }

        // If both parameters are present
        var delay = getEtternaFile().getOffset();
        var audio = getEtternaFile().getAudioFile();
        if (delay.isPresent() && audio.isPresent()) {
            try {

                final Process p = mpeg.execAndWait(FFMPEGUtils.delayAudio(
                        delay.get(),
                        audio.get().getAbsolutePath(),
                        getAudioFile().getAbsolutePath()
                ));
                return (p.exitValue() == 0) && getAudioFile().isFile();

                // Use default exit check
            } catch (final IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public String[] asyncRatedAudio() {

        if (getAudioFile().isFile()) {
            return null;
        }

        final File normal = getBaseRateAudio();
        if (normal.isFile()) {
            return FFMPEGUtils.rateAudio(
                    normal.getAbsolutePath(),
                    getRate(),
                    false,
                    getAudioFile().getAbsolutePath()
            );

        } else {
            System.err.println(
                    "Normal audio file doesn't exist: "
                            + normal.getAbsolutePath()
            );
        }

        return null;
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
                    } catch (InterruptedException | ExecutionException e) {
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
        final var v = BuildUtils.fromEtternaCache(getNotes(), getMsd());

        v.setBackgroundFile(getBgFile().getName());
        v.setAudioFile(getAudioFile().getName());
        v.setTitle(getEtternaFile().getPackFolder().getName());
        v.setCreator("The guy in your basement :)");

        // If the title is missing use the filename
        final String title;
        var element = getEtternaFile().getProperty(EtternaProperty.TITLE);
        if (element.isEmpty()) {
            title = StringUtils.getFileName(
                    getEtternaFile().getSmFile().getName()
            );
        } else {
            title = element.getProcessed();
        }

        v.setVersion(String.format(
                "[%s-%sx] - %s",
                getMsd().getSkill(SkillSet.OVERALL).toPlainString(),
                getRate(),
                title
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
