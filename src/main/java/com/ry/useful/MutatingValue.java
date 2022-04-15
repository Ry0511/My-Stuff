package com.ry.useful;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Java class created on 08/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@Data
@AllArgsConstructor(staticName = "of")
public class MutatingValue<T> {

    /**
     * The current value.
     */
    private T value;

    /**
     * Mutates the current value to the new value.
     *
     * @param mutator The mutating function.
     * @return The mutator for chaining.
     */
    public MutatingValue<T> mutateValue(@NonNull final Function<T, T> mutator) {
        this.value = mutator.apply(getValue());
        return this;
    }

    /**
     * Mutates the inner structure of 'T' that is, the given consumer is assumed
     * to modify 'T' in a mutable way.
     *
     * @param stateMutator The mutating action.
     * @return The mutator for chaining.
     */
    public MutatingValue<T> mutateState(
            @NonNull final Consumer<T> stateMutator) {
        stateMutator.accept(getValue());
        return this;
    }

    /**
     * Mutates the given value iff the given predicate yields true.
     *
     * @param condition The aforementioned predicate.
     * @param mutator The mutator.
     * @return The mutator for chaining.
     */
    public MutatingValue<T> mutateValueIf(
            @NonNull final Predicate<T> condition,
            @NonNull final Function<T, T> mutator) {
        if (condition.test(getValue())) {
            this.value = mutator.apply(getValue());
        }

        return this;
    }

    /**
     * Mutates the current value type to a new type.
     *
     * @param mutator The mutator function.
     * @param <M> The new Value type.
     * @return Mutator of type M whose value is the current value with the
     * applied type mutator.
     */
    public <M> MutatingValue<M> mutateType(
            @NonNull final Function<T, M> mutator) {
        return MutatingValue.of(mutator.apply(value));
    }

    /**
     * Mutates the value type iff the given predicate yields true returning an
     * intermediary confirmation object.
     *
     * @param condition The condition to mutate.
     * @param mutator The type mutator.
     * @param <M> The mutated type.
     * @return Failable mutation object whose original value is this.
     */
    public <M> FailableMutation<T, M> mutateTypeIf(
            @NonNull final Predicate<T> condition,
            @NonNull final Function<T, M> mutator) {

        if (condition.test(getValue())) {
            return new FailableMutation<>(
                    this,
                    MutatingValue.of(mutator.apply(getValue()))
            );
        } else {
            return new FailableMutation<>(this, null);
        }
    }

    @Data
    public static final class FailableMutation<T, M> {
        private final MutatingValue<T> original;
        private final MutatingValue<M> mutated;

        public FailableMutation(final MutatingValue<T> t,
                                final MutatingValue<M> m) {
            original = t;
            mutated = m;
        }

        public boolean hadMutation() {
            return mutated != null;
        }

        /**
         * Allows the branch handling of both cases of the failable mutation,
         * that is, if the Mutation was successful then only 'mutated' is
         * invoked, however if the mutation was not successful then only the
         * 'original' is invoked.
         *
         * @param original iff the mutation failed do this.
         * @param mutated iff the mutation succeeded do this.
         * @return this.
         */
        public FailableMutation<T, M> handle(
                final Consumer<MutatingValue<T>> original,
                final Consumer<MutatingValue<M>> mutated) {
            if (hadMutation()) {
                if (mutated != null) {
                    mutated.accept(getMutated());
                }
            } else {
                if (original != null) {
                    original.accept(getOriginal());
                }
            }

            return this;
        }

        public Optional<MutatingValue<M>> getMutatedOpt() {
            return Optional.ofNullable(mutated);
        }
    }
}
