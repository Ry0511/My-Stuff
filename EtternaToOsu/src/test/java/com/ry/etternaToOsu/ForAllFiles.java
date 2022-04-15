package com.ry.etternaToOsu;

import com.ry.etterna.EtternaFile;
import com.ry.useful.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java class created on 14/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class ForAllFiles {
    private static final File OUTPUT = new File("C:\\Games\\Etterna\\Songs123\\Positive-Offset-Pack");
    private static final File DIR = new File("C:\\Games\\Etterna\\Songs\\xXxRUSSIAxXx 3");

    public static void main(String[] args) {
        final AtomicInteger count = new AtomicInteger();
        int goal = 20;

        FileUtils.iterateFiles(
                DIR,
                new SuffixFileFilter(".sm"),
                TrueFileFilter.TRUE).forEachRemaining(x -> {
            try {
                if (count.get() == goal) return;
                EtternaFile f = new EtternaFile(x);

                if (f.hasPackStructure() && f.getOffset().isPresent()) {
                    if (f.getOffset().get().signum() >= 0) {
                        System.out.println("File: " + f.getSmFile().getAbsolutePath());
                        copyStructure(f);
                        count.getAndIncrement();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void copyStructure(final EtternaFile f) throws IOException {
        final File parent = f.getSmFile().getParentFile();

        final File dir = new File(StringUtils.buildPath(
                OUTPUT,
                parent.getName()
        ));

        if (dir.isDirectory() || dir.mkdir()) {
            FileUtils.copyDirectory(parent, dir);
        }
    }
}
