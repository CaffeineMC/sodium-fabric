package me.jellysquid.mods.sodium.mixin.mojmath;

import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class MixinFrustum implements FrustumExtended {
    private float xF, yF, zF;

    private float nxX, nxY, nxZ, nxW;
    private float pxX, pxY, pxZ, pxW;
    private float nyX, nyY, nyZ, nyW;
    private float pyX, pyY, pyZ, pyW;
    private float nzX, nzY, nzZ, nzW;
    private float pzX, pzY, pzZ, pzW;

    @Inject(method = "setPosition", at = @At("HEAD"))
    private void prePositionUpdate(double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        this.xF = (float) cameraX;
        this.yF = (float) cameraY;
        this.zF = (float) cameraZ;
    }

    @Inject(method = "transform", at = @At("HEAD"))
    private void transform(Matrix4f mat, int x, int y, int z, int index, CallbackInfo ci) {
        Vector4f vec = new Vector4f((float) x, (float) y, (float) z, 1.0F);
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

    @Override
    public boolean fastAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.isAnyCornerVisible(minX - this.xF, minY - this.yF, minZ - this.zF,
                maxX - this.xF, maxY - this.yF, maxZ - this.zF);
    }

    /**
     * @author JellySquid
     * @reason Optimize away object allocations and for-loop
     */
    @Overwrite
    private boolean isAnyCornerVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.nxX * (this.nxX < 0 ? minX : maxX) + this.nxY * (this.nxY < 0 ? minY : maxY) + this.nxZ * (this.nxZ < 0 ? minZ : maxZ) >= -this.nxW &&
                this.pxX * (this.pxX < 0 ? minX : maxX) + this.pxY * (this.pxY < 0 ? minY : maxY) + this.pxZ * (this.pxZ < 0 ? minZ : maxZ) >= -this.pxW &&
                this.nyX * (this.nyX < 0 ? minX : maxX) + this.nyY * (this.nyY < 0 ? minY : maxY) + this.nyZ * (this.nyZ < 0 ? minZ : maxZ) >= -this.nyW &&
                this.pyX * (this.pyX < 0 ? minX : maxX) + this.pyY * (this.pyY < 0 ? minY : maxY) + this.pyZ * (this.pyZ < 0 ? minZ : maxZ) >= -this.pyW &&
                this.nzX * (this.nzX < 0 ? minX : maxX) + this.nzY * (this.nzY < 0 ? minY : maxY) + this.nzZ * (this.nzZ < 0 ? minZ : maxZ) >= -this.nzW &&
                this.pzX * (this.pzX < 0 ? minX : maxX) + this.pzY * (this.pzY < 0 ? minY : maxY) + this.pzZ * (this.pzZ < 0 ? minZ : maxZ) >= -this.pzW;
    }
}
