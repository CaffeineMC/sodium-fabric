package net.caffeinemc.mods.sodium.client.platform.windows.api.version;

import net.caffeinemc.mods.sodium.client.platform.windows.api.Kernel32;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Bindings to the <a href="https://learn.microsoft.com/en-us/windows/win32/api/winver/">winver.h</a> API. This can be
 * used to query file version information about a PE module on disk.
 */
public class Version {
    private static final SharedLibrary LIBRARY = APIUtil.apiCreateLibrary("version");

    private static final long PFN_GetFileVersionInfoSizeW;
    private static final long PFN_GetFileVersionInfoW;

    private static final long PFN_VerQueryValueW;

    static {
        PFN_GetFileVersionInfoSizeW = APIUtil.apiGetFunctionAddress(LIBRARY, "GetFileVersionInfoSizeW");
        PFN_GetFileVersionInfoW = APIUtil.apiGetFunctionAddress(LIBRARY, "GetFileVersionInfoW");

        PFN_VerQueryValueW = APIUtil.apiGetFunctionAddress(LIBRARY, "VerQueryValueW");
    }

    /**
     * Queries a sub-block with the given name from a {@link VersionInfo} instance. See also:
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/winver/nf-winver-verqueryvaluew">VerQueryValueW</a>
     *
     * @param block The pointer to version info block
     * @param subBlock The name of the sub-block to query
     * @return The query result if successful, otherwise null
     */
    static @Nullable QueryResult query(ByteBuffer block, String subBlock) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer pSubBlock = stack.malloc(16, MemoryUtil.memLengthUTF16(subBlock, true));
            MemoryUtil.memUTF16(subBlock, true, pSubBlock);

            PointerBuffer pBuffer = stack.callocPointer(1);
            IntBuffer pLen = stack.callocInt(1);

            int result = JNI.callPPPPI(MemoryUtil.memAddress(block), MemoryUtil.memAddress(pSubBlock),
                    MemoryUtil.memAddress(pBuffer), MemoryUtil.memAddress(pLen), Version.PFN_VerQueryValueW);

            if (result == 0) {
                return null;
            }

            return new QueryResult(pBuffer.get(), pLen.get());
        }
    }

    /**
     * Creates a {@link VersionInfo} for a PE module at the specified path.
     *
     * <p>See also: <a href="https://learn.microsoft.com/en-us/windows/win32/api/winver/nf-winver-getfileversioninfow">
     *     GetFileVersionInfoW</a></p>
     *
     * <p>NOTE: The caller must free the resource returned by this function (if not null).</p>
     *
     * @param filename The path to the PE module on disk
     * @throws RuntimeException If a non-success code is returned by GetFileVersionInfoSizeW
     * @return An instance of {@link VersionInfo}, or null if the PE module is invalid
     */
    public static @Nullable VersionInfo getModuleFileVersion(String filename) {
        ByteBuffer lptstrFilename = MemoryUtil.memAlignedAlloc(16, MemoryUtil.memLengthUTF16(filename, true));

        try (MemoryStack stack = MemoryStack.stackPush()) {
            MemoryUtil.memUTF16(filename, true, lptstrFilename);

            IntBuffer lpdwHandle = stack.callocInt(1);
            int versionInfoLength = JNI.callPPI(MemoryUtil.memAddress(lptstrFilename), MemoryUtil.memAddress(lpdwHandle), PFN_GetFileVersionInfoSizeW);

            if (versionInfoLength == 0) {
                int error = Kernel32.getLastError();

                switch (error) {
                    case 0x714 /* ERROR_RESOURCE_DATA_NOT_FOUND */:
                    case 0x715 /* ERROR_RESOURCE_TYPE_NOT_FOUND */:
                        return null;
                    default:
                        throw new RuntimeException("GetFileVersionInfoSizeW failed, error=" + error);
                }
            }

            VersionInfo versionInfo = VersionInfo.allocate(versionInfoLength);
            int result = JNI.callPPI(MemoryUtil.memAddress(lptstrFilename), lpdwHandle.get(), versionInfoLength, versionInfo.address(), PFN_GetFileVersionInfoW);

            if (result == 0) {
                versionInfo.close();

                throw new RuntimeException("GetFileVersionInfoW failed, error=" + Kernel32.getLastError());
            }

            return versionInfo;
        } finally {
            MemoryUtil.memAlignedFree(lptstrFilename);
        }
    }
}
