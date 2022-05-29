package net.caffeinemc.gfx.api.device;

public final class RenderDeviceProperties {
    /**
     * The required alignment for offsets used by uniform buffer bindings. Always a power-of-two.
     */
    public final int uniformBufferOffsetAlignment;

    /**
     * The required alignment for offsets used by storage buffer bindings. Always a power-of-two.
     */
    public final int storageBufferOffsetAlignment;

    public RenderDeviceProperties(int uniformBufferOffsetAlignment, int storageBufferOffsetAlignment) {
        this.uniformBufferOffsetAlignment = uniformBufferOffsetAlignment;
        this.storageBufferOffsetAlignment = storageBufferOffsetAlignment;
    }
}
