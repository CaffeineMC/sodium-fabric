package me.jellysquid.mods.sodium.client.model.quad;

import java.nio.ByteBuffer;

/**
 * This interface is responsible for encoding model codes into a given vertex format which can then be passed to
 * the graphics card for rendering. This allows multiple alternative vertex format encodings to be used when the
 * hardware supports it without calling code being aware of the detail.
 */
public interface ModelQuadEncoder {
    /**
     * @param quad The quad data to write
     * @param buffer The buffer to write the encoded vertex data into
     * @param position The starting byte position from which the vertex data should be written into the buffer
     */
    void write(ModelQuadView quad, ByteBuffer buffer, int position);
}
