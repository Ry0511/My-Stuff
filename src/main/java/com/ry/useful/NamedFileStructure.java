package com.ry.useful;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

/**
 * Java class created on 25/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@Setter(AccessLevel.PRIVATE)
@Getter(AccessLevel.PRIVATE)
public class NamedFileStructure {

    /**
     * String identifier used to mark a file as a root of some structure.
     */
    public static final String ROOT_ID = "struct/root";

    /**
     * Map of all files in this structure, it is assumed that not file shares a
     * name/ID.
     */
    private final Map<String, File> namedFiles = new HashMap<>();

    /**
     * @return The file with the {@link #ROOT_ID} identifier.
     */
    public File getRoot() {
        return this.namedFiles.get(ROOT_ID);
    }

    /**
     * Sets the root file to the provided file.
     *
     * @param rootFile The root f
     */
    public void setRoot(final File rootFile) {
        addFile(ROOT_ID, rootFile);
    }

    /**
     * Gets a file by its ID.
     *
     * @param id The ID of the file to get.
     * @return The file with the id, or null if the id doesn't exist.
     */
    public File getFile(@NonNull final String id) {
        return this.namedFiles.get(id);
    }

    /**
     * Adds a file in full by its id.
     *
     * @param id The id of the file.
     * @param file The file to add.
     */
    public void addFile(@NonNull final String id,
                        @NonNull final File file) {
        this.namedFiles.put(id, file);
    }

    /**
     * @param id The id of the new file.
     * @param pathExtensions The file path extensions.
     */
    public void addFile(@NonNull final String id,
                        @NonNull final String... pathExtensions) {

        final StringJoiner sj = new StringJoiner("/");
        sj.add(getFile(ROOT_ID).getAbsolutePath());
        Arrays.stream(pathExtensions).forEach(sj::add);

        addFile(id, new File(sj.toString()));
    }

    /**
     * Attempts to create the file denoted by the provided ID.
     *
     * @param id The ID of the file to create.
     * @return The file that may or may not have been created.
     * @throws IOException If one occurs whilst attempting to create the file.
     */
    public File createFile(@NonNull final String id) throws IOException {
        final File f = getFile(id);
        f.createNewFile();
        return f;
    }

    /**
     * Attempts to create the Directory structure of the file denoted by the
     * provided ID.
     *
     * @param id The ID of the file.
     * @return The directory which may or may not exist, alongside all parent
     * directories.
     */
    public File createDirectory(@NonNull final String id) {
        final File f = getFile(id);
        f.mkdirs();
        return f;
    }

    /**
     * @param action The action to apply to each file.
     */
    public void forEachFile(@NonNull final BiConsumer<String, File> action) {
        this.namedFiles.forEach(action);
    }
}
