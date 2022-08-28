package net.caffeinemc.gfx.api.device;

public class RenderConfiguration {
    /**
     * If enabled, extra checks are done across GFX and its users to ensure that calls, state, and more are valid.
     * Users of GFX can also depend on this flag for debug checks.
     * <p>
     * This is left as a static final field, so it can be optimized out by the JIT when not used.
     */
    public static final boolean DEBUG_CHECKS = false;
    
    /**
     * If enabled, will ask the API to perform extra debug checks and produce more readable information for errors
     * at runtime. This can add some overhead.
     */
    public final boolean apiDebug;
    
    public RenderConfiguration(boolean apiDebug) {
        this.apiDebug = apiDebug;
    }
}
