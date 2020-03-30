package me.jellysquid.mods.sodium.mixin.entity_rendering;

import me.jellysquid.mods.sodium.client.render.model.ExtendedCuboid;
import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ModelPart.Cuboid.class)
public class MixinCuboid implements ExtendedCuboid {
    @Shadow @Final
    private ModelPart.Quad[] sides;

    @Override
    public ModelPart.Quad[] getQuads() {
        return this.sides;
    }
}
