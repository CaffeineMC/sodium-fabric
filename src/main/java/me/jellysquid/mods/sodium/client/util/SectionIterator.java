package me.jellysquid.mods.sodium.client.util;

import me.jellysquid.mods.sodium.core.types.CLocalSectionList;

public class SectionIterator {
    private final CLocalSectionList sectionList;

    private final int step;

    private int cur;
    private int rem;

    public SectionIterator(CLocalSectionList sections, int start, int end, boolean reverse) {
        this.sectionList = sections;

        this.rem = end - start;

        this.step = reverse ? -1 : 1;
        this.cur = reverse ? end - 1 : start;
    }

    public SectionIterator(CLocalSectionList sections, boolean reverse) {
        this(sections, 0, sections.size(), reverse);
    }

    public boolean hasNext() {
        return this.rem > 0;
    }

    public int next() {
        int result = this.sectionList.listElement(this.cur);

        this.cur += this.step;
        this.rem--;

        return result;
    }
}
