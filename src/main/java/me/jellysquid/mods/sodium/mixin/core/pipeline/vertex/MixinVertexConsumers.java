package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;


import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

public class MixinVertexConsumers {
    @Mixin(targets = "net/minecraft/client/render/VertexConsumers$Dual")
    public static class MixinDual implements VertexBufferWriter {
        @Shadow
        @Final
        private VertexConsumer first;

        @Shadow
        @Final
        private VertexConsumer second;

        @Override
        public void push(long ptr, int count, VertexFormatDescription format) {
            VertexBufferWriter.of(this.first)
                    .push(ptr, count, format);

            VertexBufferWriter.of(this.second)
                    .push(ptr, count, format);
        }
    }

    @Mixin(targets = "net/minecraft/client/render/VertexConsumers$Union")
    public static class MixinUnion implements VertexBufferWriter {
        @Shadow
        @Final
        private VertexConsumer[] delegates;

        @Override
        public void push(long ptr, int count, VertexFormatDescription format) {
            for (var delegate : this.delegates) {
                VertexBufferWriter.of(delegate)
                        .push(ptr, count, format);
            }
        }
    }
}
