package com.ry.etternaToOsu;

import com.ry.etterna.note.EtternaNoteInfo;
import com.ry.etternaToOsu.osu.HitObject;
import com.ry.etternaToOsu.osu.TimingPoint;
import com.ry.useful.Entity;
import com.ry.vsrg.BPM;
import lombok.Data;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Java class created on 12/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
public class OsuNoteInfo {

    /**
     * Map of osu elements and their values to write.
     */
    private final Map<OsuElement, String> elementMap;

    /**
     * List of all timing points for the map.
     */
    private final List<TimingPoint> timingPoints;

    /**
     * List of all hit objects for the map.
     */
    private final List<HitObject> hitObjects;

    /**
     * Constructs the info from the base Etterna timing data.
     *
     * @param notes Etterna note info that has been timed.
     */
    public OsuNoteInfo(final EtternaNoteInfo notes) {
        if (notes.getCurTimingInfo() == null) {
            throw new NullPointerException("Timing data must not be null...");
        }
        timingPoints = new ArrayList<>();
        hitObjects = new ArrayList<>();

        final Entity<BPM> prevBpm = Entity.empty();
        notes.forEachNote((measure, row) -> {

            // Load timing data
            final BigDecimal startTime = row.getStartTime();
            final BPM curBpm = row.getBpm();
            final boolean inherit = timingPoints.size() > 0;
            if (!prevBpm.isPresent() || !prevBpm.getValue().equals(curBpm)) {
                prevBpm.setValue(curBpm);
                timingPoints.add(new TimingPoint(
                        startTime,
                        curBpm.getValue(),
                        measure.size(),
                        inherit
                ));
            }

            // Load hit objects
            row.stream().filter(x -> x.getStartNote().isTap()
                            || x.getStartNote().isHoldHead())
                    .forEach(x -> hitObjects.add(new HitObject(x)));
        });

        this.elementMap = new HashMap<>();
    }

    /**
     * Sets the provided file element to the provided value.
     *
     * @param elem The element to write.
     * @param value The value of the element.
     */
    public void setElement(final OsuElement elem,
                           final String value) {
        this.elementMap.put(elem, value);
    }

    /**
     * Loads all mapped data to a template file writer.
     *
     * @return Template file writer with all mapped elements written and set.
     * @throws IOException Iff the template failed to load.
     */
    public OsuTemplate asTemplate() throws IOException {
        final OsuTemplate template = new OsuTemplate();

        // Properties
        this.elementMap.forEach(template::setElement);

        // Timing points
        final StringJoiner tp = new StringJoiner(System.lineSeparator());
        this.timingPoints.forEach(x -> tp.add(x.getArgs().toString()));
        template.setTimingPoints(tp.toString());

        // Hit objects
        final StringJoiner hitObj = new StringJoiner(System.lineSeparator());
        this.hitObjects.forEach(x -> hitObj.add(x.getHitObj()));
        template.setHitObjects(hitObj.toString());

        return template;
    }
}
