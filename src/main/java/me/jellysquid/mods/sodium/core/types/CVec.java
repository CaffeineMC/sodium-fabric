package me.jellysquid.mods.sodium.core.types;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

public class CVec extends Struct {
    public static final int SIZEOF;
    public static final int ALIGNOF;

    private static final int
            OFFSET_SIZE,
            OFFSET_DATA;

    static {
        Layout layout = __struct(
                __member(Integer.BYTES),
                __member(Pointer.POINTER_SIZE)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_SIZE = layout.offsetof(0);
        OFFSET_DATA = layout.offsetof(1);
    }

    // unused
    private CVec(long address, ByteBuffer container) {
        super(address, container);
    }

    public static CVec stackAlloc(MemoryStack stack) {
        return wrap(CVec.class, stack.nmalloc(ALIGNOF, SIZEOF));
    }

    public int len() {
        return MemoryUtil.memGetInt(this.address + OFFSET_SIZE);
    }

    public long data() {
        return MemoryUtil.memGetAddress(this.address + OFFSET_DATA);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }
}
