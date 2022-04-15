package com.ry.etterna.reader;

import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.useful.MutatingValue;
import com.ry.useful.property.Mapper;
import com.ry.useful.property.NamedRegexProperty;
import com.ry.useful.property.NamedRegexProperty.ExtractionMode;
import com.ry.useful.property.SimpleStringProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ry.useful.property.NamedRegexProperty.ExtractionMode.MANY;
import static com.ry.useful.property.NamedRegexProperty.ExtractionMode.SINGLETON;

/**
 * Java enum created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@ToString
@Getter
public enum EtternaProperty {
    NOTES(MANY, 0, EtternaNoteInfo::loadFromStr, EtternaNoteInfo.class),
    BPMS(SINGLETON, EtternaTiming::loadFromStr, EtternaTiming.class),


    // This stuff doesn't really require a lot of code and can just use the
    // same type
    MUSIC(SINGLETON),
    BACKGROUND(SINGLETON),
    TITLE(SINGLETON),
    SUBTITLE(SINGLETON),
    ARTIST(SINGLETON),
    GENRE(SINGLETON),
    CREDIT(SINGLETON),
    BANNER(SINGLETON),
    OFFSET(SINGLETON),

    // Little more complex, but just a lazy way to get the header
    // information about the chart info
    NOTES_HEADER(new NamedRegexProperty(
            Pattern.compile("(?is)#NOTES:(.*?)(([;#])|$)"),
            1,
            "NOTES_HEADER", MANY),
            x -> MutatingValue.of(x)
                    .mutateType(v -> v.split(":"))
                    .mutateType(vs -> new LinkedList<>(List.of(vs)))
                    .mutateValueIf(vs -> vs.size() > 4, vs -> {
                        vs.removeLast();
                        return vs;
                    }).mutateType(vs -> vs.toArray(new String[0]))
                    .getValue(),
            String[].class
    );

    /**
     * Base regex which matches etterna style properties. Specifically,
     * properties that are of the form "#A:B;" however extra checks are added
     * for loosely matching such as: "#A:B" note that the delimiting ";" has
     * been removed/omitted. This regex will also match through New lines.
     */
    // There is probably a better way than using OR in the regex...
    private static final String BASE_REGEX = "(?is)#%s:(.*?)(([;#])|$)";

    /**
     * The property extractor.
     */
    private final NamedRegexProperty property;

    /**
     * The property type mapper.
     */
    private final Mapper<?> valueMapper;

    /**
     * The type of the class that maps this property.
     */
    private final Class<?> mappedClassType;

    /**
     * @return Stream of properties.
     */
    public static Stream<EtternaProperty> stream() {
        return Arrays.stream(values());
    }

    /**
     * @param type The target mapped type for elements of this stream.
     * @return A stream of which only consists of those that have the provided
     * mapped type.
     */
    public static Stream<EtternaProperty> streamWithMappedType(
            @NonNull final Class<?> type) {
        return stream().filter(x -> x.getMappedClassType() == type);
    }

    /**
     * Constructs the etterna property using the base template.
     *
     * @param mode The extraction mode type, that is, Many or One.
     * @param valueMapper The mapper used to go from String -> Type.
     */
    EtternaProperty(final ExtractionMode mode,
                    final int group,
                    final Mapper<?> valueMapper,
                    final Class<?> mappedClassType) {

        this.property = new NamedRegexProperty(
                Pattern.compile(String.format(BASE_REGEX, name())),
                group,
                name(),
                mode
        );
        this.valueMapper = valueMapper;
        this.mappedClassType = mappedClassType;
    }

    EtternaProperty(final ExtractionMode mode,
                    final Mapper<?> valueMapper,
                    final Class<?> mappedClassType) {
        this(mode, 1, valueMapper, mappedClassType);
    }

    EtternaProperty(final ExtractionMode mode) {
        this(mode, 1, SimpleStringProperty::new, SimpleStringProperty.class);
    }

    EtternaProperty(final NamedRegexProperty property,
                    final Mapper<?> valueMapper,
                    final Class<?> mappedClassType) {
        this.property = property;
        this.mappedClassType = mappedClassType;
        this.valueMapper = valueMapper;
    }
}
