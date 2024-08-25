package net.caffeinemc.mods.sodium.mixin.features.render.entity.shadows;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Unique
    private static final int SHADOW_COLOR = ColorABGR.pack(1.0f, 1.0f, 1.0f);

    /**
     * @author JellySquid
     * @reason Reduce vertex assembly overhead for shadow rendering
     */
    @Inject(method = "renderBlockShadow", at = @At("HEAD"), cancellable = true)
    private static void renderShadowPartFast(PoseStack.Pose matrices, VertexConsumer vertices, ChunkAccess chunk, LevelReader level, BlockPos pos, double x, double y, double z, float radius, float opacity, CallbackInfo ci) {
        var writer = VertexConsumerUtils.convertOrLog(vertices);

        if (writer == null) {
            return;
        }

        ci.cancel();

        BlockPos blockPos = pos.below();
        BlockState blockState = level.getBlockState(blockPos);

        if (blockState.getRenderShape() == RenderShape.INVISIBLE || !blockState.isCollisionShapeFullBlock(level, blockPos)) {
            return;
        }

        var light = level.getMaxLocalRawBrightness(pos);

        if (light <= 3) {
            return;
        }

        VoxelShape voxelShape = blockState.getShape(level, blockPos);

        if (voxelShape.isEmpty()) {
            return;
        }

        float brightness = LightTexture.getBrightness(level.dimensionType(), light);
        float alpha = (float) (((double) opacity - ((y - (double) pos.getY()) / 2.0)) * 0.5 * (double) brightness);

        if (alpha >= 0.0F) {
            if (alpha > 1.0F) {
                alpha = 1.0F;
            }

            AABB box = voxelShape.bounds();

            float minX = (float) ((pos.getX() + box.minX) - x);
            float maxX = (float) ((pos.getX() + box.maxX) - x);

            float minY = (float) ((pos.getY() + box.minY) - y);

            float minZ = (float) ((pos.getZ() + box.minZ) - z);
            float maxZ = (float) ((pos.getZ() + box.maxZ) - z);

            renderShadowPart(matrices, writer, radius, alpha, minX, maxX, minY, minZ, maxZ);
        }
    }

    @Unique
    private static void renderShadowPart(PoseStack.Pose matrices, VertexBufferWriter writer, float radius, float alpha, float minX, float maxX, float minY, float minZ, float maxZ) {
        float size = 0.5F * (1.0F / radius);

        float u1 = (-minX * size) + 0.5F;
        float u2 = (-maxX * size) + 0.5F;

        float v1 = (-minZ * size) + 0.5F;
        float v2 = (-maxZ * size) + 0.5F;

        var matNormal = matrices.normal();
        var matPosition = matrices.pose();

        var color = ColorABGR.withAlpha(SHADOW_COLOR, alpha);
        var normal = MatrixHelper.transformNormal(matNormal, matrices.trustedNormals, Direction.UP);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * EntityVertex.STRIDE);
            long ptr = buffer;

            writeShadowVertex(ptr, matPosition, minX, minY, minZ, u1, v1, color, normal);
            ptr += EntityVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, minX, minY, maxZ, u1, v2, color, normal);
            ptr += EntityVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, maxZ, u2, v2, color, normal);
            ptr += EntityVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, minZ, u2, v1, color, normal);
            ptr += EntityVertex.STRIDE;

            writer.push(stack, buffer, 4, EntityVertex.FORMAT);
        }
    }

    @Unique
    private static void writeShadowVertex(long ptr, Matrix4f matPosition, float x, float y, float z, float u, float v, int color, int normal) {
        // The transformed position vector
        float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
        float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
        float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

        EntityVertex.write(ptr, xt, yt, zt, color, u, v, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, normal);
    }
}
