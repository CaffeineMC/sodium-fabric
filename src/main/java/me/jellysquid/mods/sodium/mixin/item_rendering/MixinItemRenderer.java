package me.jellysquid.mods.sodium.mixin.item_rendering;

import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    private Random random;

    @Redirect(method = "renderBakedItemModel", at = @At(value = "NEW", target = "java/util/Random"))
    public Random reduceRandomAllocations() {
        // Random instance can be safely reused, since renderBakedItemModel always resets its seed to 42L anyways
        if (random == null)
            random = new Random();
        return random;
    }

    @Redirect(method = "renderBakedItemModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Direction;values()[Lnet/minecraft/util/math/Direction;"))
    public Direction[] reduceDirectionArrayAllocations() {
        return DirectionUtil.ALL_DIRECTIONS;
    }
}
