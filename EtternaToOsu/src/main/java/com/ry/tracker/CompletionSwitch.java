package com.ry.tracker;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

/**
 * Java class created on 25/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class CompletionSwitch {

    /**
     * The cancelled status of the main task.
     */
    @Getter
    private boolean isCancelled = false;

    /**
     * The complete status of the main task.
     */
    @Getter
    private boolean isComplete = false;

    /**
     * List of all tasks that are to run when completed.
     */
    private List<Runnable> runOnComplete = new ArrayList<>();

    @Synchronized
    private void withList(final Consumer<List<Runnable>> action) {
        action.accept(runOnComplete);
    }

    public void runOnComplete(final Runnable onComplete) {
        withList(xs -> {
            if (isCancelled()) {
                return;
            }

            if (!isComplete()) {
                xs.add(onComplete);
            } else {
                onComplete.run();
            }
        });
    }

    public void setComplete(final boolean isComplete) {
        this.isComplete = isComplete;
        if (isComplete) {
            pingComplete();
        }
    }

    private void pingComplete() {
        withList(xs -> {
            final ListIterator<Runnable> iter = xs.listIterator();
            while (iter.hasNext()) {
                iter.next().run();
                iter.remove();
            }
        });
    }

    public void setCancelled(final boolean isCancelled) {
        withList(xs -> {
            this.isCancelled = isCancelled;
            if (isCancelled) {
                xs.clear();
            }
        });
    }
}
