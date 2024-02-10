package net.caffeinemc.mods.sodium.client.platform.unix;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.*;

/**
 * Bindings to <a href="">libc.so.6</a> on Linux.
 *
 * <p>NOTE: Accessing this class (or even referencing it) will cause a crash on non-Linux platforms, since the
 * dynamic library is not available.</p>
 */
public class Libc {
    private static final SharedLibrary LIBRARY = APIUtil.apiCreateLibrary("libc.so.6");

    // API FUNCTION POINTERS
    private static final long PFN_setenv;

    static {
        PFN_setenv = APIUtil.apiGetFunctionAddress(LIBRARY, "setenv");
    }

    /**
     * Sets an environment variable for the current process. If the value is <pre>null</pre>, the environment variable
     * will be deleted.
     *
     * <p>See also: <a href="https://manpages.ubuntu.com/manpages/noble/man3/setenv.3.html">setenv(3)</a></p>
     *
     * @param name The name of the environment variable
     * @param value The value of the environment variable
     */
    public static void setEnvironmentVariable(String name, @Nullable String value) {
        try (var stack = MemoryStack.stackPush()) {
            var nameBuf = stack.UTF8(name);
            var valueBuf = value != null ? stack.UTF8(value) : null;

            JNI.callPPI(MemoryUtil.memAddress(nameBuf), MemoryUtil.memAddressSafe(valueBuf), 1 /* replace */, PFN_setenv);
        }
    }
}
