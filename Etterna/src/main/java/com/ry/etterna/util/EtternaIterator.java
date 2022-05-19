package com.ry.etterna.util;

import com.ry.etterna.EtternaFile;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Java class created on 22/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@Setter(AccessLevel.PRIVATE)
public class EtternaIterator {

    /**
     * Suffix file filter for '.SM' Files will extend this for '.SSC'
     * functionality once I verify that my FileReader won't break.
     */
    public static final IOFileFilter SM_FILTER = new SuffixFileFilter(".sm");

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
     * The current etterna file filter used to get next elements.
     */
    @Setter(AccessLevel.PUBLIC)
    private Predicate<EtternaFile> filter;

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
    public Optional<EtternaFile> mapFile(final File file) {
        try {
            final EtternaFile f = new EtternaFile(file);
            if (getFilter().test(f)) {
                return Optional.of(f);
            } else {
                return Optional.empty();
            }
        } catch (final IOException e) {
            System.err.printf(
                    "[IO-ERROR] '%s' failed...%n", file.getAbsolutePath()
            );
            return Optional.empty();
        }
    }
}
