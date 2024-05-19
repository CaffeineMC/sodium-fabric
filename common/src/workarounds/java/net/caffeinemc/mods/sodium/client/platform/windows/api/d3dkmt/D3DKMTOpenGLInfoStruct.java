package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;

// https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/ns-d3dkmthk-_d3dkmt_openglinfo
// typedef struct _D3DKMT_OPENGLINFO {
//           WCHAR UmdOpenGlIcdFileName[MAX_PATH];
//     [out] ULONG Version;
//     [in]  ULONG Flags;
// } D3DKMT_OPENGLINFO;
public class D3DKMTOpenGLInfoStruct extends Struct<D3DKMTOpenGLInfoStruct> {
    private static final int MAX_PATH = 260;
    private static final int SIZEOF, ALIGNOF;

    private static final int OFFSET_UMD_OPENGL_ICD_FILE_NAME;
    private static final int OFFSET_VERSION;
    private static final int OFFSET_FLAGS;

    static {
        var layout = __struct(
                __member(Short.BYTES * MAX_PATH, Short.BYTES),
                __member(Integer.BYTES),
                __member(Integer.BYTES)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        OFFSET_UMD_OPENGL_ICD_FILE_NAME = layout.offsetof(0);
        OFFSET_VERSION = layout.offsetof(1);
        OFFSET_FLAGS = layout.offsetof(2);
    }

    private D3DKMTOpenGLInfoStruct(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected @NotNull D3DKMTOpenGLInfoStruct create(long address, ByteBuffer container) {
        return new D3DKMTOpenGLInfoStruct(address, container);
    }

    public static D3DKMTOpenGLInfoStruct calloc() {
        return new D3DKMTOpenGLInfoStruct(MemoryUtil.nmemCalloc(1, SIZEOF), null);
    }

    public static D3DKMTOpenGLInfoStruct calloc(MemoryStack stack) {
        return new D3DKMTOpenGLInfoStruct(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    public ByteBuffer getUserModeDriverFileNameBuffer() {
        return MemoryUtil.memByteBuffer(this.address + OFFSET_UMD_OPENGL_ICD_FILE_NAME,
                Short.BYTES * MAX_PATH);
    }

    public @Nullable String getUserModeDriverFileName() {
        var name = this.getUserModeDriverFileNameBuffer();
        var length = memLengthNT2(name);

        if (length == 0) {
            return null;
        }

        return memUTF16(memAddress(name), length >> 1);
    }

    public int getVersion() {
        return MemoryUtil.memGetInt(this.address + OFFSET_VERSION);
    }

    public int getFlags() {
        return MemoryUtil.memGetInt(this.address + OFFSET_FLAGS);
    }

    @Override
    public int sizeof() {
        return SIZEOF;
    }
}
