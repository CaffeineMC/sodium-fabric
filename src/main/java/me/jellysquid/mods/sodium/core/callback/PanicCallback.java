package me.jellysquid.mods.sodium.core.callback;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class PanicCallback extends Callback {
    private PanicCallback(long address) {
        super(address);
    }

    public static PanicCallback defaultHandler() {
        var callback = new PanicCallbackI() {
            @Override
            public void invoke(long address, int length) {
                String panicMessage;

                if (address != MemoryUtil.NULL) {
                    panicMessage = MemoryUtil.memUTF8(address, length);
                } else {
                    panicMessage = "(no available information.)";
                }

                Thread thread = Thread.currentThread();
                StackTraceElement[] stackTrace = thread.getStackTrace();

                StringBuilder log = new StringBuilder();
                log.append("""
                        # FATAL: A panic has occurred within the native library used by Sodium. This is not supposed
                        # to happen, and it likely indicates a bug in Sodium.
                        #
                        # If you are submitting a bug report, you must include the following information, along with
                        # a complete description about what you were doing at the time of the crash.
                        #
                        """);

                log.append("# Details:\n");

                log.append("# \tDescription: ")
                        .append(panicMessage)
                        .append('\n');

                log.append("# \tFaulting thread: ")
                        .append("'")
                        .append(thread.getName())
                        .append("' (id: ").append(thread.getId()).append(")")
                        .append('\n');

                log.append("# \tStack trace (J=Java code, N=Native code):\n");

                if (stackTrace.length > 0) {
                    for (var elem : stackTrace) {
                        log.append("#\t\t");

                        if (elem.isNativeMethod()) {
                            log.append("N");
                        } else {
                            log.append("J");
                        }

                        log.append(" at ").append(elem).append('\n');
                    }
                } else {
                    log.append("#\t\t (no stack trace information is available...)\n");
                }

                log.append("# \n");
                log.append("# This is not a recoverable error. The Java process will now be forcefully aborted.");

                printAndSaveLog(log.toString());

                Runtime.getRuntime()
                        .halt(134 /* SIGABRT */);
            }
        };

        return new PanicCallback(callback.address());
    }

    private static void printAndSaveLog(String message) {
        System.err.println(message);

        var process = ProcessHandle.current();
        var pid = process.pid();

        var path = FabricLoader.getInstance().getGameDir()
                .resolve("logs/sodium_err_pid" + pid + ".log");

        try {
            Files.writeString(path, message);
        } catch (IOException e) {
            // ignore
        }
    }
}
