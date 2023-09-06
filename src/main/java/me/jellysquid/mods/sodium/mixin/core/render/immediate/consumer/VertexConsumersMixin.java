package me.jellysquid.mods.sodium.mixin.core.render.immediate.consumer;


import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.render.VertexConsumer;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class VertexConsumersMixin {
    @Mixin(targets = "net/minecraft/client/render/VertexConsumers$Dual")
    public static class DualMixin implements VertexBufferWriter {
        @Shadow
        @Final
        private VertexConsumer first;

        @Shadow
        @Final
        private VertexConsumer second;

        private boolean isFullWriter;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void checkFullStatus(CallbackInfo ci) {
            this.isFullWriter = VertexBufferWriter.tryOf(this.first) != null && VertexBufferWriter.tryOf(this.second) != null;
        }

        @Override
        public boolean isFullWriter() {
            return this.isFullWriter;
        }

        @Override
        public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
            VertexBufferWriter.copyInto(VertexBufferWriter.of(this.first), stack, ptr, count, format);
            VertexBufferWriter.copyInto(VertexBufferWriter.of(this.second), stack, ptr, count, format);
        }
    }

    @Mixin(targets = "net/minecraft/client/render/VertexConsumers$Union")
    public static class UnionMixin implements VertexBufferWriter {
        @Shadow
        @Final
        private VertexConsumer[] delegates;

        private boolean isFullWriter;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void checkFullStatus(CallbackInfo ci) {
            boolean notWriter = false;
            for(var delegate : this.delegates) {
                if(VertexBufferWriter.tryOf(delegate) == null) {
                    notWriter = true;
                    break;
                }
            }
            this.isFullWriter = !notWriter;
        }

        @Override
        public boolean isFullWriter() {
            return this.isFullWriter;
        }

        @Override
        public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
            for (var delegate : this.delegates) {
                VertexBufferWriter.copyInto(VertexBufferWriter.of(delegate), stack, ptr, count, format);
            }
        }
    }
}
