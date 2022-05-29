package net.caffeinemc.gfx.api.shader;

/**
 * Represents a storage block in a shader which can be sourced from a buffer object.
 */
public interface BufferBlock {
    /**
     * @return The type of storage block this points to in the shader.
     */
    BufferBlockType type();

    /**
     * @return The index of the storage block in the shader.
     */
    int index();
}
