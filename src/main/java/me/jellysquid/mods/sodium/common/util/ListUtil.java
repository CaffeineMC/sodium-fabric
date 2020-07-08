package me.jellysquid.mods.sodium.common.util;

import java.util.Collection;

public class ListUtil {
    public static <T> void updateList(Collection<T> collection, Collection<T> before, Collection<T> after) {
        if (!before.isEmpty()) {
            collection.removeAll(before);
        }

        if (!after.isEmpty()) {
            collection.addAll(after);
        }
    }

}
