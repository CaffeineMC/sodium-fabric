package net.caffeinemc.sodium.util;

import java.util.Arrays;
import java.util.Collection;

public class ListUtil {
    public static <T> void updateList(Collection<T> collection, T[] before, T[] after) {
        if (before != null) {
            collection.removeAll(Arrays.asList(before));
        }

        if (after != null) {
            collection.addAll(Arrays.asList(after));
        }
    }
}
