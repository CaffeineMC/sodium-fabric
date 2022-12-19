package net.caffeinemc.sodium.util;

import java.util.Collection;
import java.util.List;

public class ListUtil {
    public static <T> void updateList(Collection<T> collection, T[] before, T[] after) {
        if (before != null && before.length > 0) {
            collection.removeAll(List.of(before));
        }

        if (after != null && after.length > 0) {
            collection.addAll(List.of(after));
        }
    }
}
