package com.ry.etterna.util;

import com.ry.etterna.EtternaFile;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import lombok.extern.log4j.Log4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Java class created on 22/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@CommonsLog
@Setter(AccessLevel.PRIVATE)
public class EtternaIterator {

    /**
     * The original root directory being iterated.
     */
    private final File root;

    /**
     * Iterator of which iterates the Root directory for potential .SM files.
     */
    @Getter(AccessLevel.PRIVATE)
    private final Stream<Path> rootStream;

    /**
     * @param root Root directory containing potentially Zero Stepmania files.
     */
    public EtternaIterator(final File root) throws IOException {
        this.root = root;
        this.rootStream = Files.walk(root.toPath());
    }

    /**
     * Maps the elements of the root stream to Etterna Files using the input
     * filter.
     *
     * @return Stream of all acceptable etterna files.
     */
    public Stream<EtternaFile> getEtternaStream() {
        log.info("Walking through root: " + getRoot());
        return this.getRootStream()
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(x -> x.getName().endsWith(".sm"))
                .map(this::mapFile)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    /**
     * Maps the input file using the provided filter to an EtternaFile object.
     *
     * @param file The file to map.
     * @return Optionally mapped file if the input file can be mapped and is
     * acceptable to the input filter.
     */
    private Optional<EtternaFile> mapFile(final File file) {
        try {
            return Optional.of(new EtternaFile(file));
        } catch (final Exception e) {
            log.warn("File: " + file + "; produced exception " + e.toString() + "; Skipping this file.");
            return Optional.empty();
        }
    }
}
