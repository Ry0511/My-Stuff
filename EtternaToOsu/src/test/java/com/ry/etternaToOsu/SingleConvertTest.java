package com.ry.etternaToOsu;

import com.ry.etterna.EtternaFile;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Java class created on 22/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class SingleConvertTest {

    private static final File SONGS_DIR = new File("");
    private static final File OUTPUT_DIR = new File("");

    public static void main(final String[] args) {

    }

    private static Iterator<File> iterateSM(final File dir) {
        return FileUtils.iterateFiles(
                dir,
                new SuffixFileFilter(".sm"),
                TrueFileFilter.TRUE
        );
    }

    private static Stream<EtternaFile> getAllWhere(
            @NonNull final Predicate<EtternaFile> condition) {

        final Stream.Builder<EtternaFile> str = Stream.builder();

        iterateSM(SONGS_DIR).forEachRemaining(x -> {
            try {
                final EtternaFile smFile = new EtternaFile(x);
                if (condition.test(smFile)) {
                    str.add(smFile);
                }

                // Stacktrace + skip
            } catch (final IOException e) {
                e.printStackTrace();
                System.err.println("[FILE->SM FAILED] " + x.getAbsolutePath());
            }
        });

        return str.build();
    }
}
