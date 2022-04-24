package com.ry.etterna.util;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.db.CacheDB;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
    private final Iterator<File> rootIterator;

    /**
     * The current file in the iterator.
     */
    private EtternaFile currentFile;

    /**
     * The current etterna file filter used to get next elements.
     */
    @Setter(AccessLevel.PUBLIC)
    private Predicate<EtternaFile> filter;

    /**
     * @param root Root directory containing potentially Zero Stepmania files.
     */
    public EtternaIterator(final File root) {
        this.root = root;
        this.rootIterator = FileUtils.iterateFiles(
                root,
                SM_FILTER,
                TrueFileFilter.TRUE
        );
    }

    /**
     * @return {@code true} if this Iterator has another file to test.
     */
    public boolean hasNext() {
        return rootIterator.hasNext();
    }

    /**
     * Optionally returns the next element in the iteration, that is, if the
     * next element exists and is Mappable then an element is returned, else if
     * the next element is not Mappable then Empty is returned.
     *
     * @return Empty or if Mappable the next element.
     * @throws NoSuchElementException If this iterator does not have a 'next'.
     */
    public Optional<EtternaFile> next() throws NoSuchElementException {
        final File next = rootIterator.next();

        // Only files which are compatible + non Exception
        try {
            final EtternaFile file = new EtternaFile(next);
            if (getFilter() == null || getFilter().test(file)) {
                return Optional.of(file);

                // Inform of skipped file
            } else {
                System.out.println(
                        "[INFO] Skipping: " + next.getAbsolutePath()
                );
            }

            // Print error
        } catch (final IOException ex) {
            System.err.println("[INFO] Skipping: " + next.getAbsolutePath());
            ex.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Parses all remaining files into Cached etterna files.
     *
     * @param db The cache db to probe.
     * @param handle The handler action.
     */
    public void forEachCached(final CacheDB db,
                              final Consumer<CachedNoteInfo> handle) {
        while (hasNext()) {
            next().map(x -> CachedNoteInfo.from(x, db))
                    .ifPresent(xs -> xs.forEach(handle));
        }
    }
}
