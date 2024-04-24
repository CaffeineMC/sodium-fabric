package net.caffeinemc.mods.sodium.neoforge.mixin;


import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.extensions.IBlockGetterExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * This is a self-mixin to implement Forge interfaces into LevelSlice.
 */
@Mixin(LevelSlice.class)
public class LevelSliceMixin implements IBlockGetterExtension {
    @Shadow
    public Object getPlatformModelData(BlockPos pos) {
        return null;
    }

    @Override
    public ModelData getModelData(BlockPos pos) {
        Object modelData = getPlatformModelData(pos);
        return modelData != null ? (ModelData) modelData : null;
    }
}
