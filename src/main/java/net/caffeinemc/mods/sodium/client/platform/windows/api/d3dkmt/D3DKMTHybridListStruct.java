package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

// https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/ns-d3dkmthk-_d3dkmt_hybrid_list
//typedef struct _D3DKMT_HYBRID_LIST {
//    D3DKMT_GPU_PREFERENCE_QUERY_STATE State;    // Gpu preference query state
//    LUID AdapterLuid;                           // in,opt: Adapter luid to per-adapter DList state. Optional if QueryType == D3DKMT_GPU_PREFERENCE_TYPE_IHV_DLIST
//    BOOL bUserPreferenceQuery;                  // Whether referring to user gpu preference, or per-adapter DList query
//#if (DXGKDDI_INTERFACE_VERSION >= DXGKDDI_INTERFACE_VERSION_WDDM2_6)
//    D3DKMT_GPU_PREFERENCE_QUERY_TYPE QueryType; // Replaced bUserPreferenceQuery, for referring to which D3DKMT_GPU_PREFERENCE_QUERY_TYPE
//#endif
//} D3DKMT_HYBRID_LIST;
class D3DKMTHybridListStruct extends Struct<D3DKMTAdapterInfoStruct> {
    private static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_STATE;
    private static final int OFFSET_ADAPTER_LUID;
    private static final int OFFSET_BUSERPREFERENCEQUERY;
    private static final int OFFSET_QUERYTYPE;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(Long.BYTES, 4),
                __member(Integer.BYTES),
                __member(Integer.BYTES)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_STATE = layout.offsetof(0);
        OFFSET_ADAPTER_LUID = layout.offsetof(1);
        OFFSET_BUSERPREFERENCEQUERY = layout.offsetof(2);
        OFFSET_QUERYTYPE = layout.offsetof(3);
    }

    private D3DKMTHybridListStruct(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @NotNull D3DKMTAdapterInfoStruct create(long address, ByteBuffer container) {
        return new D3DKMTAdapterInfoStruct(address, container);
    }

    public static D3DKMTHybridListStruct malloc(MemoryStack stack) {
        return new D3DKMTHybridListStruct(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    public void setState(int adapter) {
        memPutInt(this.address + OFFSET_STATE, adapter);
    }

    public void setAdapterLuid(long luid) {
        memPutLong(this.address + OFFSET_ADAPTER_LUID, luid);
    }

    public void setbUserPreferenceQuery(boolean userPreferenceQuery) {
        memPutInt(this.address + OFFSET_BUSERPREFERENCEQUERY, userPreferenceQuery?1:0);
    }

    public void setQueryType(int type) {
        memPutInt(this.address + OFFSET_QUERYTYPE, type);
    }
}
