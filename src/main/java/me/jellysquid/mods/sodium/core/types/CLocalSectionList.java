package me.jellysquid.mods.sodium.core.types;

import me.jellysquid.mods.sodium.client.render.chunk.graph.LocalSectionIndex;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

public class CLocalSectionList extends Struct {
    public static final int SIZEOF;
    public static final int ALIGNOF;

    private static final int
            OFFSET_ARRAY_SIZE,
            OFFSET_ARRAY_BASE;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __array(LocalSectionIndex.SIZEOF, RenderRegion.REGION_SIZE)
        );

        OFFSET_ARRAY_SIZE = layout.offsetof(0);
        OFFSET_ARRAY_BASE = layout.offsetof(1);

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();
    }

    private CLocalSectionList(long address, ByteBuffer container) {
        super(address, container);
    }

    public static CLocalSectionList fromHeap(long ptr) {
        return wrap(CLocalSectionList.class, ptr);
    }

    public int size() {
        return MemoryUtil.memGetInt(this.address + OFFSET_ARRAY_SIZE);
    }

    public long arrayBase() {
        return this.address + OFFSET_ARRAY_BASE;
    }

    public int listElement(int index) {
        return LocalSectionIndex.fromByte(
                MemoryUtil.memGetByte(this.address + OFFSET_ARRAY_BASE + index));
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }
}
