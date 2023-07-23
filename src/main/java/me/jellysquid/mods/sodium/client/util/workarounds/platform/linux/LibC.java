package me.jellysquid.mods.sodium.client.util.workarounds.platform.linux;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.*;

public class LibC {
    private static final SharedLibrary LIBRARY = Library.loadNative("me.jellyquid.mods.sodium", "libc.so.6");

    private static final long PFN_setenv;

    static {
        PFN_setenv = APIUtil.apiGetFunctionAddress(LIBRARY, "setenv");
    }

    public static void setEnvironmentVariable(String name, @Nullable String value) {
        try (var stack = MemoryStack.stackPush()) {
            var nameBuf = stack.UTF8(name);
            var valueBuf = value != null ? stack.UTF8(value) : null;

            JNI.callPPI(MemoryUtil.memAddress(nameBuf), MemoryUtil.memAddressSafe(valueBuf), 1 /* replace */, PFN_setenv);
        }
    }
}
