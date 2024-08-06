package net.caffeinemc.mods.sodium.client.platform.windows;

import net.caffeinemc.mods.sodium.client.platform.windows.api.Kernel32;
import org.lwjgl.system.MemoryUtil;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class WindowsCommandLine {
    private static CommandLineHook ACTIVE_COMMAND_LINE_HOOK;

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

        ACTIVE_COMMAND_LINE_HOOK = new CommandLineHook(cmdline, buffer);
    }

    public static void resetCommandLine() {
        if (ACTIVE_COMMAND_LINE_HOOK != null) {
            ACTIVE_COMMAND_LINE_HOOK.uninstall();
            ACTIVE_COMMAND_LINE_HOOK = null;
        }
    }

    private static class CommandLineHook {
        private final String cmdline;
        private final ByteBuffer cmdlineBuf;

        private boolean active = true;

        private CommandLineHook(String cmdline, ByteBuffer cmdlineBuf) {
            this.cmdline = cmdline;
            this.cmdlineBuf = cmdlineBuf;
        }

        public void uninstall() {
            if (!this.active) {
                throw new IllegalStateException("Hook was already uninstalled");
            }

            // Restore the original value of the command line arguments
            // Must be null-terminated (as it was given to us)
            MemoryUtil.memUTF16(this.cmdline, true, this.cmdlineBuf);

            this.active = false;
        }
    }
}
