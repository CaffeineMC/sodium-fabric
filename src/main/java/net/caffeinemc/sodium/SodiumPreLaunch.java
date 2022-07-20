package net.caffeinemc.sodium;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Platform;
import org.lwjgl.system.jemalloc.JEmalloc;

public class SodiumPreLaunch implements PreLaunchEntrypoint {
    private static final Logger LOGGER = LogManager.getLogger("Sodium");
    private static volatile boolean runThread = true;
    private static volatile double fmaBlackhole = 0.0;
    
    @Override
    public void onPreLaunch() {
        checkJomlFmaSupport();
        tryLoadRenderdoc();
        checkJemalloc();
    }
    
    private static void tryLoadRenderdoc() {
        if (System.getProperty("sodium.load_renderdoc") != null) {
            LOGGER.info("Loading renderdoc...");
            try {
                System.loadLibrary("renderdoc");
            } catch (Throwable t) {
                LOGGER.warn("Unable to load renderdoc (is it in your path?)", t);
            }
        }
    }
    
    public static void checkJomlFmaSupport() {
        // generate the random number out here, so it won't be in the stacktrace of the thread
        double randNum = Math.random();
        
        // keep as anonymous class to shrink stack trace
        //noinspection AnonymousHasLambdaAlternative
        Thread t = new Thread() {
            @Override
            public void run() {
                while (runThread) {
                    fmaBlackhole = Math.fma(randNum, randNum, randNum);
                }
            }
        };
        
        t.setDaemon(true);
        t.start();
        
        long endTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(100);
        
        // This won't ever be initialized to true, and we need to use the blackhole variable so the JVM won't optimize it out
        boolean foundSlowPath = Double.isNaN(fmaBlackhole);
        do {
            // if the stack trace length is greater than 1, then that means we either went into nanoTime,
            // which should be native and therefore not be visible by this stacktrace, or we went into fma,
            // which will be inlined when fma intrinsics are enabled.
            if (t.getStackTrace().length > 1) {
                foundSlowPath = true;
            }
        } while (!foundSlowPath && System.nanoTime() < endTime);
        
        runThread = false;
        
        if (!foundSlowPath) {
            System.setProperty("joml.useMathFma", "true");
        }
    }

    private static void checkJemalloc() {
        // LWJGL 3.2.3 ships Jemalloc 5.2.0 which seems to be broken on Windows and suffers from critical memory leak problems
        // Using the system allocator prevents memory leaks and other problems
        // See changelog here: https://github.com/jemalloc/jemalloc/releases/tag/5.2.1
        if (Platform.get() == Platform.WINDOWS && isVersionWithinRange(JEmalloc.JEMALLOC_VERSION, "5.0.0", "5.2.0")) {
            LOGGER.info("Switching memory allocators to work around memory leaks present with Jemalloc 5.0.0 through 5.2.0 on Windows...");

            if (!Objects.equals(Configuration.MEMORY_ALLOCATOR.get(), "system")) {
                Configuration.MEMORY_ALLOCATOR.set("system");
            }
        }
    }

    private static boolean isVersionWithinRange(String curStr, String minStr, String maxStr) {
        Version cur, min, max;

        try {
            cur = Version.parse(curStr);
            min = Version.parse(minStr);
            max = Version.parse(maxStr);
        } catch (VersionParsingException e) {
            LOGGER.warn("Unable to parse version string", e);
            return false;
        }

        return cur.compareTo(min) >= 0 && cur.compareTo(max) <= 0;
    }
}
