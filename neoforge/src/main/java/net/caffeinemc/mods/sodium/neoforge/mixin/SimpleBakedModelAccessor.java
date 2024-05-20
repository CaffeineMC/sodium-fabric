package net.caffeinemc.mods.sodium.neoforge.mixin;

import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraftforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleBakedModel.class)
public interface SimpleBakedModelAccessor {
    @Accessor
    ChunkRenderTypeSet getBlockRenderTypes();
}
