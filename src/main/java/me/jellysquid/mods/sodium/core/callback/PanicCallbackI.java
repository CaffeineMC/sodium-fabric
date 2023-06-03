package me.jellysquid.mods.sodium.core.callback;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.NativeType;
import org.lwjgl.system.libffi.FFICIF;

import static org.lwjgl.system.APIUtil.apiCreateCIF;
import static org.lwjgl.system.MemoryUtil.memGetAddress;
import static org.lwjgl.system.MemoryUtil.memGetInt;
import static org.lwjgl.system.libffi.LibFFI.*;

@FunctionalInterface
public interface PanicCallbackI extends CallbackI {
    FFICIF CIF = apiCreateCIF(
            FFI_DEFAULT_ABI,
            ffi_type_void,
            ffi_type_sint32, ffi_type_pointer
    );

    @Override
    default @NotNull FFICIF getCallInterface() {
        return CIF;
    }

    @Override
    default void callback(long ret, long args) {
        this.invoke(
                memGetAddress(memGetAddress(args)),
                memGetInt(memGetAddress(args + POINTER_SIZE))
        );
    }

    /**
     * Will be called when the native library encounters a panic. It is required that the JVM aborts
     * before this function returns!
     *
     * @param address a pointer to a UTF-8 encoded string describing the panic
     * @param length  the length of the string (in bytes)
     */
    void invoke(@NativeType("char *") long address, @NativeType("int") int length);
}
