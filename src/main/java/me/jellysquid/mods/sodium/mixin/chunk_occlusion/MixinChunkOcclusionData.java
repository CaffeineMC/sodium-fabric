package me.jellysquid.mods.sodium.mixin.chunk_occlusion;

import net.minecraft.client.render.chunk.ChunkOcclusionData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.BitSet;

@Mixin(ChunkOcclusionData.class)
public class MixinChunkOcclusionData {
    @Shadow
    @Final
    private static int DIRECTION_COUNT;

    private static final BitSet DUMMY_BIT_SET = new BitSet(0);

    private boolean[] visibilityFlags;

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "java/util/BitSet"))
    private BitSet nullifyBitSet(int size) {
        return DUMMY_BIT_SET;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.visibilityFlags = new boolean[DIRECTION_COUNT * DIRECTION_COUNT];
    }

    @Redirect(method = "setVisibleThrough", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;set(IZ)V"))
    public void redirectBitSet(BitSet bitSet, int bitIndex, boolean value) {
        this.visibilityFlags[bitIndex] = value;
    }

    @Redirect(method = "isVisibleThrough", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;get(I)Z"))
    public boolean redirectBitGet(BitSet bitSet, int bitIndex) {
        return this.visibilityFlags[bitIndex];
    }

    @Redirect(method = "fill", at = @At(value = "INVOKE", target = "Ljava/util/BitSet;set(IIZ)V"))
    public void redirectBitSetFill(BitSet bitSet, int fromIndex, int toIndex, boolean value) {
        Arrays.fill(this.visibilityFlags, value);
    }
}
