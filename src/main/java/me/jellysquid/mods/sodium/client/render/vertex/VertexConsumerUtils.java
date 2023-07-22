package me.jellysquid.mods.sodium.client.render.vertex;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.render.VertexConsumer;

public class VertexConsumerUtils {
    /**
     * Attempt to convert a {@link VertexConsumer} into a {@link VertexBufferWriter}. If this fails, return null
     * and log a message.
     * @param consumer the consumer to convert
     * @return a {@link VertexBufferWriter}, or null if the consumer does not support this
     */
    public static VertexBufferWriter convertOrLog(VertexConsumer consumer) {
        VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);
        if(writer == null) {
            VertexConsumerTracker.logBadConsumer(consumer);
        }
        return writer;
    }
}
