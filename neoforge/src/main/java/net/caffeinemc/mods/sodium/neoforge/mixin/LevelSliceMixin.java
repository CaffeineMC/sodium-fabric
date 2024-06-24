package net.caffeinemc.mods.sodium.neoforge.mixin;


import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.model.data.ModelDataManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * This is a self-mixin to implement Forge interfaces into LevelSlice.
 */
@Mixin(LevelSlice.class)
public abstract class LevelSliceMixin implements BlockAndTintGetter {
    @Shadow
    @Final
    private ClientLevel level;

    @Shadow
    public BlockEntity getBlockEntity(int blockX, int blockY, int blockZ) {
        throw new IllegalStateException();
    }

    @Override
    public @Nullable BlockEntity getExistingBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public @Nullable ModelDataManager getModelDataManager() {
        return level.getModelDataManager();
    }

    @Override
    public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        return this.level.getShade(normalX, normalY, normalZ, shade);
    }
}
