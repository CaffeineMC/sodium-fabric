package me.jellysquid.mods.sodium.mixin.fast_mojmath;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class MixinFrustum {
    private float nxX, nxY, nxZ, nxW;
    private float pxX, pxY, pxZ, pxW;
    private float nyX, nyY, nyZ, nyW;
    private float pyX, pyY, pyZ, pyW;
    private float nzX, nzY, nzZ, nzW;
    private float pzX, pzY, pzZ, pzW;

    @Inject(method = "transform", at = @At("HEAD"))
    private void transform(Matrix4f mat, int x, int y, int z, int index, CallbackInfo ci) {
        Vector4f vec = new Vector4f((float)x, (float)y, (float)z, 1.0F);
        vec.transform(mat);
        vec.normalize();

        switch (index) {
            case 0:
                this.nxX = vec.getX();
                this.nxY = vec.getY();
                this.nxZ = vec.getZ();
                this.nxW = vec.getW();
                break;
            case 1:
                this.pxX = vec.getX();
                this.pxY = vec.getY();
                this.pxZ = vec.getZ();
                this.pxW = vec.getW();
                break;
            case 2:
                this.nyX = vec.getX();
                this.nyY = vec.getY();
                this.nyZ = vec.getZ();
                this.nyW = vec.getW();
                break;
            case 3:
                this.pyX = vec.getX();
                this.pyY = vec.getY();
                this.pyZ = vec.getZ();
                this.pyW = vec.getW();
                break;
            case 4:
                this.nzX = vec.getX();
                this.nzY = vec.getY();
                this.nzZ = vec.getZ();
                this.nzW = vec.getW();
                break;
            case 5:
                this.pzX = vec.getX();
                this.pzY = vec.getY();
                this.pzZ = vec.getZ();
                this.pzW = vec.getW();
                break;
            default:
                throw new IllegalArgumentException("Invalid index");
        }
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private boolean isAnyCornerVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return nxX * (nxX < 0 ? minX : maxX) + nxY * (nxY < 0 ? minY : maxY) + nxZ * (nxZ < 0 ? minZ : maxZ) >= -nxW &&
                pxX * (pxX < 0 ? minX : maxX) + pxY * (pxY < 0 ? minY : maxY) + pxZ * (pxZ < 0 ? minZ : maxZ) >= -pxW &&
                nyX * (nyX < 0 ? minX : maxX) + nyY * (nyY < 0 ? minY : maxY) + nyZ * (nyZ < 0 ? minZ : maxZ) >= -nyW &&
                pyX * (pyX < 0 ? minX : maxX) + pyY * (pyY < 0 ? minY : maxY) + pyZ * (pyZ < 0 ? minZ : maxZ) >= -pyW &&
                nzX * (nzX < 0 ? minX : maxX) + nzY * (nzY < 0 ? minY : maxY) + nzZ * (nzZ < 0 ? minZ : maxZ) >= -nzW &&
                pzX * (pzX < 0 ? minX : maxX) + pzY * (pzY < 0 ? minY : maxY) + pzZ * (pzZ < 0 ? minZ : maxZ) >= -pzW;
    }
}
