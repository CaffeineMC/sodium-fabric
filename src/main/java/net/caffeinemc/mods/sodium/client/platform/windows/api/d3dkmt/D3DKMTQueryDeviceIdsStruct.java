package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memPutInt;
import static org.lwjgl.system.MemoryUtil.memPutLong;

//https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/ns-d3dkmthk-_d3dkmt_query_device_ids
//typedef struct _D3DKMT_QUERY_DEVICE_IDS {
//    UINT              PhysicalAdapterIndex;
//    D3DKMT_DEVICE_IDS DeviceIds;
//} D3DKMT_QUERY_DEVICE_IDS;
public class D3DKMTQueryDeviceIdsStruct extends Struct<D3DKMTQueryDeviceIdsStruct> {
    private static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_PHYSICALADAPTERINDEX;
    private static final int OFFSET_DEVICEIDS;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(D3DKMTDeviceIdsStruct.SIZEOF, D3DKMTDeviceIdsStruct.ALIGNOF)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_PHYSICALADAPTERINDEX = layout.offsetof(0);
        OFFSET_DEVICEIDS = layout.offsetof(1);
    }

    private D3DKMTQueryDeviceIdsStruct(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @NotNull D3DKMTQueryDeviceIdsStruct create(long address, ByteBuffer container) {
        return new D3DKMTQueryDeviceIdsStruct(address, container);
    }

    public static D3DKMTQueryDeviceIdsStruct malloc(MemoryStack stack) {
        return new D3DKMTQueryDeviceIdsStruct(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    public static D3DKMTQueryDeviceIdsStruct calloc(MemoryStack stack) {
        return new D3DKMTQueryDeviceIdsStruct(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    public void setPhysicalAdapterIndex(int index) {
        memPutInt(this.address + OFFSET_PHYSICALADAPTERINDEX, index);
    }


    public D3DKMTDeviceIdsStruct getDeviceIds() {
        return new D3DKMTDeviceIdsStruct(this.address + OFFSET_DEVICEIDS, null);
    }

}
