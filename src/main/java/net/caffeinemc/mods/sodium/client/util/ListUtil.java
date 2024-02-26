package net.caffeinemc.mods.sodium.client.util;

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
