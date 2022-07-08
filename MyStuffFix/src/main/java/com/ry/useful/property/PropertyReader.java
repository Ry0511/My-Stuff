package com.ry.useful.property;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Java class created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@ToString
@EqualsAndHashCode
@Getter(AccessLevel.PROTECTED)
public abstract class PropertyReader {

    /**
     * Source file use to create this reader.
     */
    private final File source;

    /**
     * The File content loaded from source.
     */
    private final String content;

    /**
     * Constructs the reader from the base file.
     *
     * @param file The file to base this reader on.
     * @throws IOException If reading the file fails.
     */
    public PropertyReader(@NonNull final File file) throws IOException {
        if (!file.isFile()) throw new Error("Not File: " + file);
        this.source = file;
        this.content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    /**
     * Extracts the provided property from this reader.
     *
     * @param property The property to extract.
     * @return The extracted property.
     */
    protected ExtractedProperty loadProperty(final Property property) {
        return property.extract(content);
    }
}
