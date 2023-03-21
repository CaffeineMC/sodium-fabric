package me.jellysquid.mods.sodium.client.util;

import java.util.NoSuchElementException;

public class SectionIterator {
    private final byte[] sections;

    private final int step;

    private int cur;
    private int rem;

    public SectionIterator(byte[] sections, int start, int end, boolean reverse) {
        this.sections = sections;

        this.rem = end - start;

        this.step = reverse ? -1 : 1;
        this.cur = reverse ? end - 1 : start;
    }

    public boolean hasNext() {
        return this.rem > 0;
    }

    public int next() {
        int result = this.sections[this.cur] & 0xFF;

        this.cur += this.step;
        this.rem--;

        return result;
    }
}
