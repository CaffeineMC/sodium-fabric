package me.jellysquid.mods.sodium.mixin.features.buffer_builder.intrinsics;

import me.jellysquid.mods.sodium.client.render.RenderGlobal;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.common.util.MatrixHelper;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {
    private static final int SHADOW_COLOR = ColorABGR.pack(1.0f, 1.0f, 1.0f);

    /**
     * @author JellySquid
     * @reason Reduce vertex assembly overhead for shadow rendering
     */
    @Overwrite
    private static void renderShadowPart(MatrixStack.Entry entry, VertexConsumer vertices, Chunk chunk, WorldView world, BlockPos pos, double x, double y, double z, float radius, float opacity) {
        BlockPos blockPos = pos.down();
        BlockState blockState = world.getBlockState(blockPos);

        if (blockState.getRenderType() == BlockRenderType.INVISIBLE || !blockState.isFullCube(world, blockPos)) {
            return;
        }

        var light = world.getLightLevel(pos);

        if (light <= 3) {
            return;
        }

        VoxelShape voxelShape = blockState.getOutlineShape(world, blockPos);

        if (voxelShape.isEmpty()) {
            return;
        }

        float brightness = LightmapTextureManager.getBrightness(world.getDimension(), light);
        float alpha = (float) (((double) opacity - ((y - (double) pos.getY()) / 2.0)) * 0.5 * (double) brightness);

        if (alpha >= 0.0F) {
            if (alpha > 1.0F) {
                alpha = 1.0F;
            }

            Box box = voxelShape.getBoundingBox();

            float minX = (float) ((pos.getX() + box.minX) - x);
            float maxX = (float) ((pos.getX() + box.maxX) - x);

            float minY = (float) ((pos.getY() + box.minY) - y);

            float minZ = (float) ((pos.getZ() + box.minZ) - z);
            float maxZ = (float) ((pos.getZ() + box.maxZ) - z);

            renderShadowPart(entry, vertices, radius, alpha, minX, maxX, minY, minZ, maxZ);
        }
    }

    private static void renderShadowPart(MatrixStack.Entry matrices, VertexConsumer vertices, float radius, float alpha, float minX, float maxX, float minY, float minZ, float maxZ) {
        float size = 0.5F * (1.0F / radius);

        float u1 = (-minX * size) + 0.5F;
        float u2 = (-maxX * size) + 0.5F;

        float v1 = (-minZ * size) + 0.5F;
        float v2 = (-maxZ * size) + 0.5F;

        var matNormal = matrices.getNormalMatrix();
        var matPosition = matrices.getPositionMatrix();

        var color = ColorABGR.withAlpha(SHADOW_COLOR, alpha);
        var normal = MatrixHelper.transformNormal(matNormal, 0.0f, 1.0f, 0.0f);

        try (MemoryStack stack = RenderGlobal.VERTEX_DATA.push()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            writeShadowVertex(ptr, matPosition, minX, minY, minZ, u1, v1, color, normal);
            ptr += ModelVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, minX, minY, maxZ, u1, v2, color, normal);
            ptr += ModelVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, maxZ, u2, v2, color, normal);
            ptr += ModelVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, minZ, u2, v1, color, normal);
            ptr += ModelVertex.STRIDE;

            VertexBufferWriter.of(vertices)
                    .push(stack, buffer, 4, ModelVertex.FORMAT);
        }
    }

    private static void writeShadowVertex(long ptr, Matrix4f matPosition, float x, float y, float z, float u, float v, int color, int normal) {
        // The transformed position vector
        float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
        float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
        float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

        ModelVertex.write(ptr, xt, yt, zt, color, u, v, OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, normal);
    }
}
