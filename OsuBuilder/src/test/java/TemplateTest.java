import com.ry.osu.builderRedone.TemplateFile;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public class TemplateTest {

    public static void main(final String[] args) {
//        final TemplateFile templateFile = new TemplateFile();
//
//        for (final TemplateFile.Element e : TemplateFile.Element.values()) {
//            templateFile.setElement(e, "ELEMENT_" + e.name());
//        }
//
//        System.out.println(templateFile.compile());


        final Instant start = Instant.now();

        TemplateFile file = TemplateFile.builder()
                .setHitObjects("ELEM::HIT_OBJECTS")
                .setTimingPoints("ELEM:TIMING_POINTS")
                .setBackgroundFile("ELEM::BG_FILE")
                .setOverallDifficulty("ELEM::OD")
                .setHpDrain("ELEM::HP_DRAIN")
                .setTags("ELEM::TAGS")
                .setSource("ELEM::SOURCE")
                .setVersion("ELEM::VERSION")
                .setCreator("ELEM::CREATOR")
                .setArtistUnicode("ELEM::ARTIST_UNICODE")
                .setArtist("ELEM::ARTIST")
                .setTitleUnicode("ELEM::TITLE_UNICODE")
                .setTitle("ELEM::TITLE")
                .setAudioFileName("ELEM:AUDIO_FILE")
//                .streamElements((b, s) -> s.forEach(e -> b.setElement(e, "ELEM::" + e)))
                .build();

        System.out.println(file.compile());

        final Instant end = Instant.now();
        System.out.println("Total Time: " + Duration.between(start, end).toMillis());
    }
}
