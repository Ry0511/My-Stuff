package com.ry.useful;

import lombok.Data;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Java class created on 20/06/2022 for usage in project My-Stuff.
 *
 * @author -Ry
 */
public class StreamUtils {

    /**
     * Creates a stream that is Non-Null and populated by the supplying
     * function, endless, or until the function returns null.
     *
     * @param supplier The generator for the stream. Should return null to mark
     * end of stream.
     * @param <T> The type of the elements for this stream.
     * @return New stream that is lazily populated by the elements on the
     * stream.
     */
    public static <T> Stream<T> createStreamLazily(final Supplier<T> supplier) {
        return StreamSupport.stream(new NullSpliterator<>(supplier), false);
    }

    @Data
    private static class NullSpliterator<T> implements Spliterator<T> {

        private final Supplier<T> generator;

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            final T next = generator.get();

            if (next == null) {
                return false;
            } else {
                action.accept(next);
                return true;
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return Spliterator.NONNULL;
        }
    }
}
