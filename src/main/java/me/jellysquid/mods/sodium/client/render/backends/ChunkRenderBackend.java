package me.jellysquid.mods.sodium.client.render.backends;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Iterator;

public interface ChunkRenderBackend<T extends ChunkRenderState> {
    void upload(Iterator<ChunkBuildResult<T>> queue);

    void render(Iterator<T> renders, MatrixStack matrixStack, double x, double y, double z);

    void delete();

    Class<T> getRenderStateType();

    GlVertexFormat<ChunkMeshAttribute> getVertexFormat();

    default BlockPos getRenderOffset(ChunkSectionPos pos) {
        return new BlockPos(pos.getX() << 4, pos.getY() << 4, pos.getZ() << 4);
    }
}
