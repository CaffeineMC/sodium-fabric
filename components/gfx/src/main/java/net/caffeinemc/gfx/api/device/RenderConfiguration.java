package net.caffeinemc.gfx.api.device;

public class RenderConfiguration {
    /**
     * If enabled, backends will check usages of the API to ensure usage is correct. This does not guard against all
     * forms of invalid usage (such as buffers with malformed draw commands). These checks might add some overhead,
     * so they can be disabled in production here.
     */
    public static boolean API_CHECKS = true;
}
