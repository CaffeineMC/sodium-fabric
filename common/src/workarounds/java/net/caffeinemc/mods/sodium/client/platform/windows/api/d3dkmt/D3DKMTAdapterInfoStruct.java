package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memGetInt;

// https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/ns-d3dkmthk-_d3dkmt_adapterinfo
// typedef struct _D3DKMT_ADAPTERINFO {
//    D3DKMT_HANDLE       hAdapter;
//    LUID                AdapterLuid;
//    ULONG               NumOfSources;
//    BOOL                bPrecisePresentRegionsPreferred;
// } D3DKMT_ADAPTERINFO;
class D3DKMTAdapterInfoStruct extends Struct<D3DKMTAdapterInfoStruct> {
    public static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_HADAPTER;
    private static final int OFFSET_ADAPTER_LUID;
    private static final int OFFSET_NUM_OF_SOURCES;
    private static final int OFFSET_PRECISE_PRESENT_REGIONS_PREFERRED;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(Long.BYTES, Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_HADAPTER = layout.offsetof(0);
        OFFSET_ADAPTER_LUID = layout.offsetof(1);
        OFFSET_NUM_OF_SOURCES = layout.offsetof(2);
        OFFSET_PRECISE_PRESENT_REGIONS_PREFERRED = layout.offsetof(3);
    }

    D3DKMTAdapterInfoStruct(long address, ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected D3DKMTAdapterInfoStruct create(long address, ByteBuffer container) {
        return new D3DKMTAdapterInfoStruct(address, container);
    }

    public static D3DKMTAdapterInfoStruct create(long address) {
        return new D3DKMTAdapterInfoStruct(address, null);
    }

    public static Buffer calloc(int count) {
        return new Buffer(MemoryUtil.nmemCalloc(count, SIZEOF), count);
    }

    public int getAdapterHandle() {
        return memGetInt(this.address + OFFSET_HADAPTER);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }

    static class Buffer extends StructBuffer<D3DKMTAdapterInfoStruct, Buffer> {
        private static final D3DKMTAdapterInfoStruct ELEMENT_FACTORY = D3DKMTAdapterInfoStruct.create(-1L);

        protected Buffer(long address, int capacity) {
            super(address, null, -1, 0, capacity, capacity);
        }

        @Override
        protected @NotNull D3DKMTAdapterInfoStruct getElementFactory() {
            return ELEMENT_FACTORY;
        }

        @Override
        protected @NotNull Buffer self() {
            return this;
        }
    }
}
