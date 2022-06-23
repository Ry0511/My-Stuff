import com.ry.etterna.EtternaFile;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.osu.builderRedone.TemplateFile;
import com.ry.osu.builderRedone.TimingInfo;

import java.io.File;
import java.io.IOException;
import java.util.stream.IntStream;

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

                TimingInfo ti = TimingInfo.loadFromEtternaInfo(info, (builder, bpm) -> builder.build(), ((builder, note, row) -> builder.build()));
                String s = TemplateFile.builder()
                        .setHitObjects(ti.getHitObjects())
                        .setTimingPoints(ti.getTimingPoints())
                        .build()
                        .compile();

                System.out.println(s);
                IntStream.range(0, 120).forEach(x -> System.out.print(">"));
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
