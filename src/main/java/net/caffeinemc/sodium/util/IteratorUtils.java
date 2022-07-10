package net.caffeinemc.sodium.util;

import java.util.Iterator;
import java.util.List;

public class IteratorUtils {
    public static <T> Iterator<T> reversibleIterator(List<T> list, boolean reverse) {
        var iterator = list.listIterator(reverse ? list.size() : 0);

        return new Iterator<>() {
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
