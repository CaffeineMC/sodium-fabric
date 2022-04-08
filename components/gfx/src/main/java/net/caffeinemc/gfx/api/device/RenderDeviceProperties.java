package net.caffeinemc.gfx.api.device;

public final class RenderDeviceProperties {
    /**
     * The required alignment for offsets used by uniform buffer bindings. Always a power-of-two.
     */
    public final int uniformBufferOffsetAlignment;

    public RenderDeviceProperties(int uniformBufferOffsetAlignment) {
        this.uniformBufferOffsetAlignment = uniformBufferOffsetAlignment;
    }
}
