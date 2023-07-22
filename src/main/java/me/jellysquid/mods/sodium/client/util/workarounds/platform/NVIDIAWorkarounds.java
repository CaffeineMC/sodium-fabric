package me.jellysquid.mods.sodium.client.util.workarounds.platform;

import net.minecraft.util.Util;
import org.lwjgl.system.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class NVIDIAWorkarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-CommandLineWorkaround");

    private static Hook ACTIVE_HOOK;

    public static class Hook {
        // Reference to the shared library so that it doesn't get unloaded.
        private SharedLibrary sharedLibrary;

        // Pointer into the command-line arguments stored within the Windows process structure
        // We do not own this memory, and it should not be freed.
        private final ByteBuffer commandLineBuffer;

        // function pointer to GetCommandLineW in Kernel32
        private final long pfnGetCommandLineW;

        // function pointer to SetEnvironmentVariableW in Kernel32
        private final long pfnSetEnvironmentVariable;

        // The original command-line the process was started with.
        private final String originalCommandLine;

        // True if the command line arguments in memory have been modified
        private boolean modifiedCommandLine;
        // True if the environment variables have been modified
        private boolean modifiedEnvironmentVariables;

        public Hook() {
            this.sharedLibrary = Library.loadNative("me.jellyquid.mods.sodium", "kernel32");

            try {
                this.pfnGetCommandLineW = APIUtil.apiGetFunctionAddress(this.sharedLibrary, "GetCommandLineW");
                this.pfnSetEnvironmentVariable = APIUtil.apiGetFunctionAddress(this.sharedLibrary, "SetEnvironmentVariableW");

                var lpwstrCommandLine = JNI.callP(this.pfnGetCommandLineW);

                this.originalCommandLine = MemoryUtil.memUTF16(lpwstrCommandLine);

                // This should remain valid for the lifetime of the process.
                this.commandLineBuffer = MemoryUtil.memByteBuffer(lpwstrCommandLine,
                        MemoryUtil.memLengthUTF16(this.originalCommandLine, true));
            } catch (Throwable t) {
                this.sharedLibrary.close();
                throw t; // re-throw the exception
            }
        }

        public void install() {
            // Check that the library is still loaded, otherwise the function pointers are invalid.
            if (this.sharedLibrary == null) {
                throw new IllegalStateException("Library is unloaded");
            }

            this.modifyCommandLineArguments();
            this.modifyEnvironmentVariables();
        }

        private void modifyCommandLineArguments() {
            if (this.modifiedCommandLine) {
                return;
            }

            LOGGER.info("... Modifying process command line arguments... (forces NVIDIA drivers to not apply broken optimizations)");

            // The NVIDIA drivers rely on parsing the command line arguments to detect Minecraft. If we destroy those,
            // then it shouldn't be able to detect us anymore.
            var modifiedCmdline = "net.caffeinemc.sodium"; // Honestly, even giving the drivers this much might be a mistake.
            var modifiedCmdlineLen = MemoryUtil.memLengthUTF16(modifiedCmdline, true);

            if (modifiedCmdlineLen > this.commandLineBuffer.remaining()) {
                // We can never write a string which is larger than what we were given, as there
                // may not be enough space remaining. Realistically, this should never happen, since
                // our identifying string is very short, and the command line is *at least* going to contain
                // the class entrypoint.
                throw new BufferOverflowException();
            }

            // Write the new command line arguments into the process structure.
            // The Windows API documentation explicitly says this is forbidden, but it *does* give us a pointer
            // directly into the PEB structure, so...
            // Must be null-terminated (as it was given to us).
            MemoryUtil.memUTF16(modifiedCmdline, true,
                    this.commandLineBuffer);

            // Make sure we can actually see our changes in the process structure
            // We don't know if this could ever actually happen, but since we're doing something pretty hacky
            // it's not out of the question that Windows might try to prevent it in a newer version.
            //
            // NOTE: When reading it back, we pass the memory address by itself instead of the ByteBuffer. This
            // results in it reading up to the null terminator, rather than the end of the buffer.
            if (!Objects.equals(modifiedCmdline, MemoryUtil.memUTF16(MemoryUtil.memAddress(this.commandLineBuffer)))) {
                throw new RuntimeException("Sanity check failed, the command line arguments did not appear to change");
            }

            this.modifiedCommandLine = true;
        }

        private void modifyEnvironmentVariables() {
            if (this.modifiedEnvironmentVariables) {
                return;
            }

            LOGGER.info("... Modifying process environment variables... (forces NVIDIA drivers to use dedicated GPU when available)");

            // Since we broke the driver's ability to find Minecraft, it won't use the dedicated GPU any longer.
            // We need to update the environment variables to ensure it picks the dedicated GPU.
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var name = "SHIM_MCCOMPAT"; // control var for multi-GPU systems
                var value = "0x800000001"; // use dedicated GPU

                var lpName = stack.malloc(16, MemoryUtil.memLengthUTF16(name, true));
                MemoryUtil.memUTF16(name, true, lpName);

                var lpValue = stack.malloc(16, MemoryUtil.memLengthUTF16(value, true));
                MemoryUtil.memUTF16(value, true, lpValue);

                // We don't care about the return value.
                JNI.callJJI(MemoryUtil.memAddress0(lpName), MemoryUtil.memAddress(lpValue), this.pfnSetEnvironmentVariable);
            }

            this.modifiedEnvironmentVariables = true;
        }

        public void uninstall() {
            if (this.modifiedCommandLine) {
                this.restoreCommandLine();
            }

            if (this.modifiedEnvironmentVariables) {
                this.restoreEnvironmentVariables();
            }

            if (this.sharedLibrary != null) {
                this.sharedLibrary.close();
                this.sharedLibrary = null;
            }
        }

        private void restoreCommandLine() {
            // Restore the original value of the command line arguments
            // Must be null-terminated (as it was given to us)
            MemoryUtil.memUTF16(this.originalCommandLine, true,
                    this.commandLineBuffer);

            this.modifiedCommandLine = false;
        }


        private void restoreEnvironmentVariables() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var name = "SHIM_MCCOMPAT"; // control var for multi-GPU systems

                var lpName = stack.malloc(16, MemoryUtil.memLengthUTF16(name, true));
                MemoryUtil.memUTF16(name, true, lpName);

                // Passing NULL as the value will unset the environment variable.
                // We don't care about the return value.
                JNI.callJJI(MemoryUtil.memAddress0(lpName), MemoryUtil.NULL, this.pfnSetEnvironmentVariable);
            }

            this.modifiedEnvironmentVariables = false;
        }
    }

    public static void install() {
        if (ACTIVE_HOOK != null) {
            return;
        }

        if (Util.getOperatingSystem() != Util.OperatingSystem.WINDOWS) {
            return;
        }

        LOGGER.info("Attempting to apply workarounds for the NVIDIA Graphics Driver...");
        LOGGER.info("If the game crashes immediately after this point, please make a bug report: https://github.com/CaffeineMC/sodium-fabric/issues");

        try {
            ACTIVE_HOOK = new Hook();
            ACTIVE_HOOK.install();

            LOGGER.info("Successfully applied workarounds for the NVIDIA Graphics Driver!");
        } catch (Throwable t) {
            LOGGER.error("Failure while applying workarounds", t);

            LOGGER.error("READ ME! The workarounds for the NVIDIA Graphics Driver did not apply correctly!");
            LOGGER.error("READ ME! You are very likely going to run into unexplained crashes and severe performance issues!");
            LOGGER.error("READ ME! Please see this issue for more information: https://github.com/CaffeineMC/sodium-fabric/issues/1816");
        }
    }

    public static void setLinuxDisableEnv() {
        try (SharedLibrary sharedLibrary = Library.loadNative("me.jellyquid.mods.sodium", "libc.so.6")) {
            long pfnSetenv = APIUtil.apiGetFunctionAddress(sharedLibrary, "setenv");
            try (var stack = MemoryStack.stackPush()) {
                JNI.callPPI(MemoryUtil.memAddress0(stack.UTF8("__GL_THREADED_OPTIMIZATIONS")), MemoryUtil.memAddress0(stack.UTF8("0")), 1, pfnSetenv);
            }
        } catch (Throwable t) {
            LOGGER.error("Failure while applying workarounds", t);
            LOGGER.error("READ ME! The workarounds for the NVIDIA Graphics Driver did not apply correctly!");
        }
    }

    public static void uninstall() {
        if (ACTIVE_HOOK == null) {
            return;
        }

        ACTIVE_HOOK.uninstall();
        ACTIVE_HOOK = null;
    }
}
