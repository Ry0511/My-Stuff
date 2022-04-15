package com.ry.etternaToOsu;

import com.ry.useful.StringUtils;
import lombok.Data;

import java.io.File;

/**
 * Java class created on 13/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data(staticConstructor = "of")
public class OsuFileStructure {

    /**
     * The base directory of this file structure.
     */
    private final File baseDir;

    /**
     * The osu file for this structure.
     */
    private File osuFile;

    /**
     * The audio file for this structure.
     */
    private File audioFile;

    /**
     * The background file for this structure.
     */
    private File bgFile;

    /**
     * Appends to the base directory the provided string args. Effectively
     * building a path deriving from the base directory such as
     * baseDir/stuff/e.txt which <pre>fn(stuff, e.txt)</pre>
     *
     * @param args The path arguments.
     * @return A file reference which is "baseDir + / + arg + / + arg..."
     */
    public File appendBaseDir(final String... args) {
        final Object[] xs = new Object[args.length + 1];
        xs[0] = baseDir;
        System.arraycopy(args, 0, xs, 1, xs.length - 1);
        return new File(StringUtils.buildPath(xs));
    }

    /**
     * @return {@code true} iff audio file is not null.
     */
    public boolean isAudioPresent() {
        return audioFile != null;
    }

    /**
     * @return {@code true} iff bg file is not null.
     */
    public boolean isBgPresent() {
        return bgFile != null;
    }

    /**
     * @return {@code true} iff osu file is not null.
     */
    public boolean isOsuPresent() {
        return osuFile != null;
    }

    /**
     * @return {@code true} iff the audio file doesn't exist and the osu file
     * doesn't exist, that is, both references are null.
     */
    public boolean isInvalid() {
        return !isAudioPresent() && isOsuPresent();
    }
}
