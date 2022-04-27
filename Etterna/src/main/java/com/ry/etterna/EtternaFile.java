package com.ry.etterna;

import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.reader.EtternaFileReader;
import com.ry.etterna.reader.EtternaProperty;
import com.ry.etterna.reader.EtternaTiming;
import com.ry.useful.Entity;
import com.ry.useful.MutatingValue;
import com.ry.useful.StringUtils;
import com.ry.useful.property.SimpleStringProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@Setter(AccessLevel.PRIVATE)
public class EtternaFile {

    /**
     * Regex to match a bunch of different media types.
     */
    private static final Pattern ANY_AUDIO_REGEX = Pattern.compile(
            "(?i).*\\.(mp3|ogg|flv|wav)"
    );

    /**
     * Regex to match a bunch of different visual media types.
     */
    private static final Pattern ANY_BG_FILE = Pattern.compile(
            "(?i).*\\.(png|jpg|jpeg|jpeg2000)"
    );

    /**
     * Tries to generically match the default bg file name for Etterna files.
     */
    private static final Pattern BG_FILE_REGEX = Pattern.compile(
            "(?i)(bg|background)(file)?"
    );

    /**
     * Reader that reads the etterna chart.
     */
    private final EtternaFileReader reader;

    /**
     * The etterna file used to create this, etterna file.
     */
    private final File smFile;

    /**
     * The timing information of the etterna file at 1.0x rate.
     */
    private final EtternaTiming timingInfo;

    /**
     * All note info's for the target file.
     */
    private final List<EtternaNoteInfo> noteInfo;

    /**
     * Map of all simple string property elements.
     */
    @Getter(AccessLevel.PRIVATE)
    private final HashMap<EtternaProperty, SimpleStringProperty> properties;

    /**
     * @param file The file to construct from.
     * @throws IOException If reading the subject file fails for some reason,
     *                     such as malformed file, or fucked file encoding.
     */
    public EtternaFile(@NonNull final File file) throws IOException {
        reader = new EtternaFileReader(file);
        this.smFile = file;
        this.timingInfo = reader.getTimingInfo();
        this.noteInfo = reader.getNoteInfo();

        for (int i = 0; i < noteInfo.size(); ++i) {
            final var v = noteInfo.get(i);
            v.setParent(this);
            v.setDifficultyIndex(i);
        }

        // Load the basic string properties
        properties = new HashMap<>();
        EtternaProperty.streamWithMappedType(SimpleStringProperty.class)
                .forEach(x -> properties.put(x, reader.getStringProperty(x)));
    }

    /**
     * Gets a simple string property.
     *
     * @param property The property to get.
     * @return {@code null} iff the property was not loaded, or if the
     * properties mapped class type is not a Simple String Property.
     */
    public SimpleStringProperty getProperty(final EtternaProperty property) {
        return properties.get(property);
    }

    /**
     * @return To string of all loaded properties.
     */
    @Override
    public String toString() {
        final StringJoiner sj = new StringJoiner(", ", "[", "]");
        EtternaProperty.streamWithMappedType(
                SimpleStringProperty.class).forEach(x -> sj.add(String.format(
                "%s='%s'",
                x.name(),
                getProperty(x)
        )));

        return String.format(
                "%s, %s, %s%n",
                "DiffCount: " + noteInfo.size(),
                timingInfo.toString(),
                sj.toString()
        );
    }

    /**
     * Checks to see if the {@link #getSmFile()} is 3 layers deep in the Songs
     * directory, that is, <pre>Songs > Pack > Song > File.sm</pre>
     * <pre>3       2      1      0</pre>
     *
     * @return {@code true} if and only if the above structure applies where
     * {@link #getSmFile()} is File.sm. (In a structure sense not that it's
     * named File.sm)
     */
    public boolean hasPackStructure() {
        return MutatingValue.of(getSmFile())
                .mutateValueIf(Objects::nonNull, File::getParentFile)
                .mutateValueIf(Objects::nonNull, File::getParentFile)
                .mutateValueIf(Objects::nonNull, File::getParentFile)
                .mutateType(this::isSongsDir)
                .getValue();
    }

    /**
     * @param f The file to test if it is the Songs directory.
     * @return {@code true} iff the provided file is a directory and is aptly
     * named "Songs" case ignorant.
     */
    public boolean isSongsDir(final File f) {
        return f != null
                && f.isDirectory()
                && f.getName().equalsIgnoreCase("Songs");
    }

    /**
     * @return Attempts to find the Audio file for this chart.
     */
    public Optional<File> getAudioFile() {
        // Any audio file
        return streamSongDir()
                .filter(x -> ANY_AUDIO_REGEX.matcher(x.getName()).matches())
                .findFirst();
    }

    /**
     * @return Attempts to find the Background file for this chart.
     */
    public Optional<File> getBackgroundFile() {
        final var expectedName = getProperty(EtternaProperty.BACKGROUND)
                .asMutatingValue()
                .mutateValueIf(SimpleStringProperty::isEmpty,
                        x -> x.mapRaw(v -> null))
                .mutateTypeIf(Objects::nonNull, SimpleStringProperty::getRaw)
                .getMutatedOpt()
                .orElse(MutatingValue.of(""))
                .getValue();

        final Entity<File> bgFile = Entity.empty();
        streamSongDir()
                .filter(x -> StringUtils.contains(x.getName(), ANY_BG_FILE.pattern()))
                .forEach(x -> {
                    final String n = x.getName();

                    // First check known
                    if (n.equals(expectedName)
                            || n.equalsIgnoreCase(expectedName)) {
                        bgFile.setValue(x);

                    } else if (StringUtils.contains(n, BG_FILE_REGEX.pattern())) {
                        bgFile.setValue(x);
                    }
                });

        return bgFile.asOptional();
    }

    /**
     * @return Stream of all files in the Parent folder of the .sm file.
     */
    public Stream<File> streamSongDir() {
        final File[] files = getSmFile().getParentFile().listFiles();
        if (files == null) {
            return Stream.empty();
        } else {
            return Stream.of(files);
        }
    }

    /**
     * @return The parent containing the stepmania file.
     */
    public File getSongFolder() {
        return getSmFile().getParentFile();
    }

    /**
     * @return Offset for this chart, if present, else empty.
     */
    public Optional<BigDecimal> getOffset() {
        final SimpleStringProperty x = getProperty(EtternaProperty.OFFSET);

        if (x != null && !x.isEmpty()) {
            return x.asDecimal();
        } else {
            return Optional.empty();
        }
    }

    /**
     * @return The Pack folder.
     * @throws RuntimeException Iff {@link #hasPackStructure()} is false.
     */
    public File getPackFolder() {
        if (hasPackStructure()) {
            return smFile.getParentFile().getParentFile();
        } else {
            throw new RuntimeException("No Pack Structure...");
        }
    }

    /**
     * @return {@code true} if this File has the pack structure, has an audio
     * file, and has a parseable offset.
     */
    public boolean isStandard() {
        return hasPackStructure()
                && getAudioFile().isPresent()
                && getOffset().isPresent();
    }
}
