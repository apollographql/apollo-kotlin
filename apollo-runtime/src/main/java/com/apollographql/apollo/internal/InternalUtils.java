package com.apollographql.apollo.internal;

import java.util.Set;

class InternalUtils {

    /**
     * Checks if two {@link Set} are disjoint. Returns true if the sets don't have a single common element. Also returns
     * true if either of the sets is null.
     *
     * @param setOne the first set
     * @param setTwo the second set
     * @param <E>    the value type contained within the sets
     * @return True if the sets don't have a single common element or if either of the sets is null.
     */
    static <E> boolean areDisjoint(Set<E> setOne, Set<E> setTwo) {
        if (setOne == null || setTwo == null) {
            return true;
        }
        Set<E> smallerSet = setOne;
        Set<E> largerSet = setTwo;
        if (setOne.size() > setTwo.size()) {
            smallerSet = setTwo;
            largerSet = setOne;
        }
        for (E el : smallerSet) {
            if (largerSet.contains(el)) {
                return false;
            }
        }
        return true;
    }
}
