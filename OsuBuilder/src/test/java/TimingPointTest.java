import com.ry.osu.builderRedone.TimingPoint;

import java.math.BigDecimal;

/**
 * Java class created on 23/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public class TimingPointTest {
    public static void main(String[] args) {
        TimingPoint tp = TimingPoint.builder()
                .setBeatLength(new BigDecimal("120.0"))
                .setMeter(4)
                .setTime(new BigDecimal("1.23"), true)
                .initDefaults()
                .build();

        System.out.println(tp.compile());
    }
}
