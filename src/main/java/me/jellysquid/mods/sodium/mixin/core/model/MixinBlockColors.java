package me.jellysquid.mods.sodium.mixin.core.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.model.color.interop.BlockColorsExtended;
import net.minecraft.block.Block;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.block.BlockColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockColors.class)
public class MixinBlockColors implements BlockColorsExtended {
    // We're keeping a copy as we need to be able to iterate over the entry pairs, rather than just the values.
    private Reference2ReferenceMap<Block, BlockColorProvider> blocksToColor;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.blocksToColor = new Reference2ReferenceOpenHashMap<>();
    }

    @Inject(method = "registerColorProvider", at = @At("HEAD"))
    private void preRegisterColorProvider(BlockColorProvider provider, Block[] blocks, CallbackInfo ci) {
        for (Block block : blocks) {
            this.blocksToColor.put(block, provider);
        }
    }

    @Override
    public Iterable<Reference2ReferenceMap.Entry<Block, BlockColorProvider>> getProviders() {
        return Reference2ReferenceMaps.unmodifiable(this.blocksToColor)
                .reference2ReferenceEntrySet();
    }
}
