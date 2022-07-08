package com.ry.useful;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * Java class created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Entity<V> {

    /**
     * The value held at this entity.
     */
    private V value;

    /**
     * Creates an empty entity.
     *
     * @param <V> The type of the value held at the entity.
     * @return Empty entity.
     */
    public static <V> Entity<V> empty() {
        return new Entity<>();
    }

    /**
     * @param fn The comparer and setting function.
     * @return The new value.
     */
    public V compareAndSet(@NonNull final Function<V, V> fn) {
        return value = fn.apply(value);
    }

    /**
     * @return {@code true} if the current value is not null.
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * @return Optional of the current value.
     */
    public Optional<V> asOptional() {
        return Optional.ofNullable(value);
    }
}
