package com.ry.useful.database;

import com.ry.useful.Failable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Java class created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SQLiteDBUtils {

    /**
     * Processes the given result set and constructs a new instance of the
     * provided target class.
     *
     * @param target The class to load.
     * @param results The data to populate into it.
     * @param <T> The type of the class to create.
     * @return New instance of 'T' with all Column data specified.
     */
    public static <T> T initTarget(final Class<T> target,
                                   final ResultSet results) {
        final SQLiteResultMap map = target.getAnnotation(SQLiteResultMap.class);
        if (map == null) throw new Error("Missing Annotation: " + target);

        try {
            final T obj = target.cast(target.getConstructor().newInstance());
            forAllColumnHandles(target, (v) -> {

                // Override jvm
                if (map.isOverrideJvm()) {
                    v.setAccessible(true);
                }

                // Inferred not null
                final Column col = v.getAnnotation(Column.class);
                v.invoke(obj, results.getObject(col.value()));
            });

            return obj;

            // Re-throw generally is bad, however in this case its fine
        } catch (final Exception e) {
            throw new Error(e.getMessage());
        }
    }

    /**
     * Applies the given action for all methods that are annotated with {@link
     * Column}.
     *
     * @param cls The class to process.
     * @param action The action to apply.
     * @throws Exception If the action fails.
     */
    private static void forAllColumnHandles(
            final Class<?> cls,
            final Failable<Method> action) throws Exception {

        for (final Method m : getAllMethods(cls)) {
            if (m.isAnnotationPresent(Column.class)) {
                action.invoke(m);
            }
        }
    }

    /**
     * Gets all methods of a class leading up to its super class. Note that
     * Methods from the class Object are not collected.
     *
     * @param cls The class to collect the methods for.
     * @return All declared methods, and all inherited declared methods.
     */
    private static List<Method> getAllMethods(final Class<?> cls) {
        final List<Method> xs = new ArrayList<>();

        Class<?> temp = cls;
        while (temp != null) {
            xs.addAll(List.of(temp.getDeclaredMethods()));
            temp = temp.getSuperclass();
        }

        return xs;
    }
}
