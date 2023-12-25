package me.jellysquid.mods.sodium.mixin.features.render.frapi;

import me.jellysquid.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import me.jellysquid.mods.sodium.client.render.frapi.render.ItemRenderContext;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Supplier;

@Mixin(BakedModel.class)
public interface BakedModelMixin extends FabricBakedModel {
    @Override
    default void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        ((AbstractBlockRenderContext) context).bufferDefaultModel((BakedModel) this, state);
    }

    // Override the default implementation to redirect to the fast ItemRenderer#renderBakedItemModel method when no transforms are applied.
    @Override
    default void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        ((ItemRenderContext) context).bufferDefaultModel((BakedModel) this, null);
    }
}
