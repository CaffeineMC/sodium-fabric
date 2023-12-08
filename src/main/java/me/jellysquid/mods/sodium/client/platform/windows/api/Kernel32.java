package me.jellysquid.mods.sodium.client.platform.windows.api;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.*;

import java.nio.ByteBuffer;

public class Kernel32 {
    private static final SharedLibrary LIBRARY = Library.loadNative("me.jellyquid.mods.sodium", "kernel32");

    private static final long PFN_GetCommandLineW;
    private static final long PFN_SetEnvironmentVariableW;

    static {
        PFN_GetCommandLineW = APIUtil.apiGetFunctionAddress(LIBRARY, "GetCommandLineW");
        PFN_SetEnvironmentVariableW = APIUtil.apiGetFunctionAddress(LIBRARY, "SetEnvironmentVariableW");
    }

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

    public static long getCommandLine() {
        return JNI.callP(PFN_GetCommandLineW);
    }
}
