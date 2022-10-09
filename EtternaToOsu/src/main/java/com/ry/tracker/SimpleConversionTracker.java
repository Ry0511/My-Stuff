package com.ry.tracker;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Synchronized;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Java class created on 27/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
@Data
@Getter(AccessLevel.PRIVATE)
public class SimpleConversionTracker {

    private final List<AudioConversionListener> audioListenerList = new ArrayList<>();
    private final List<BackgroundConversionListener> backgroundListenerList = new ArrayList<>();

    @Synchronized
    private <T> void synchronous(final Supplier<T> supplier, final Consumer<T> action) {
        action.accept(supplier.get());
    }

    public void addAudioConversionListener(final AudioConversionListener listener) {
        synchronous(this::getAudioListenerList, xs -> xs.add(listener));
    }

    public void forEachAudioListener(final Consumer<AudioConversionListener> action) {
        synchronous(this::getAudioListenerList, xs -> xs.forEach(action));
    }

    public void addBackgroundListener(final BackgroundConversionListener listener) {
        synchronous(this::getBackgroundListenerList, xs -> xs.add(listener));
    }

    public void forEachBackgroundListener(final Consumer<BackgroundConversionListener> action) {
        synchronous(this::getBackgroundListenerList, xs -> xs.forEach(action));
    }
}
