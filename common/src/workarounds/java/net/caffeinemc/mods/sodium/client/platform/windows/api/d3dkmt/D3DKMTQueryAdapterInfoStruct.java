package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memPutAddress;
import static org.lwjgl.system.MemoryUtil.memPutInt;

// https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/ns-d3dkmthk-_d3dkmt_queryadapterinfo
// typedef struct _D3DKMT_QUERYADAPTERINFO {
//     D3DKMT_HANDLE           hAdapter;
//     KMTQUERYADAPTERINFOTYPE Type;
//     VOID                    *pPrivateDriverData;
//     UINT                    PrivateDriverDataSize;
// } D3DKMT_QUERYADAPTERINFO;
class D3DKMTQueryAdapterInfoStruct extends Struct<D3DKMTAdapterInfoStruct> {
    private static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_ADAPTER_HANDLE;
    private static final int OFFSET_TYPE;
    private static final int OFFSET_DATA_PTR;
    private static final int OFFSET_DATA_SIZE;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Pointer.POINTER_SIZE),
                __member(Integer.BYTES)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_ADAPTER_HANDLE = layout.offsetof(0);
        OFFSET_TYPE = layout.offsetof(1);
        OFFSET_DATA_PTR = layout.offsetof(2);
        OFFSET_DATA_SIZE = layout.offsetof(3);
    }

    private D3DKMTQueryAdapterInfoStruct(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @NotNull D3DKMTAdapterInfoStruct create(long address, ByteBuffer container) {
        return new D3DKMTAdapterInfoStruct(address, container);
    }

    public static D3DKMTQueryAdapterInfoStruct malloc(MemoryStack stack) {
        return new D3DKMTQueryAdapterInfoStruct(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    public void setAdapterHandle(int adapter) {
        memPutInt(this.address + OFFSET_ADAPTER_HANDLE, adapter);
    }

    public void setType(int type) {
        memPutInt(this.address + OFFSET_TYPE, type);
    }

    public void setDataPointer(long address) {
        memPutAddress(this.address + OFFSET_DATA_PTR, address);
    }

    public void setDataLength(int length) {
        memPutInt(this.address + OFFSET_DATA_SIZE, length);
    }
}
