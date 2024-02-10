package net.caffeinemc.mods.sodium.client.platform.windows.api;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;
import java.nio.ByteBuffer;

/**
 * Bindings to <a href="">kernel32.dll</a> on Windows.
 *
 * <p>NOTE: Accessing this class (or even referencing it) will cause a crash on non-Windows platforms, since the
 * dynamic library is not available.</p>
 */
public class Kernel32 {
    private static final SharedLibrary LIBRARY = APIUtil.apiCreateLibrary("kernel32");

    /**
     * The maximum length (in bytes) for a filesystem path.
     */
    private static final int MAX_PATH = 32767;

    // API CONSTANTS
    private static final int GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT = 1 << 0;
    private static final int GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS = 1 << 2;

    // API FUNCTION POINTERS
    private static final long PFN_GetCommandLineW;
    private static final long PFN_SetEnvironmentVariableW;

    private static final long PFN_GetModuleHandleExW;
    private static final long PFN_GetLastError;

    private static final long PFN_GetModuleFileNameW;


    static {
        PFN_GetCommandLineW = APIUtil.apiGetFunctionAddress(LIBRARY, "GetCommandLineW");
        PFN_SetEnvironmentVariableW = APIUtil.apiGetFunctionAddress(LIBRARY, "SetEnvironmentVariableW");
        PFN_GetModuleHandleExW = APIUtil.apiGetFunctionAddress(LIBRARY, "GetModuleHandleExW");
        PFN_GetLastError = APIUtil.apiGetFunctionAddress(LIBRARY, "GetLastError");
        PFN_GetModuleFileNameW = APIUtil.apiGetFunctionAddress(LIBRARY, "GetModuleFileNameW");
    }

    /**
     * Sets an environment variable for the current process. If the value is <pre>null</pre>, the environment variable
     * will be deleted.
     *
     * <p>See also: <a href="https://learn.microsoft.com/en-us/windows/win32/api/processenv/nf-processenv-setenvironmentvariablew">
     *     SetEnvironmentVariableW</a></p>
     *
     * @param name The name of the environment variable
     * @param value The value of the environment variable
     */
    public static void setEnvironmentVariable(String name, @Nullable String value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer lpNameBuf = stack.malloc(16, MemoryUtil.memLengthUTF16(name, true));
            MemoryUtil.memUTF16(name, true, lpNameBuf);

            ByteBuffer lpValueBuf = null;

            if (value != null) {
                lpValueBuf = stack.malloc(16, MemoryUtil.memLengthUTF16(value, true));
                MemoryUtil.memUTF16(value, true, lpValueBuf);
            }

            JNI.callPPI(MemoryUtil.memAddress0(lpNameBuf), MemoryUtil.memAddressSafe(lpValueBuf), PFN_SetEnvironmentVariableW);
        }
    }

    /**
     * Returns the command line of the current process.
     **
     * <p>See also: <a href="https://learn.microsoft.com/en-us/windows/win32/api/processenv/nf-processenv-getcommandlinew">
     *     GetCommandLineW</a></p>

     * @return A pointer to a UTF-16 null-terminated string
     */
    public static long getCommandLine() {
        return JNI.callP(PFN_GetCommandLineW);
    }

    /**
     * Searches for a module matching any of the provided names, and returns a handle to the first module which matches.
     *
     * <p>SAFETY: The memory referenced by the pointer MUST NOT be modified by the caller.</p>
     *
     * <p>See also: <a href="https://learn.microsoft.com/en-us/windows/win32/api/libloaderapi/nf-libloaderapi-getmodulehandleexw">
     *     GetModuleHandleExW</a></p>
     *
     * @param names The array of module names to search for
     * @throws RuntimeException If a non-success code is returned by <pre>GetModuleHandleEx</pre>
     * @return The pointer to the handle, or null if no module is present with a matching name
     */
    public static long getModuleHandleByName(String[] names) {
        for (String name : names) {
            var handle = getModuleHandleByName(name);

            if (handle != MemoryUtil.NULL) {
                return handle;
            }
        }

        return MemoryUtil.NULL;
    }

    /**
     * Searches for a module with the given name, and returns the handle.
     *
     * <p>SAFETY: The memory referenced by the pointer MUST NOT be modified by the caller.</p>
     *
     * <p>See also: <a href="https://learn.microsoft.com/en-us/windows/win32/api/libloaderapi/nf-libloaderapi-getmodulehandleexw">
     *     GetModuleHandleExW</a></p>
     *
     * @param name The name of the module to obtain a handle for
     * @throws RuntimeException If a non-success code is returned by <pre>GetModuleHandleEx</pre>
     * @return The pointer to the handle, or null if no module is present with a matching name
     */
    public static long getModuleHandleByName(String name) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer lpFunctionNameBuf = stack.malloc(16, MemoryUtil.memLengthUTF16(name, true));
            MemoryUtil.memUTF16(name, true, lpFunctionNameBuf);

            PointerBuffer phModule = stack.callocPointer(1);

            int result;
            result = JNI.callPPI(GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                    MemoryUtil.memAddress(lpFunctionNameBuf), MemoryUtil.memAddress(phModule), PFN_GetModuleHandleExW);

            if (result == 0) {
                var error = getLastError();

                switch (error) {
                    case 126 /* ERROR_MOD_NOT_FOUND */:
                        return MemoryUtil.NULL;
                    default:
                        throw new RuntimeException("GetModuleHandleEx failed, error=" + error);
                }
            }

            return phModule.get(0);
        }
    }

    /**
     * Returns the path on disk for a module.
     *
     * <p>SAFETY: {@param phModule} must be a valid pointer to an <pre>HMODULE</pre> as returned by
     * {@link Kernel32#getModuleHandleByName(String)}.</p>
     *
     * <p>See also: <a href="https://learn.microsoft.com/en-us/windows/win32/api/libloaderapi/nf-libloaderapi-getmodulefilenamew">
     *     GetModuleFileNameW</a></p>
     *
     * @param phModule A pointer to an <pre>HMODULE</pre> instance
     * @throws RuntimeException If a non-success code is returned by <pre>GetModuleFileNameW</pre>
     * @return The file path of the module
     */
    public static String getModuleFileName(long phModule) {
        // We don't know how long the response will be, so we just allocate for the largest possible response.
        ByteBuffer lpFileName = MemoryUtil.memAlignedAlloc(16, MAX_PATH);

        try {
            int length = JNI.callPPI(phModule, MemoryUtil.memAddress(lpFileName), lpFileName.capacity(), PFN_GetModuleFileNameW);

            if (length == 0) {
                throw new RuntimeException("GetModuleFileNameW failed, error=" + getLastError());
            }

            return MemoryUtil.memUTF16(lpFileName, length);
        } finally {
            MemoryUtil.memAlignedFree(lpFileName);
        }
    }

    /**
     * @return The last error returned by a kernel32 function
     */
    public static int getLastError() {
        return JNI.callI(PFN_GetLastError);
    }
}
