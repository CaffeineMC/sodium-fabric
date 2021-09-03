package me.jellysquid.mods.sodium.mixin.core.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.model.quad.QuadColorizer;
import me.jellysquid.mods.sodium.interop.vanilla.colors.BlockColorsExtended;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.block.BlockColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockColors.class)
public class MixinBlockColors implements BlockColorsExtended {
    private Reference2ReferenceMap<Block, QuadColorizer<BlockState>> blocksToColor;

    private static final QuadColorizer<?> DEFAULT_PROVIDER = (state, view, pos, tint) -> -1;

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.blocksToColor = new Reference2ReferenceOpenHashMap<>();
        this.blocksToColor.defaultReturnValue((QuadColorizer<BlockState>) DEFAULT_PROVIDER);
    }

    @Inject(method = "registerColorProvider", at = @At("HEAD"))
    private void preRegisterColor(BlockColorProvider provider, Block[] blocks, CallbackInfo ci) {
        for (Block block : blocks) {
            this.blocksToColor.put(block, provider::getColor);
        }
    }

    @Override
    public QuadColorizer<BlockState> getColorProvider(BlockState state) {
        return this.blocksToColor.get(state.getBlock());
    }
}
