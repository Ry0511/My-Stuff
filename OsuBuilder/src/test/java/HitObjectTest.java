import com.ry.etterna.note.Note;
import com.ry.etterna.note.NoteType;
import com.ry.osu.builder.BuildUtils;
import com.ry.osu.builderRedone.HitObject;
import com.ry.osu.builderRedone.sound.HitSound;
import com.ry.osu.builderRedone.sound.HitType;
import com.ry.osu.builderRedone.sound.SampleSet;
import com.ry.osu.builderRedone.sound.Volume;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public class HitObjectTest {
    public static void main(String[] args) {

        BigDecimal start = new BigDecimal("1.425");

        HitObject obj = HitObject.builder()
                .setX(0, 4)
                .setY(0)
                .setType(HitType.HIT)
                .setHitSound(HitSound.FINISH)
                .setStartTime(start.multiply(new BigDecimal("1000"), MathContext.DECIMAL64))
                .setArgs(HitObject.ObjectArgs.builder()
                        .setVolume(Volume.FULL)
                        .setSampleSet(SampleSet.NORMAL)
                        .setAdditionSet(SampleSet.AUTO)
                        .setIndex(HitSound.HIT)
                        .build())
                .build();

        System.out.println(obj.compile());

        Note n = new Note(NoteType.TAP);
        n.setStartTime(start);
        n.setColumn(0);
        System.out.println(BuildUtils.noteToHitObject(n, 4).asOsuStr());

    }
}
