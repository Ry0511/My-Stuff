package com.ry.etterna.reader;

import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.useful.property.ExtractedProperty;
import com.ry.useful.property.Mapper;
import com.ry.useful.property.PropertyReader;
import com.ry.useful.property.SimpleStringProperty;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public class EtternaFileReader extends PropertyReader {

    /**
     * Constructs the reader from the base file.
     *
     * @param file The file to base this reader on.
     * @throws IOException If reading the file fails.
     */
    public EtternaFileReader(final @NonNull File file) throws IOException {
        super(file);

        // foo.sm or foo.SM...
        if (!file.getName().matches("(?i).*?\\.(sm)|(ssc)")) {
            throw new MalformedParametersException(
                    "Input file must be an Etterna File type: " + this
            );
        }
    }

    /**
     * Processes a singleton value into a target type wrapper.
     *
     * @param property The property to load.
     * @param target The type to initiate.
     * @param <T> The type of the mapped result.
     * @return Empty optional if the property doesn't exist. Else an Optional
     * consisting of the target type.
     */
    public <T> Optional<T> getSingleton(final EtternaProperty property,
                                        final Class<T> target) {
        final ExtractedProperty extracted
                = this.loadProperty(property.getProperty());

        if (extracted.isEmpty()) {
            return Optional.empty();
        }

        // This method is for singleton values only
        if (!extracted.isSingleton()) {
            throw new Error("Singleton Query produced many: " + property);
        }

        // Just assume the type is correct
        final Object result
                = extracted.processSingleton(property.getValueMapper());
        return Optional.of(target.cast(result));
    }

    /**
     * Extracts all properties of the target type.
     *
     * @param property The property to load.
     * @param target The type to return as, assumed to initiate as this.
     * @param <T> The type of the mapped result.
     * @return A potentially empty list, or a list containing all found
     * properties.
     */
    public <T> List<T> getMany(final EtternaProperty property,
                               final Class<T> target) {
        final ExtractedProperty extracted
                = this.loadProperty(property.getProperty());

        // We assume that ALL items in the list are of type T; we
        // specifically want it to fail if not
        final Mapper<?> mapper = property.getValueMapper();
        return extracted.processMany(mapper).stream()
                .map(target::cast)
                .collect(Collectors.toList());
    }

    /**
     * @return All notes sections mapped to a complex type.
     */
    public List<EtternaNoteInfo> getNoteInfo() {
        return getMany(EtternaProperty.NOTES, EtternaNoteInfo.class);
    }

    /**
     * @return The BPM timing information for this chart.
     */
    public EtternaTiming getTimingInfo() {
        return getSingleton(EtternaProperty.BPMS, EtternaTiming.class)
                .orElse(null);
    }

    /**
     * Loads a Simple String Property.
     *
     * @param property The property to load.
     * @return A potentially {@code empty} Simple String Property, iff not empty
     * then it contains something that is not the Empty string/null.
     */
    public SimpleStringProperty getStringProperty(
            @NonNull final EtternaProperty property) {

        // Singleton + String property
        if ((property.getMappedClassType() == SimpleStringProperty.class)
                && property.getProperty().isSingleton()) {

            return getSingleton(property, SimpleStringProperty.class)
                    .orElse(SimpleStringProperty.empty());
        }

        throw new Error("Invalid String Property: " + property);
    }

    /**
     * Source file use to create this reader.
     */
    @Override
    public File getSource() {
        return super.getSource();
    }

    /**
     * The File content loaded from source.
     */
    @Override
    public String getContent() {
        return super.getContent();
    }
}
