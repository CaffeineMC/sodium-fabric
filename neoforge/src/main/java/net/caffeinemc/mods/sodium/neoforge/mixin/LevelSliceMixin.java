package net.caffeinemc.mods.sodium.neoforge.mixin;


import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.extensions.IBlockGetterExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * This is a self-mixin to implement Forge interfaces into LevelSlice.
 */
@Mixin(LevelSlice.class)
public abstract class LevelSliceMixin implements BlockAndTintGetter {
    @Shadow
    @Final
    private Object[] auxLightManager;

    @Shadow
    private @Nullable Object modelDataSnapshot;

    @Shadow
    private int originBlockX, originBlockY, originBlockZ;

    @Override
    public @Nullable ModelDataManager getModelDataManager() {
        return (ModelDataManager.Snapshot) modelDataSnapshot;
    }

    @Shadow
    public Object getPlatformModelData(BlockPos pos) {
        return null;
    }

    @Override
    public ModelData getModelData(BlockPos pos) {
        Object modelData = getPlatformModelData(pos);
        return modelData != null ? (ModelData) modelData : null;
    }

    @Shadow
    public static int getLocalSectionIndex(int sectionX, int sectionY, int sectionZ) {
        throw new IllegalStateException("Not shadowed!");
    }

    @Override
    public @Nullable AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        int relChunkX = pos.x - (this.originBlockX >> 4);
        int relChunkZ = pos.z - (this.originBlockZ >> 4);

        return (AuxiliaryLightManager) auxLightManager[getLocalSectionIndex(relChunkX, 0, relChunkZ)];
    }

    @Override
    public @Nullable AuxiliaryLightManager getAuxLightManager(BlockPos pos) {
        int relBlockX = pos.getX() - this.originBlockX;
        int relBlockY = pos.getY() - this.originBlockY;
        int relBlockZ = pos.getZ() - this.originBlockZ;

        return (AuxiliaryLightManager) auxLightManager[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)];
    }
}
