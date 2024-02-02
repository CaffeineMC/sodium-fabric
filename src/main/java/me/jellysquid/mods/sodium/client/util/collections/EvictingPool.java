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
    Link[] links;
    int oldestIdx;
    int newestIdx;

    IntArrayFIFOQueue free;
    int top;

    int size;

    public EvictingPool(int size) {
        this.pool = (E[]) new Object[size];
        this.links = new Link[size];
        Arrays.asList(this.links).replaceAll(i -> new Link(-1, -1));

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
                int nextOldest = links[this.oldestIdx].next();
                links[nextOldest].prev(-1);

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
        Link link = this.links[index];

        System.out.println(link);

        if (index == oldestIdx) {
            links[link.next()].prev(-1);
            this.oldestIdx = link.next();
        } else if (index == newestIdx) {
            links[link.prev()].next(-1);
            this.newestIdx = link.prev();
        } else {
            links[link.prev()].next(link.next());
            links[link.next()].prev(link.prev());
        }

        pool[index] = null;
    }

    @Override
    public int size() {
        return top - free.size();
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

        if (prevNewest >= 0) links[prevNewest].next(newestIdx);
        links[newestIdx] = new Link(prevNewest, -1);
    }

    /**
     * Added to prevent confusion on left/right
     */
    private static class Link extends IntIntMutablePair {
        public Link(int prev, int next) {
            super(prev, next);
        }

        public int next() {
            return this.right;
        }

        public void next(int next) {
            this.right = next;
        }

        public int prev() {
            return this.left;
        }

        public void prev(int prev) {
            this.left = prev;
        }
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
        Link currentLink;

        LinkItr() {
            this.cursor = -1;
            this.currentLink = null;
        }

        @Override
        public boolean hasNext() {
            return this.currentLink.next() != -1;
        }

        @Override
        public E next() {
            if (!beganIterating) {
                this.cursor = EvictingPool.this.newestIdx;
                beganIterating = true;
            } else {
                this.cursor = this.currentLink.next();
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