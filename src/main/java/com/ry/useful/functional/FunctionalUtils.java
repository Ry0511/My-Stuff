package com.ry.useful.functional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Java class created on 07/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FunctionalUtils {

    //
    // I can never get this idea just right since there are too many things
    // to think about in a single Function however the main idea is to
    // encapsulate large chunks of boilerplate with a single lambda.
    //

    /**
     * Simple If Predicate then Action, else empty.
     *
     * @param condition The condition.
     * @param action The action.
     * @param <R> The type of the result.
     * @return Empty iff false, else R.
     * @throws Exception Propagate.
     */
    public static <R> Optional<R> ifPred(
            @NonNull final EXSupplier<Boolean> condition,
            @NonNull final EXSupplier<R> action) throws Exception {

        if (condition.get()) {
            return Optional.of(action.get());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Simple iff pred then A else B.
     *
     * @param condition The condition.
     * @param onTrue A.
     * @param onFalse B.
     * @param <R> The type of the result.
     * @return iff true then A, else B.
     * @throws Exception Propagate.
     */
    public static <R> R ifPred(
            @NonNull final EXSupplier<Boolean> condition,
            @NonNull final EXSupplier<R> onTrue,
            @NonNull final EXSupplier<R> onFalse) throws Exception {

        if (condition.get()) {
            return onTrue.get();
        } else {
            return onFalse.get();
        }
    }

    /**
     * Performs the given action collecting the results until false is
     * achieved.
     *
     * @param condition The condition to loop.
     * @param action The action to perform.
     * @param <R> The type of the data to collect.
     * @return All collected data, even if none.
     * @throws Exception Propagate.
     */
    public static <R> List<R> whileTrueCollect(
            @NonNull final EXSupplier<Boolean> condition,
            @NonNull final EXSupplier<R> action) throws Exception {

        final List<R> xs = new ArrayList<>();
        while (condition.get()) {
            xs.add(action.get());
        }
        return xs;
    }

    ///////////////////////////////////////////////////////////////////////////
    // NO FORCED EXCEPTION ON THESE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Simple If Predicate then Action, else empty.
     *
     * @param condition The condition.
     * @param action The action.
     * @param <R> The type of the result.
     * @return Empty iff false, else R.
     */
    public static <R> Optional<R> ifPred(
            @NonNull final Supplier<Boolean> condition,
            @NonNull final Supplier<R> action) {

        if (condition.get()) {
            return Optional.of(action.get());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Simple iff pred then A else B.
     *
     * @param condition The condition.
     * @param onTrue A.
     * @param onFalse B.
     * @param <R> The type of the result.
     * @return iff true then A, else B.
     */
    public static <R> R ifPred(
            @NonNull final Supplier<Boolean> condition,
            @NonNull final Supplier<R> onTrue,
            @NonNull final Supplier<R> onFalse) {

        if (condition.get()) {
            return onTrue.get();
        } else {
            return onFalse.get();
        }
    }

    /**
     * Performs the given action collecting the results until false is
     * achieved.
     *
     * @param condition The condition to loop.
     * @param action The action to perform.
     * @param <R> The type of the data to collect.
     * @return All collected data, even if none.
     */
    public static <R> List<R> whileTrueCollect(
            @NonNull final Supplier<Boolean> condition,
            @NonNull final Supplier<R> action) {

        final List<R> xs = new ArrayList<>();
        while (condition.get()) {
            xs.add(action.get());
        }
        return xs;
    }
}
