package me.jellysquid.mods.sodium.mixin.features.entity.smooth_lighting;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.model.light.EntityLighter;
import me.jellysquid.mods.sodium.client.render.entity.EntityLightSampler;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PaintingEntityRenderer.class)
public abstract class MixinPaintingEntityRenderer extends EntityRenderer<PaintingEntity> implements EntityLightSampler<PaintingEntity> {
    private PaintingEntity entity;
    private float tickDelta;

    protected MixinPaintingEntityRenderer(EntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void preRender(PaintingEntity paintingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        this.entity = paintingEntity;
        this.tickDelta = g;
    }

    /**
     * @author FlashyReese
     * @reason Redirect Lightmap coord with Sodium's EntityLighter.
     */
    @Redirect(method = "method_4074", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;getLightmapCoordinates(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;)I"))
    public int redirectLightmapCoord(BlockRenderView world, BlockPos pos) {
        if (SodiumClientMod.options().quality.smoothLighting == SodiumGameOptions.LightingQuality.HIGH) {
            return EntityLighter.getBlendedLight(this, this.entity, tickDelta);
        } else {
            return WorldRenderer.getLightmapCoordinates(world, pos);
        }
    }

    @Override
    public int bridge$getBlockLight(PaintingEntity entity, BlockPos pos) {
        return this.getBlockLight(entity, pos);
    }

    @Override
    public int bridge$getSkyLight(PaintingEntity entity, BlockPos pos) {
        return this.method_27950(entity, pos);
    }
}