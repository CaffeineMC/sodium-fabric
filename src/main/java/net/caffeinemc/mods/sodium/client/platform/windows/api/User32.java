package net.caffeinemc.mods.sodium.client.platform.windows.api;

import net.caffeinemc.mods.sodium.client.platform.windows.api.msgbox.MsgBoxParamSw;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.SharedLibrary;

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;

public class User32 {
    private static final SharedLibrary LIBRARY = APIUtil.apiCreateLibrary("user32");

    private static final long PFN_MessageBoxIndirectW = apiGetFunctionAddress(LIBRARY, "MessageBoxIndirectW");

    /**
     * @see <a href="https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-messageboxw>Winuser.h Documentation</a>
     */
    public static void callMessageBoxIndirectW(MsgBoxParamSw params) {
        JNI.callPI(params.address(), PFN_MessageBoxIndirectW);
    }
}
