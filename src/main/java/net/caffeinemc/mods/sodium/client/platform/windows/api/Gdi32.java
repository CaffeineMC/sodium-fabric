package net.caffeinemc.mods.sodium.client.platform.windows.api;

import org.apache.commons.lang3.Validate;
import org.lwjgl.system.JNI;
import org.lwjgl.system.SharedLibrary;

import static org.lwjgl.system.APIUtil.apiCreateLibrary;
import static org.lwjgl.system.APIUtil.apiGetFunctionAddressOptional;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Gdi32 {
    private static final SharedLibrary LIBRARY = apiCreateLibrary("gdi32");

    public static final long PFN_D3DKMTQueryAdapterInfo = apiGetFunctionAddressOptional(LIBRARY, "D3DKMTQueryAdapterInfo");
    public static final long PFN_D3DKMTCloseAdapter = apiGetFunctionAddressOptional(LIBRARY, "D3DKMTCloseAdapter");
    public static final long PFN_D3DKMTEnumAdapters = apiGetFunctionAddressOptional(LIBRARY, "D3DKMTEnumAdapters");

    public static boolean isD3DKMTSupported() {
        return PFN_D3DKMTQueryAdapterInfo != NULL && PFN_D3DKMTCloseAdapter != NULL && PFN_D3DKMTEnumAdapters != NULL;
    }

    // https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/nf-d3dkmthk-d3dkmtqueryadapterinfo
    public static int /* NTSTATUS */ nd3dKmtQueryAdapterInfo(long ptr /* D3DKMT_QUERYADAPTERINFO */) {
        Validate.isTrue(PFN_D3DKMTQueryAdapterInfo != NULL);
        return JNI.callPI(ptr, PFN_D3DKMTQueryAdapterInfo);
    }

    // https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/nf-d3dkmthk-d3dkmtenumadapters2
    public static int /* NTSTATUS */ nD3DKMTEnumAdapters(long ptr /* D3DKMT_ENUMADAPTERS */) {
        Validate.isTrue(PFN_D3DKMTEnumAdapters != NULL);
        return JNI.callPI(ptr, PFN_D3DKMTEnumAdapters);
    }

    // https://learn.microsoft.com/en-us/windows-hardware/drivers/ddi/d3dkmthk/nf-d3dkmthk-d3dkmtcloseadapter
    public static int /* NTSTATUS */ nD3DKMTCloseAdapter(long ptr /* D3DKMT_CLOSEADAPTER */) {
        Validate.isTrue(PFN_D3DKMTCloseAdapter != NULL);
        return JNI.callPI(ptr, PFN_D3DKMTCloseAdapter);
    }
}
