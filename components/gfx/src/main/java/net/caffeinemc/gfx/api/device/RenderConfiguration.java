package net.caffeinemc.gfx.api.device;

public class RenderConfiguration {
    /**
     * If enabled, backends will check usages of the API to ensure usage is correct. This does not guard against all
     * forms of invalid usage (such as buffers with malformed draw commands). These checks might add some overhead.
     *
     * This is left as a static final field so it can be optimized out by the JIT when not used.
     */
    public static final boolean API_CHECKS = true;
    
    /**
     * If enabled, will ask the API to perform extra debug checks and produce more readable information for errors
     * at runtime. This can add some overhead.
     */
    public final boolean apiDebug;
    
    public RenderConfiguration(boolean apiDebug) {
        this.apiDebug = apiDebug;
    }
}
