package net.caffeinemc.sodium.render.entity.compile;

import net.caffeinemc.gfx.api.buffer.Buffer;

public record BuiltEntityModel(Buffer vertexBuffer, int vertexCount, float[] primitivePositions, int[] primitivePartIds) {
}
