package net.caffeinemc.sodium.render.entity.compile;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.device.ResourceDestructors;

// FIXME: VertexFormat<?> format, EntityRenderPass pass
public record BuiltEntityModel(Buffer vertexBuffer, int vertexCount, float[] primitivePositions, int[] primitivePartIds) {

    public void delete(ResourceDestructors resourceDestructors) {
        resourceDestructors.deleteBuffer(this.vertexBuffer);
    }
}
