package net.caffeinemc.sodium.util;

import java.util.Iterator;
import java.util.List;

public class IteratorUtils {
    public static <T> Iterator<T> reverse(List<T> list) {
        var iterator = list.listIterator(list.size());
    
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasPrevious();
            }
    
            @Override
            public T next() {
                return iterator.previous();
            }
        };
    }
}
