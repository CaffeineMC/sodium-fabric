package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

// https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/ns-d3dkmthk-_d3dkmt_enumadapters
// typedef struct _D3DKMT_ENUMADAPTERS {
//  [in] ULONG              NumAdapters;
//       D3DKMT_ADAPTERINFO Adapters[MAX_ENUM_ADAPTERS];
//} D3DKMT_ENUMADAPTERS;
class D3DKMTEnumAdaptersStruct extends Struct<D3DKMTEnumAdaptersStruct> {
    private static final int SIZEOF, ALIGNOF;
    private static final int MAX_ENUM_ADAPTERS = 16;

    private static final int OFFSET_NUM_ADAPTERS;
    private static final int OFFSET_ADAPTERS;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(D3DKMTAdapterInfoStruct.SIZEOF * MAX_ENUM_ADAPTERS, D3DKMTAdapterInfoStruct.ALIGNOF)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_NUM_ADAPTERS = layout.offsetof(0);
        OFFSET_ADAPTERS = layout.offsetof(1);
    }

    private D3DKMTEnumAdaptersStruct(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @NotNull D3DKMTEnumAdaptersStruct create(long address, ByteBuffer container) {
        return new D3DKMTEnumAdaptersStruct(address, container);
    }

    public static D3DKMTEnumAdaptersStruct calloc(MemoryStack stack) {
        return new D3DKMTEnumAdaptersStruct(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    public D3DKMTAdapterInfoStruct.Buffer getAdapters() {
        return new D3DKMTAdapterInfoStruct.Buffer(this.address + OFFSET_ADAPTERS,
                MemoryUtil.memGetInt(this.address + OFFSET_NUM_ADAPTERS));
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }
}
