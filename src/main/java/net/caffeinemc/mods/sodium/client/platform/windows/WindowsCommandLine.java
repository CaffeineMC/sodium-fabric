package net.caffeinemc.mods.sodium.client.platform.windows;

import net.caffeinemc.mods.sodium.client.platform.windows.api.Kernel32;
import org.lwjgl.system.MemoryUtil;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Terrible hacks for modifying the Windows PEB structure. This can be used to evade process detection.
 */
public class WindowsCommandLine {
    private static CommandLineHook ACTIVE_COMMAND_LINE_HOOK;

    /**
     * Modifies the Win32 PEB structure so that it holds a different command line for the process. This can be used to
     * change how other applications and modules identify the process.
     *
     * <p>WARNING: This function is not perfect, and it can fail if the modified command line is longer than the
     * original, or if the existing command line is identical to the modified one. To avoid issues, the string should
     * be short and unique.</p>
     *
     * <p>NOTE: There can only be one command line "installed" at a time. To change the command line after this function
     * has been called, you must first call {@link WindowsCommandLine#resetCommandLine()}.</p>
     *
     * @param modifiedCmdline The string to replace the process command line with
     * @throws BufferOverflowException If there is not enough memory in the PEB structure to store the modified string
     * @throws IllegalArgumentException If the modified command line is the same as the current command line
     * @throws RuntimeException If reading back the command line does not show the modified contents
     */
    public static void setCommandLine(String modifiedCmdline) {
        if (ACTIVE_COMMAND_LINE_HOOK != null) {
            throw new IllegalStateException("Command line is already modified");
        }

        // Pointer into the command-line arguments stored within the Windows process structure
        // We do not own this memory, and it should not be freed.
        var pCmdline = Kernel32.getCommandLine();

        // The original command-line the process was started with.
        var cmdline = MemoryUtil.memUTF16(pCmdline);
        var cmdlineLen = MemoryUtil.memLengthUTF16(cmdline, true);

        if (Objects.equals(cmdline, modifiedCmdline)) {
            throw new IllegalArgumentException("The modified command line string must be different from the original");
        }

        if (MemoryUtil.memLengthUTF16(modifiedCmdline, true) > cmdlineLen) {
            // We can never write a string which is larger than what we were given, as there
            // may not be enough space remaining. Realistically, this should never happen, since
            // our identifying string is very short, and the command line is *at least* going to contain
            // the class entrypoint.
            throw new BufferOverflowException();
        }

        ByteBuffer buffer = MemoryUtil.memByteBuffer(pCmdline, cmdlineLen);

        // Write the new command line arguments into the process structure.
        // The Windows API documentation explicitly says this is forbidden, but it *does* give us a pointer
        // directly into the PEB structure, so...
        MemoryUtil.memUTF16(modifiedCmdline, true, buffer);

        // Make sure we can actually see our changes in the process structure
        // We don't know if this could ever actually happen, but since we're doing something pretty hacky
        // it's not out of the question that Windows might try to prevent it in a newer version.
        if (!Objects.equals(modifiedCmdline, MemoryUtil.memUTF16(pCmdline))) {
            throw new RuntimeException("Sanity check failed, the command line arguments did not appear to change");
        }

        // Store the original command line and the address into memory which we modified
        // This will be needed later to restore the original contents
        ACTIVE_COMMAND_LINE_HOOK = new CommandLineHook(cmdline, buffer);
    }

    /**
     * Resets the process command line back to what it was before {@link WindowsCommandLine#setCommandLine(String)} was
     * called. If the command line is not currently modified, then calling this function does nothing.
     */
    public static void resetCommandLine() {
        if (ACTIVE_COMMAND_LINE_HOOK != null) {
            ACTIVE_COMMAND_LINE_HOOK.uninstall();
            ACTIVE_COMMAND_LINE_HOOK = null;
        }
    }

    private static class CommandLineHook {
        private final String cmdline;
        private final ByteBuffer cmdlineBuf;

        private CommandLineHook(String cmdline, ByteBuffer cmdlineBuf) {
            this.cmdline = cmdline;
            this.cmdlineBuf = cmdlineBuf;
        }

        public void uninstall() {
            // Restore the original value of the command line arguments
            // Must be null-terminated (as it was given to us)
            MemoryUtil.memUTF16(this.cmdline, true, this.cmdlineBuf);
        }
    }
}
