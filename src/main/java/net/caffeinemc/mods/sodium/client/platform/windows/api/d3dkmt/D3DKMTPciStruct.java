package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

//Fully undocumented

//typedef struct _D3DKMT_PROPERTIES_PCI
//{
//    uint32_t vendor;
//    uint32_t device;
//    uint32_t subsys;
//    uint32_t hasval;
//} D3DKMT_PROPERTIES_PCI;

class D3DKMTPciStruct extends Struct<D3DKMTPciStruct> {
    private static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_VENDOR;
    private static final int OFFSET_DEVICE;
    private static final int OFFSET_SUBSYS;
    private static final int OFFSET_HASVAL;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_VENDOR = layout.offsetof(0);
        OFFSET_DEVICE = layout.offsetof(1);
        OFFSET_SUBSYS = layout.offsetof(2);
        OFFSET_HASVAL = layout.offsetof(3);
    }

    private D3DKMTPciStruct(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @NotNull D3DKMTPciStruct create(long address, ByteBuffer container) {
        return new D3DKMTPciStruct(address, container);
    }

    public static D3DKMTPciStruct malloc(MemoryStack stack) {
        return new D3DKMTPciStruct(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    public static D3DKMTPciStruct calloc(MemoryStack stack) {
        return new D3DKMTPciStruct(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }


    public void setVendor(int vendor) {
        memPutInt(this.address + OFFSET_VENDOR, vendor);
    }

    public void setDevice(int device) {
        memPutInt(this.address + OFFSET_DEVICE, device);
    }

    public void setSubSys(int subsys) {
        memPutInt(this.address + OFFSET_SUBSYS, subsys);
    }
}
