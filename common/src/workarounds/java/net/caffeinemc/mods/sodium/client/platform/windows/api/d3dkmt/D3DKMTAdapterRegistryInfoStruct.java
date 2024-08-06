package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

// https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/ns-d3dkmthk-_d3dkmt_adapterregistryinfo
// typedef struct _D3DKMT_ADAPTERREGISTRYINFO {
//         WCHAR AdapterString[MAX_PATH];
//         WCHAR BiosString[MAX_PATH];
//         WCHAR DacType[MAX_PATH];
//         WCHAR ChipType[MAX_PATH];
// } D3DKMT_ADAPTERREGISTRYINFO;
class D3DKMTAdapterRegistryInfoStruct extends Struct<D3DKMTAdapterRegistryInfoStruct> {
    private static final int MAX_PATH = 260;
    private static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_ADAPTER_STRING;
    private static final int OFFSET_BIOS_STRING;
    private static final int OFFSET_DAC_TYPE;
    private static final int OFFSET_CHIP_TYPE;

    static {
        var layout = __struct(
                __member(Short.BYTES * MAX_PATH, Short.BYTES),
                __member(Short.BYTES * MAX_PATH, Short.BYTES),
                __member(Short.BYTES * MAX_PATH, Short.BYTES),
                __member(Short.BYTES * MAX_PATH, Short.BYTES)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_ADAPTER_STRING = layout.offsetof(0);
        OFFSET_BIOS_STRING = layout.offsetof(1);
        OFFSET_DAC_TYPE = layout.offsetof(2);
        OFFSET_CHIP_TYPE = layout.offsetof(3);
    }

    private D3DKMTAdapterRegistryInfoStruct(long address, ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected D3DKMTAdapterRegistryInfoStruct create(long address, ByteBuffer container) {
        return new D3DKMTAdapterRegistryInfoStruct(address, container);
    }

    public static D3DKMTAdapterRegistryInfoStruct calloc(MemoryStack stack) {
        return new D3DKMTAdapterRegistryInfoStruct(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    public @Nullable String getAdapterString() {
        return getString(this.address + OFFSET_ADAPTER_STRING);
    }

    private static @Nullable String getString(long ptr) {
        final var buf = memByteBuffer(ptr, Short.BYTES * MAX_PATH);
        final var len = memLengthNT2(buf) >> 1;

        if (len == 0) {
            return null;
        }

        return memUTF16(buf, len);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }
}
