package me.jellysquid.mods.sodium.core.types;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

public class CRegionDrawBatch extends Struct {
    public static final int SIZEOF;
    public static final int ALIGNOF;

    private static final int OFFSET_REGION_X, OFFSET_REGION_Y, OFFSET_REGION_Z;
    private static final int OFFSET_SECTION_LIST;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),

                __member(CLocalSectionList.SIZEOF, CLocalSectionList.ALIGNOF)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_REGION_X = layout.offsetof(0);
        OFFSET_REGION_Y = layout.offsetof(1);
        OFFSET_REGION_Z = layout.offsetof(2);

        OFFSET_SECTION_LIST = layout.offsetof(3);
    }

    private CRegionDrawBatch(long address, ByteBuffer container) {
        super(address, container);
    }

    public static CRegionDrawBatch fromHeap(long ptr) {
        return wrap(CRegionDrawBatch.class, ptr);
    }

    public int regionX() {
        return MemoryUtil.memGetInt(this.address + OFFSET_REGION_X);
    }

    public int regionY() {
        return MemoryUtil.memGetInt(this.address + OFFSET_REGION_Y);
    }

    public int regionZ() {
        return MemoryUtil.memGetInt(this.address + OFFSET_REGION_Z);
    }

    public CLocalSectionList sectionList() {
        return CLocalSectionList.fromHeap(this.address + OFFSET_SECTION_LIST);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }
}
