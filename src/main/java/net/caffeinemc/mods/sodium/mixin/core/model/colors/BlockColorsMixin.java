package net.caffeinemc.mods.sodium.mixin.core.model.colors;

import it.unimi.dsi.fastutil.objects.*;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.model.color.interop.BlockColorsExtension;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockColors.class)
public class BlockColorsMixin implements BlockColorsExtension {
    // We're keeping a copy as we need to be able to iterate over the entry pairs, rather than just the values.
    @Unique
    private final Reference2ReferenceMap<Block, BlockColor> blocksToColor = new Reference2ReferenceOpenHashMap<>();

    @Unique
    private final ReferenceSet<Block> overridenBlocks = new ReferenceOpenHashSet<>();

    @Inject(method = "register", at = @At("HEAD"))
    private void preRegisterColorProvider(BlockColor provider, Block[] blocks, CallbackInfo ci) {
        for (Block block : blocks) {
            // There will be one provider already registered for vanilla blocks, if we are replacing it,
            // it means a mod is using custom logic and we need to disable per-vertex coloring
            if (this.blocksToColor.put(block, provider) != null) {
                this.overridenBlocks.add(block);
                SodiumClientMod.logger().info("Block {} had its color provider replaced with {} and will not use per-vertex coloring", BuiltInRegistries.BLOCK.getKey(block), provider.toString());
            }
        }
    }

    @Override
    public Reference2ReferenceMap<Block, BlockColor> sodium$getProviders() {
        return Reference2ReferenceMaps.unmodifiable(this.blocksToColor);
    }

    @Override
    public ReferenceSet<Block> sodium$getOverridenVanillaBlocks() {
        return ReferenceSets.unmodifiable(this.overridenBlocks);
    }
}
