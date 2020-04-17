package me.jellysquid.mods.sodium.client.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Iterator;

public class RenderList<T> extends ObjectArrayList<T> {
    public Iterator<T> iterator(boolean backwards) {
        if (backwards) {
            return new Iterator<T>() {
                private final T[] data = RenderList.this.a;

                private int pos = RenderList.this.size() - 1;

                @Override
                public boolean hasNext() {
                    return this.pos >= 0;
                }

                @Override
                public T next() {
                    return this.data[this.pos--];
                }
            };
        } else {
            return new Iterator<T>() {
                private final T[] data = RenderList.this.a;
                private final int lim = RenderList.this.size;

                private int pos = 0;

                @Override
                public boolean hasNext() {
                    return this.pos < this.lim;
                }

                @Override
                public T next() {
                    return this.data[this.pos++];
                }
            };
        }
    }
}
