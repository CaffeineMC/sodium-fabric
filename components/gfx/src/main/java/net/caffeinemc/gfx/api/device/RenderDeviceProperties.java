package net.caffeinemc.gfx.api.device;

public final class RenderDeviceProperties {

    /**
     * A list of values that represent minimums, maximums, etc. for the device that should be respected in their
     * required contexts.
     */
    public final Values values;

    /**
     * A list of capabilities for functions that may not have full support on all platforms.
     */
    public final Capabilities capabilities;

    /**
     * A list of workarounds for driver bugs/issues that should be active on the current device.
     */
    public final DriverWorkarounds driverWorkarounds;

    /**
     * The name of the vendor that created the renderer/device, as reported by the driver.
     */
    public final String vendorName;

    /**
     * The name of the renderer/device, as reported by the driver.
     */
    public final String deviceName;

    /**
     * The name of the graphics API that is being used by this device.
     */
    public final String apiName;

    /**
     * The version of the graphics API that this device supports.
     */
    public final String apiVersion;

    public RenderDeviceProperties(
            String vendorName,
            String deviceName,
            String apiName,
            String apiVersion,
            Values values,
            Capabilities capabilities,
            DriverWorkarounds driverWorkarounds
    ) {
        this.vendorName = vendorName;
        this.deviceName = deviceName;
        this.apiName = apiName;
        this.apiVersion = apiVersion;
        this.values = values;
        this.capabilities = capabilities;
        this.driverWorkarounds = driverWorkarounds;
    }

    public static class Values {

        /**
         * The required alignment for offsets used by uniform buffer bindings. Always a power-of-two.
         */
        public final int uniformBufferOffsetAlignment;

        /**
         * The required alignment for offsets used by storage buffer bindings. Always a power-of-two.
         */
        public final int storageBufferOffsetAlignment;

        /**
         * The maximum amount of combined texture-image units that the device has available. This is effectively the cap
         * for how many textures can be bound at once.
         */
        public final int maxCombinedTextureImageUnits;

        public Values(
                int uniformBufferOffsetAlignment,
                int storageBufferOffsetAlignment,
                int maxCombinedTextureImageUnits
        ) {
            this.uniformBufferOffsetAlignment = uniformBufferOffsetAlignment;
            this.storageBufferOffsetAlignment = storageBufferOffsetAlignment;
            this.maxCombinedTextureImageUnits = maxCombinedTextureImageUnits;
        }
    }

    public static class Capabilities {

        /**
         * Allows a count buffer to be used with indirect rendering to limit the number of elements drawn.
         */
        public final boolean indirectCount;

        /**
         * Allows the variables gl_BaseVertex, gl_BaseInstance, and gl_DrawID to be used in shaders.
         */
        public final boolean shaderDrawParameters;

        public Capabilities(
                boolean indirectCount,
                boolean shaderDrawParameters
        ) {
            this.indirectCount = indirectCount;
            this.shaderDrawParameters = shaderDrawParameters;
        }
    }

    public static class DriverWorkarounds {

        /**
         * Determines whether multiDrawElementsIndirectCount should be used in place of multiDrawElementsIndirect.
         * Symptoms: Corrupted rendering when using glMultiDrawElementsIndirect(...)
         * Affects: All OpenGL Intel drivers on Windows. // FIXME: check if it happens on linux
         * Workaround: Always use a count buffer to prevent the driver from optimizing indirect draws into direct draws.
         */
        public final boolean forceIndirectCount;

        public DriverWorkarounds(
                boolean forceIndirectCount
        ) {
            this.forceIndirectCount = forceIndirectCount;
        }
    }
}
