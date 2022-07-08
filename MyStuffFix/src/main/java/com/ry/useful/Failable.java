package com.ry.useful;

/**
 * Java interface created on 06/04/2022 for usage in project FunctionalUtils.
 *
 * @author -Ry
 */
public interface Failable<A> {
    void invoke(A argument) throws Exception;
}
