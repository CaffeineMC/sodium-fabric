package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

// https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/ns-d3dkmthk-_d3dkmt_device_ids
//typedef struct _D3DKMT_DEVICE_IDS {
//    UINT VendorID;
//    UINT DeviceID;
//    UINT SubVendorID;
//    UINT SubSystemID;
//    UINT RevisionID;
//    UINT BusType;
//} D3DKMT_DEVICE_IDS;
public class D3DKMTDeviceIdsStruct extends Struct<D3DKMTDeviceIdsStruct> {
    public static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_VENDORID;
    private static final int OFFSET_DEVICEID;
    private static final int OFFSET_SUBVENDORID;
    private static final int OFFSET_SUBSYSTEMID;
    private static final int OFFSET_REVISIONID;
    private static final int OFFSET_BUSTYPE;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_VENDORID = layout.offsetof(0);
        OFFSET_DEVICEID = layout.offsetof(1);
        OFFSET_SUBVENDORID = layout.offsetof(2);
        OFFSET_SUBSYSTEMID = layout.offsetof(3);
        OFFSET_REVISIONID = layout.offsetof(4);
        OFFSET_BUSTYPE = layout.offsetof(5);
    }

    public D3DKMTDeviceIdsStruct(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @NotNull D3DKMTDeviceIdsStruct create(long address, ByteBuffer container) {
        return new D3DKMTDeviceIdsStruct(address, container);
    }

    public static D3DKMTDeviceIdsStruct calloc() {
        return new D3DKMTDeviceIdsStruct(MemoryUtil.nmemCalloc(1, SIZEOF), null);
    }

    public static D3DKMTDeviceIdsStruct calloc(MemoryStack stack) {
        return new D3DKMTDeviceIdsStruct(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }


    public int getVendorId() {
        return memGetInt(this.address + OFFSET_VENDORID);
    }

    public int getDeviceId() {
        return memGetInt(this.address + OFFSET_DEVICEID);
    }

    public int getSubVendorId() {
        return memGetInt(this.address + OFFSET_SUBVENDORID);
    }

    public int getSubSystemId() {
        return memGetInt(this.address + OFFSET_SUBSYSTEMID);
    }
}
