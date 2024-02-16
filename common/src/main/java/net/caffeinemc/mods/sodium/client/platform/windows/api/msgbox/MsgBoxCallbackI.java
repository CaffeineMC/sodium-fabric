package net.caffeinemc.mods.sodium.client.platform.windows.api.msgbox;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.libffi.FFICIF;

import static org.lwjgl.system.APIUtil.apiCreateCIF;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.libffi.LibFFI.*;

@FunctionalInterface
@NativeType("MSGBOXCALLBACK")
public interface MsgBoxCallbackI extends CallbackI {
    FFICIF CIF = apiCreateCIF(
            FFI_DEFAULT_ABI,
            ffi_type_void,
            ffi_type_pointer
    );

    @Override
    default @NotNull FFICIF getCallInterface() {
        return CIF;
    }

    @Override
    default void callback(long ret, long args) {
        this.invoke(
                memGetAddress(memGetAddress(args)) /* lpHelpInfo */
        );
    }

    void invoke(@NativeType("LPHELPINFO *") long lpHelpInfo);
}
