package me.jellysquid.mods.sodium.client.util.collections;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Functions similarly to an EvictingQueue, except existing elements
 * stay in the same place in memory until they are removed or replaced
 * in favor of a new element.
 * @param <E>
 */
public class EvictingPool<E> extends AbstractCollection<E> {
    E[] pool;
    IntIntMutablePair[] links;
    int oldestIdx;
    int newestIdx;

    IntArrayFIFOQueue free;
    int top;

    int size;

    public EvictingPool(int size) {
        this.pool = (E[]) new Object[size];
        this.links = new IntIntMutablePair[size];
        Arrays.asList(this.links).replaceAll(i -> new IntIntMutablePair(-1, -1));

        this.free = new IntArrayFIFOQueue();
        this.top = 0;
        this.size = size;

        this.oldestIdx = -1;
        this.newestIdx = -1;
    }

    public boolean add(E item) {
        if (free.isEmpty()) {
            if (this.top < size) {
                pool[this.top] = item;
                this.newest(this.top);

                ++this.top;
            } else {
                pool[this.oldestIdx] = item;
                int nextOldest = links[this.oldestIdx].rightInt();
                links[nextOldest].left(-1);

                this.newest(this.oldestIdx);
                this.oldestIdx = nextOldest;
            }
        } else {
            int freeIdx = free.dequeueInt();
            pool[freeIdx] = item;
            this.newest(freeIdx);
        }

        return true;
    }

    public void remove(int index) {
        free.enqueue(index);
        IntIntMutablePair link = this.links[index];

        if (index == oldestIdx) {
            links[link.leftInt()].right(-1);
            this.oldestIdx = link.leftInt();
        } else if (index == newestIdx) {
            links[link.rightInt()].left(-1);
            this.newestIdx = link.rightInt();
        } else {
            links[link.leftInt()].right(link.rightInt());
            links[link.rightInt()].left(link.leftInt());
        }

        pool[index] = null;
    }

    @Override
    public int size() {
        return top - free.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public @NotNull Iterator<E> iterator() {
        if (this.saturation() < Itr.MAGIC_RATIO) {
            return new LinkItr();
        } else {
            return new IncrementItr();
        }
    }

    private double saturation() {
        return (double) top / free.size();
    }

    private void newest(int idx) {
        int prevNewest = this.newestIdx;
        this.newestIdx = idx;

        links[prevNewest].right(newestIdx);
        links[newestIdx] = new IntIntMutablePair(prevNewest, -1);
    }

    private abstract class Itr implements Iterator<E> {
        static final double MAGIC_RATIO = 0.50;
        int cursor = -1;

        @Override
        public void remove() {
            EvictingPool.this.remove(cursor);
        }
    }

    private class LinkItr extends Itr {
        boolean beganIterating;
        IntIntMutablePair currentLink;

        LinkItr() {
            this.cursor = -1;
            this.currentLink = null;
        }

        @Override
        public boolean hasNext() {
            return this.currentLink.rightInt() != -1;
        }

        @Override
        public E next() {
            if (!beganIterating) {
                this.cursor = EvictingPool.this.newestIdx;
                beganIterating = true;
            } else {
                this.cursor = this.currentLink.rightInt();
            }

            this.currentLink = EvictingPool.this.links[this.cursor];
            return EvictingPool.this.pool[this.cursor];
        }
    }

    private class IncrementItr extends Itr {
        E next = null;
        int nextCursor = -1;

        IncrementItr() {
            this.cursor = -1;
            this.next = this.getNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public E next() {
            E ret = this.next;
            this.cursor = this.nextCursor;
            this.next = this.getNext();
            return ret;
        }

        private E getNext() {
            E ret = null;
            while (ret == null && this.nextCursor + 1 < EvictingPool.this.top) {
                ++this.nextCursor;
                ret = EvictingPool.this.pool[this.nextCursor];
            }

            return ret;
        }
    }
}