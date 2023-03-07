package me.jellysquid.mods.sodium.mixin.features.entity.fast_render;

import me.jellysquid.mods.sodium.client.model.ModelCuboidAccessor;
import me.jellysquid.mods.sodium.client.render.immediate.model.ModelCuboid;
import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.Direction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ModelPart.Cuboid.class)
public class MixinCuboid implements ModelCuboidAccessor {
    private ModelCuboid sodium$cuboid;

    // Inject at the start of the function, so we don't capture modified locals
    @Inject(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/model/ModelPart$Cuboid;sides:[Lnet/minecraft/client/model/ModelPart$Quad;", ordinal = 0))
    private void onInit(int u, int v, float x, float y, float z, float sizeX, float sizeY, float sizeZ, float extraX, float extraY, float extraZ, boolean mirror, float textureWidth, float textureHeight, Set<Direction> directions, CallbackInfo ci) {
        this.sodium$cuboid = new ModelCuboid(u, v, x, y, z, sizeX, sizeY, sizeZ, extraX, extraY, extraZ, mirror, textureWidth, textureHeight, directions);
    }

    @Override
    public ModelCuboid copy() {
        return this.sodium$cuboid;
    }
}
