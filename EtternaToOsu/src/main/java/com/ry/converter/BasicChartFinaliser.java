package com.ry.converter;

import com.ry.etterna.EtternaFile;
import com.ry.etterna.msd.SkillSet;
import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etterna.reader.EtternaProperty;
import com.ry.etterna.util.MSDChart;
import com.ry.osu.builderRedone.HitObject;
import com.ry.osu.builderRedone.TagsBuilder;
import com.ry.osu.builderRedone.TemplateFile;
import com.ry.osu.builderRedone.TimingPoint;
import com.ry.osu.builderRedone.util.TimingInfo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

/**
 * Java class created on 27/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
public class BasicChartFinaliser implements ChartFinaliser {

    /**
     * The file format for .osu files.
     */
    private static final String CHART_NAME_FORMAT = "(%s - %s) - ([%s] - %s).osu";

    /**
     * The version format string for charts.
     */
    private static final String CHART_VERSION_FORMAT = "%s - [%s] %s (%.2fx)";

    /**
     * The minimum length in milliseconds for any Hold note.
     */
    private static final BigDecimal MIN_HOLD_LENGTH = new BigDecimal("20");

    /**
     * Action completes the builder.
     */
    private final UnaryOperator<TemplateFile.TemplateFileBuilder> finaliseBuilder;

    /**
     * Applied to all timing points just before they're compiled.
     */
    private final UnaryOperator<TimingPoint.TimingPointBuilder> finaliseTimingPoint;

    /**
     * Applied to all hit-objects just before they're compiled.
     */
    private final UnaryOperator<HitObject.HitObjectBuilder> finaliseHitObject;

    /**
     * Gets the difficulty ID Character.
     *
     * @param index The difficulty index. 0 -> k
     * @return Single character.
     */
    private static String getDiffId(final int index) {
        return "" + (char) (0x03B1 + index);
    }

    /**
     * Maps the provided chart to a complete Template file which can be written
     * to a file.
     *
     * @param chart The chart to map.
     * @param audioName The audio file; Can be the empty string.
     * @param bgName The bg file name; Can be the empty string.
     * @return Complete template file.
     */
    @Override
    public TemplateFile mapToTemplate(final MSDChart chart,
                                      final String audioName,
                                      final String bgName) {
        chart.accept();
        final TimingInfo timingInfo = initInfo(chart.getInfo());
        final EtternaFile eFile = chart.getEtternaFile();
        assert eFile.getOffset().isPresent();

        final String tags = new TagsBuilder()
                .add(chart.isBaseRate() ? "rate:no" : "rate:y:" + chart.getRate())
                .add(eFile.getOffset().get().signum() == -1 ? "offset:negative" : "offset:positive")
                .add(eFile.hasPackStructure() ? "pack:y:" + eFile.getPackFolder().getName() : "pack:no")
                .add(chart.getMsd().getNotBestSkillTag())
                .add(chart.getMsd().getMsdFilterTag("12", "40", "1"))
                .build();

        final var artistProp = eFile.getProperty(EtternaProperty.ARTIST);
        final String artist;
        if (artistProp.isEmpty()) {
            artist = "Unknown Artist";
        } else {
            artist = artistProp.getProcessed();
        }

        return TemplateFile.from(
                timingInfo,
                x -> finaliseBuilder.apply(
                        x.setTags(tags)
                                .setTitleBoth(getTitle(chart))
                                .setVersion(getVersion(chart))
                                .setSource(chart.getMsd().sourceStr())
                                .setArtistBoth(artist)
                                .setAudioFileName(audioName)
                                .setBackgroundFile(bgName)
                ));
    }

    /**
     * Gets the title of the chart. If the chart has the pack structure then the
     * pack name is returned. However, if it doesn't then either '__Title
     * Missing' is returned or the title of the Etterna file is returned.
     *
     * @param chart The chart to get the title for.
     * @return Aforementioned title.
     */
    private String getTitle(final MSDChart chart) {
        if (chart.getEtternaFile().hasPackStructure()) {
            return chart.getEtternaFile().getPackFolder().getName();
        } else {
            final var prop = chart.getEtternaFile().getProperty(EtternaProperty.TITLE);
            if (prop.isEmpty()) {
                return "__Title Missing";
            } else {
                return prop.getProcessed();
            }
        }
    }

    /**
     * Gets the version for this chart.
     * @param chart The chart to get the version for.
     * @return Diff ID + MSD + chart name + rate
     * @see #CHART_VERSION_FORMAT
     */
    private String getVersion(final MSDChart chart) {
        final String chartName;
        final var prop = chart.getEtternaFile().getProperty(EtternaProperty.TITLE);
        if (!prop.isEmpty()) {
            chartName = prop.getProcessed();
        } else {
            chartName = "__Unknown Title";
        }

        return String.format(
                CHART_VERSION_FORMAT,
                getDiffId(chart.getInfo().getDifficultyIndex()),
                chart.getMsd().getSkill(SkillSet.OVERALL).toPlainString(),
                chartName,
                Float.parseFloat(chart.getRate())
        );
    }

    /**
     * @param info The chart to load hit object data from.
     * @return Timing info for all notes.
     */
    private TimingInfo initInfo(final EtternaNoteInfo info) {
        // I really hate that the default is inverted...
        final AtomicBoolean unInherited = new AtomicBoolean(true);
        return TimingInfo.loadFromEtternaInfo(
                info,
                x -> x.setUnInherited(unInherited.getAndSet(false)),
                finaliseHitObject
        );
    }

    /**
     * Name of the .osu file.
     *
     * @param chart The chart the get the filename for.
     * @return Filename using only
     */
    @Override
    public String getChartFilename(final MSDChart chart) {
        final String name = chart.getEtternaFile().getSmFile().getName();
        return String.format(
                CHART_NAME_FORMAT,
                getDiffId(chart.getInfo().getDifficultyIndex()),
                chart.getRate(),
                chart.getMsd().getSkill(SkillSet.OVERALL).toPlainString(),
                name.substring(0, name.lastIndexOf("."))
        );
    }
}
