package me.jellysquid.mods.sodium.interop.vanilla.model;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.collection.Weighting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class DynamicWeightedBakedModel extends DynamicBakedModel {
    private final int totalWeight;
    private final List<Weighted.Present<BakedModel>> models;

    public DynamicWeightedBakedModel(List<Weighted.Present<BakedModel>> models) {
        super(models.get(0).getData());

        this.models = models;
        this.totalWeight = Weighting.getWeightSum(models);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        Weighted.Present<BakedModel> result = this.getWeightedModel(random);

        if (result != null) {
            return result.getData()
                    .getQuads(state, face, random);
        }

        return Collections.emptyList();
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        Weighted.Present<BakedModel> result = this.getWeightedModel(randomSupplier.get());

        if (result != null) {
            FabricBakedModel model = (FabricBakedModel) result.getData();
            model.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        Weighted.Present<BakedModel> result = this.getWeightedModel(randomSupplier.get());

        if (result != null) {
            FabricBakedModel model = (FabricBakedModel) result.getData();
            model.emitItemQuads(stack, randomSupplier, context);
        }
    }

    private Weighted.Present<BakedModel> getWeightedModel(Random random) {
        return getValueForWeight(this.models, Math.abs((int) random.nextLong()) % this.totalWeight);
    }

    private static <T extends Weighted> T getValueForWeight(List<T> pool, int totalWeight) {
        int i = 0;
        int len = pool.size();

        T weighted;

        do {
            if (i >= len) {
                return null;
            }

            weighted = pool.get(i++);
            totalWeight -= weighted.getWeight().getValue();
        } while (totalWeight >= 0);

        return weighted;
    }
}
