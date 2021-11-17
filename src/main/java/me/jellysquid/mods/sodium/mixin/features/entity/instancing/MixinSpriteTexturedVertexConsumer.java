package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.interop.vanilla.consumer.SpriteTexturedVertexConsumerAccessor;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteTexturedVertexConsumer.class)
public class MixinSpriteTexturedVertexConsumer implements SpriteTexturedVertexConsumerAccessor {
    @Shadow
    @Final
    private VertexConsumer parent;

    @Override
    public VertexConsumer getParent() {
        return parent;
    }
}
