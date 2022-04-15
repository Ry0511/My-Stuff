package com.ry.etternaToOsu;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.msd.MSD;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.reader.EtternaProperty;
import com.ry.etterna.reader.EtternaTiming;
import com.ry.etternaToOsu.ffmpeg.Commands;
import com.ry.etternaToOsu.ffmpeg.Runner;
import com.ry.useful.StringUtils;
import com.ry.useful.property.SimpleStringProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java class created on 12/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class EtternaConverter {

    /**
     * The base SM file to convert.
     */
    private final EtternaFile smFile;

    /**
     * The timing info to time the notes with.
     */
    private final EtternaTiming timingInfo;

    /**
     * The notes to convert.
     */
    private final EtternaNoteInfo noteInfo;

    /**
     * Osu info object which translates the etterna data to osu.
     */
    @Getter(AccessLevel.PRIVATE)
    private final OsuNoteInfo osuInfo;

    /**
     * Creates a converter for the provided file and notes using the provided
     * timing info.
     *
     * @param file The file to convert.
     * @param timing The timing data to use.
     * @param info The specific note data to map.
     */
    public EtternaConverter(final EtternaFile file,
                            final EtternaTiming timing,
                            final EtternaNoteInfo info) {
        this.smFile = file;
        this.timingInfo = timing;
        this.noteInfo = info;
        this.osuInfo = new OsuNoteInfo(noteInfo);
    }

    /**
     * Creates the etterna converter for a chart with the default timing data.
     *
     * @param file The file to convert.
     * @param notes The specific difficulty to map.
     */
    public EtternaConverter(final EtternaFile file,
                            final EtternaNoteInfo notes) {
        this(file, file.getTimingInfo(), notes);
    }

    /**
     * @see OsuNoteInfo#setElement(OsuElement, String)
     */
    public void setElem(final OsuElement elem, final String value) {
        this.getOsuInfo().setElement(elem, value);
    }

    /**
     * Loads all the data for this chart into the provided file structure.
     *
     * @param struct The file structure to write towards.
     * @return {@code true} iff the file has been converted, else if the file
     * was not converted then {@code false} is returned.
     */
    public boolean convert(final OsuFileStructure struct,
                           final String rate) throws Exception {

        final EtternaFile smFile = getSmFile();
        final File dest = struct.getOsuFile();
        final File base = struct.getBaseDir();
        if (!base.mkdirs() && !base.isDirectory()) {
            throw new IOException(
                    "Base Directory Creation Failed: " + base.getAbsolutePath()
            );
        }

        // Add audio
        smFile.getAudioFile().ifPresent(file -> {
            struct.setAudioFile(struct.appendBaseDir(rate + "-Audio.mp3"));
            convertAudioFile(
                    smFile.getProperty(EtternaProperty.OFFSET),
                    file,
                    struct.getAudioFile()
            );
        });

        // Add bg if it doesn't already exist
        smFile.getBackgroundFile().ifPresent(file -> {
            struct.setBgFile(struct.appendBaseDir("BG.jpg"));
            if (!struct.getBgFile().isFile()) {
                convertBgFile(
                        file,
                        struct.getBgFile()
                );
            }
        });

        // Load data to file
        final OsuTemplate template = getOsuInfo().asTemplate();
        template.setBackgroundFile("BG.jpg");
        template.setElement(
                OsuElement.AUDIO_FILE_NAME,
                struct.getAudioFile().getName()
        );
        try {
            template.createAndWrite(dest);
        } catch (IOException x) {
            System.err.println("Failed to create Dest: " + dest.getAbsolutePath());
            return false;
        }

        return true;
    }

    @SneakyThrows
    private void convertBgFile(final File input,
                               final File dest) {
        final String[] args = Commands.CompressImage.builder()
                .ioSetFfmpeg("ffmpeg")
                .ioSetInFile(input.getAbsolutePath())
                .ioSetOutFile(dest.getAbsolutePath())
                .build().compile();

        new Runner(args, x -> {
        }).start(60_000);
    }

    @SneakyThrows
    private void convertAudioFile(final SimpleStringProperty offset,
                                  final File input,
                                  final File dest) {
        // todo Rates + Positive offsets don't work this to me is more of an
        //  Osu issue than an actual conversion issue though.
        if (offset == null || offset.asDecimal().isEmpty()) return;
        final BigDecimal v = offset.asDecimal().get();
        final String[] args;

        // Negative Offset
        if (v.signum() == -1) {
            args = Commands.NegativeAudioOffset.builder()
                    .ioSetFfmpeg("ffmpeg")
                    .ioSetInFile(input.getAbsolutePath())
                    .ioSetOutFile(dest.getAbsolutePath())
                    .setDelay(v.negate()
                            .setScale(3, RoundingMode.UP)
                            .toString())
                    .build().compile();

            // Positive Offset (yes zero is positive :-)
        } else {
            final MathContext c = MathContext.DECIMAL64;
            args = Commands.PositiveAudioOffset.builder()
                    .ioSetFfmpeg("ffmpeg")
                    .ioSetInFile(input.getAbsolutePath())
                    .ioSetOutFile(dest.getAbsolutePath())
                    .setNumChannels(4)
                    .setDelay(v.multiply(new BigDecimal("1000"), c)
                            .toBigInteger()
                            .toString())
                    .build().compile();
        }

        new Runner(args, x -> {
        }).start(60_000);
    }

    /**
     * Initialises the default values for the output file.
     *
     * @param initialRate The MSD to default with.
     */
    public void initDefaultMetadata(final MSD initialRate,
                                    final int diffID) {
        final String title = getElem(EtternaProperty.TITLE).trim();
        setElem(OsuElement.TITLE, StringUtils.toAscii(title));
        setElem(OsuElement.TITLE_UNICODE, title);

        final String artist = getElem(EtternaProperty.ARTIST).trim();
        setElem(OsuElement.ARTIST, StringUtils.toAscii(artist));
        setElem(OsuElement.ARTIST_UNICODE, artist);

        final String creator = getElem(EtternaProperty.CREDIT).trim();
        setElem(OsuElement.CREATOR, creator.isEmpty() ? "Unknown" : creator);

        final String version = String.format("[%s] %s (%s)",
                initialRate.getSkill(SkillSet.OVERALL),
                title,
                diffID
        );
        setElem(OsuElement.VERSION, version);


        // Load msd info: MSD 21.31, JS 22.31, ...
        final StringJoiner source = new StringJoiner(", ");
        initialRate.forEachSkill((s, v) -> source.add(String.format("%s %s",
                s.getAcronym(), v.toPlainString())));
        setElem(OsuElement.SOURCE, source.toString());

        // MSD Query info
        setElem(OsuElement.TAGS, createQueryStr(initialRate));
    }

    /**
     * Creates a query string for the initial rate appending MSD search
     * information ranging from 15.0 to 36.0 in 1.0 increments. Finally,
     * appended to the end is the best skill, that is, the skill with the
     * largest value is appended.
     *
     * @param initialRate The rate to query.
     * @return Query string of the format 'K>V,K<V,K!' signifying less than,
     * greater than, and largest.
     */
    private String createQueryStr(final MSD initialRate) {
        final StringJoiner tags = new StringJoiner(",");

        final Object[] bestSkill = new Object[2];
        final AtomicInteger cycleCount = new AtomicInteger();
        final MathContext c = new MathContext(12, RoundingMode.UP);
        final BigDecimal increment = new BigDecimal("1.0");
        final BigDecimal max = new BigDecimal("36.0");

        initialRate.forEachSkill((skill, value) -> {
            if (skill != SkillSet.OVERALL) {
                if (bestSkill[0] == null) {
                    bestSkill[0] = skill;
                    bestSkill[1] = value;
                } else {
                    if (((BigDecimal) bestSkill[1]).compareTo(value) < 0) {
                        bestSkill[0] = skill;
                        bestSkill[1] = value;
                    }
                }
            }

            BigDecimal counter = new BigDecimal("15.0");

            // Tags where: MSD>k
            while (max.compareTo(counter) >= 0) {

                // MSD < C
                if (counter.compareTo(value) > 0) {
                    // If MSD < C more than once just stop evaluating
                    if (cycleCount.get() > 1) break;
                    cycleCount.getAndIncrement();

                    tags.add(String.format(
                            "%s<%s",
                            skill.getAcronym(),
                            counter.setScale(2, RoundingMode.UP)
                    ));

                    // MSD > C
                } else if (counter.compareTo(value) < 0) {
                    if (cycleCount.get() != 0) cycleCount.getAndDecrement();

                    tags.add(String.format(
                            "%s>%s",
                            skill.getAcronym(),
                            counter.setScale(2, RoundingMode.UP)
                    ));
                }
                counter = counter.add(increment, c);
            }
        });

        tags.add(String.format("%s!", ((SkillSet) bestSkill[0]).getAcronym()));
        return tags.toString();
    }

    /**
     * Extracts an etterna property from a file.
     *
     * @param property The property to load.
     * @return The empty string if nothing was found, else the found raw
     * element.
     */
    private String getElem(final EtternaProperty property) {
        final var v = getSmFile().getProperty(property);
        return v == null ? "" : v.getRaw();
    }
}
