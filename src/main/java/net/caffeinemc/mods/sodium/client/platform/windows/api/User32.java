package net.caffeinemc.mods.sodium.client.platform.windows.api;

import net.caffeinemc.mods.sodium.client.platform.windows.api.msgbox.MsgBoxParamSw;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.SharedLibrary;

/**
 * Bindings to <a href="">user32.dll</a> on Windows.
 *
 * <p>NOTE: Accessing this class (or even referencing it) will cause a crash on non-Windows platforms, since the
 * dynamic library is not available.</p>
 */
public class User32 {
    private static final SharedLibrary LIBRARY = APIUtil.apiCreateLibrary("user32");

    // API FUNCTION POINTERS
    private static final long PFN_MessageBoxIndirectW;

    static {
        PFN_MessageBoxIndirectW = APIUtil.apiGetFunctionAddress(LIBRARY, "MessageBoxIndirectW");
    }

    /**
     * <p>See also: <a href="https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-messageboxw">MessageBoxIndirectW</a></p>
     */
    public static void callMessageBoxIndirectW(MsgBoxParamSw params) {
        JNI.callPI(params.address(), PFN_MessageBoxIndirectW);
    }
}
