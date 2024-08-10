package net.caffeinemc.mods.sodium.client.platform.windows.api.version;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

// typedef struct tagVS_FIXEDFILEINFO {
//  DWORD dwSignature;
//  DWORD dwStrucVersion;
//  DWORD dwFileVersionMS;
//  DWORD dwFileVersionLS;
//  DWORD dwProductVersionMS;
//  DWORD dwProductVersionLS;
//  DWORD dwFileFlagsMask;
//  DWORD dwFileFlags;
//  DWORD dwFileOS;
//  DWORD dwFileType;
//  DWORD dwFileSubtype;
//  DWORD dwFileDateMS;
//  DWORD dwFileDateLS;
// } VS_FIXEDFILEINFO;

public class VersionFixedFileInfoStruct extends Struct<VersionFixedFileInfoStruct> {
    private static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_DW_SIGNATURE;
    private static final int OFFSET_DW_STRUCTURE_VERSION;
    private static final int OFFSET_DW_FILE_VERSION_MS;
    private static final int OFFSET_DW_FILE_VERSION_LS;
    private static final int OFFSET_DW_PRODUCT_VERSION_MS;
    private static final int OFFSET_DW_PRODUCT_VERSION_LS;
    private static final int OFFSET_DW_FILE_FLAGS_MASK;
    private static final int OFFSET_DW_FILE_FLAGS;
    private static final int OFFSET_DW_FILE_OS;
    private static final int OFFSET_DW_FILE_TYPE;
    private static final int OFFSET_DW_FILE_SUBTYPE;
    private static final int OFFSET_DW_FILE_DATE_MS;
    private static final int OFFSET_DW_FILE_DATE_LS;

    static {
        var layout = __struct(
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES)
        );

        OFFSET_DW_SIGNATURE = layout.offsetof(0);
        OFFSET_DW_STRUCTURE_VERSION = layout.offsetof(1);
        OFFSET_DW_FILE_VERSION_MS = layout.offsetof(2);
        OFFSET_DW_FILE_VERSION_LS = layout.offsetof(3);
        OFFSET_DW_PRODUCT_VERSION_MS = layout.offsetof(4);
        OFFSET_DW_PRODUCT_VERSION_LS = layout.offsetof(5);
        OFFSET_DW_FILE_FLAGS_MASK = layout.offsetof(6);
        OFFSET_DW_FILE_FLAGS = layout.offsetof(7);
        OFFSET_DW_FILE_OS = layout.offsetof(8);
        OFFSET_DW_FILE_TYPE = layout.offsetof(9);
        OFFSET_DW_FILE_SUBTYPE = layout.offsetof(10);
        OFFSET_DW_FILE_DATE_MS = layout.offsetof(11);
        OFFSET_DW_FILE_DATE_LS = layout.offsetof(12);

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();
    }
    private VersionFixedFileInfoStruct(long address, ByteBuffer container) {
        super(address, container);
    }

    public static VersionFixedFileInfoStruct from(long address) {
        return new VersionFixedFileInfoStruct(address, null);
    }

    @Override
    protected VersionFixedFileInfoStruct create(long address, ByteBuffer container) {
        return new VersionFixedFileInfoStruct(address, container);
    }

    public int getFileVersionMostSignificantBits() {
        return MemoryUtil.memGetInt(this.address + OFFSET_DW_FILE_VERSION_MS);
    }

    public int getFileVersionLeastSignificantBits() {
        return MemoryUtil.memGetInt(this.address + OFFSET_DW_FILE_VERSION_LS);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }
}
