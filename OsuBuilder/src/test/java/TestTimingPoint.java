import com.ry.etterna.EtternaFile;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.osu.builderRedone.TemplateFile;
import com.ry.osu.builderRedone.util.TimingInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public class TestTimingPoint {

    private static final File FILE = new File("C:\\Games\\Etterna\\Songs\\Blos Pack 5\\Alright\\Alright.sm");

    public static void main(String[] args) {

        try {
            EtternaFile ef = new EtternaFile(FILE);

            for (EtternaNoteInfo info : ef.getNoteInfo()) {
                info.timeNotesWith(ef.getTimingInfo());

                TimingInfo ti = TimingInfo.loadFromEtternaInfo(info, builder -> builder, builder -> builder);

                String s = TemplateFile.builder()
                        .setHitObjects(ti.getHitObjects())
                        .setTimingPoints(ti.getTimingPoints())
                        .build()
                        .compile();

                Files.writeString(Path.of("C:\\Users\\-Ry\\Desktop\\Test out\\test.osu"), s);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
