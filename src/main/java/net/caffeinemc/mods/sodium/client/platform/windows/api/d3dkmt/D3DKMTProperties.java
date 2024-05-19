package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memPutInt;
import static org.lwjgl.system.MemoryUtil.memPutLong;

//Fully undocumented

//typedef struct _D3DKMT_PROPERTIES
//{
//    uint32_t type;
//    uint32_t size;
//    uint32_t aa;
//    uint32_t bb;
//    D3DKMT_PTR(VOID*, dataPtr);
//} D3DKMT_PROPERTIES;

class D3DKMTProperties extends Struct<D3DKMTAdapterInfoStruct> {
    private static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_TYPE;
    private static final int OFFSET_SIZE;
    private static final int OFFSET_PTR;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Pointer.POINTER_SIZE)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_TYPE = layout.offsetof(0);
        OFFSET_SIZE = layout.offsetof(1);
        OFFSET_PTR = layout.offsetof(4);
    }

    private D3DKMTProperties(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @NotNull D3DKMTAdapterInfoStruct create(long address, ByteBuffer container) {
        return new D3DKMTAdapterInfoStruct(address, container);
    }

    public static D3DKMTProperties malloc(MemoryStack stack) {
        return new D3DKMTProperties(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    public static D3DKMTProperties calloc(MemoryStack stack) {
        return new D3DKMTProperties(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    public void setType(int type) {
        memPutInt(this.address + OFFSET_TYPE, type);
    }

    public void setSize(int size) {
        memPutInt(this.address + OFFSET_SIZE, size);
    }

    public void setPointer(long ptr) {
        memPutLong(this.address + OFFSET_PTR, ptr);
    }
}
