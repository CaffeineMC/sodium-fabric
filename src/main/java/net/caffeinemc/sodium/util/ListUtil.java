package net.caffeinemc.sodium.util;

import java.util.Collection;

public class ListUtil {
    public static <T> void updateList(Collection<T> collection, T[] before, T[] after) {
        if (before.length > 0) {
            for (T ref : before) {
                collection.remove(ref);
            }
        }

        if (after.length > 0) {
            for (T ref : after) {
                collection.add(ref);
            }
        }
    }
}
