package net.caffeinemc.sodium.util;

import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ReferenceList;

import java.util.Iterator;

public class IteratorUtils {
    public static <T> Iterator<T> reversibleIterator(ReferenceList<T> list, boolean reverse) {
        var iterator = list.listIterator(reverse ? list.size() : 0);

        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return reverse ? iterator.hasPrevious() : iterator.hasNext();
            }

            @Override
            public T next() {
                return reverse ? iterator.previous() : iterator.next();
            }
        };
    }
}
