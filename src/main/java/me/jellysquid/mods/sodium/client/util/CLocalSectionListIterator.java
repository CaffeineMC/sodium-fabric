package me.jellysquid.mods.sodium.client.util;

import me.jellysquid.mods.sodium.client.render.chunk.graph.LocalSectionIndex;
import me.jellysquid.mods.sodium.core.types.CLocalSectionList;
import org.lwjgl.system.MemoryUtil;

public class CLocalSectionListIterator {
    private final long ptr;

    private final int step;

    private int cur;
    private int rem;

    public CLocalSectionListIterator(CLocalSectionList sections, int start, int end, boolean reverse) {
        this.ptr = sections.arrayBase();

        this.rem = end - start;

        this.step = reverse ? -1 : 1;
        this.cur = reverse ? end - 1 : start;
    }

    public CLocalSectionListIterator(CLocalSectionList sections, boolean reverse) {
        this(sections, 0, sections.size(), reverse);
    }

    public boolean hasNext() {
        return this.rem > 0;
    }

    public int next() {
        int result = LocalSectionIndex.fromByte(MemoryUtil.memGetByte(this.ptr + this.cur));

        this.cur += this.step;
        this.rem--;

        return result;
    }
}
