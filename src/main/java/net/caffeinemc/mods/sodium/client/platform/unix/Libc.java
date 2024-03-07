package net.caffeinemc.mods.sodium.client.platform.unix;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.*;

public class Libc {
    private static final SharedLibrary LIBRARY = APIUtil.apiCreateLibrary("libc.so.6");

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
