package net.caffeinemc.mods.sodium.neoforge.model;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.services.SodiumModelDataContainer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;
import java.util.Set;

public class NeoForgeModelAccess implements PlatformModelAccess {
    @Override
    public Iterable<RenderType> getModelRenderTypes(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, RandomSource random, SodiumModelData modelData) {
        return model.getRenderTypes(state, random, (ModelData) (Object) modelData);
    }

    @Override
    public List<BakedQuad> getQuads(BlockAndTintGetter level, BlockPos pos, BakedModel model, BlockState state, Direction face, RandomSource random, RenderType renderType, SodiumModelData modelData) {
        return model.getQuads(state, face, random, (ModelData) (Object) modelData, renderType);
    }

    @Override
    public SodiumModelDataContainer getModelDataContainer(Level level, SectionPos sectionPos) {
        Set<Long2ObjectMap.Entry<ModelData>> entrySet = level.getModelDataManager().getAt(sectionPos).long2ObjectEntrySet();
        Long2ObjectMap<SodiumModelData> modelDataMap = new Long2ObjectOpenHashMap<>(entrySet.size());

        for (Long2ObjectMap.Entry<ModelData> entry : entrySet) {
            modelDataMap.put(entry.getLongKey(), (SodiumModelData) (Object) entry.getValue());
        }

        return new SodiumModelDataContainer(modelDataMap);
    }

    @Override
    public SodiumModelData getModelData(LevelSlice slice, BakedModel model, BlockState state, BlockPos pos, SodiumModelData originalData) {
        return (SodiumModelData) (Object) model.getModelData(slice, pos, state, (ModelData) (Object) originalData);
    }

    @Override
    public SodiumModelData getEmptyModelData() {
        return (SodiumModelData) (Object) ModelData.EMPTY;
    }
}
