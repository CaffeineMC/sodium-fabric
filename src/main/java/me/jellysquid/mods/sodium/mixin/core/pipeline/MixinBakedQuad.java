package me.jellysquid.mods.sodium.mixin.core.pipeline;

import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.jellysquid.mods.sodium.client.util.ModelQuadUtil.*;

@Mixin(BakedQuad.class)
public abstract class MixinBakedQuad implements BakedQuadView {
    @Shadow
    @Final
    protected int[] vertexData;

    @Shadow
    @Final
    protected Sprite sprite;

    @Shadow
    @Final
    protected int colorIndex;

    @Shadow
    @Final
    protected Direction face; // This is really the light face, but we can't rename it.

    @Shadow
    @Final
    private boolean shade;

    private int flags;

    private int normal;
    private float normX;
    private float normY;
    private float normZ;
    private ModelQuadFacing normalFace;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(int[] vertexData, int colorIndex, Direction face, Sprite sprite, boolean shade, CallbackInfo ci) {
        this.calculateAccurateNormal();
        this.normal = NormI8.pack(this.normX, this.normY, this.normZ);
        this.normalFace = ModelQuadUtil.findNormalFace(this.normal);

        this.flags = ModelQuadFlags.getQuadFlags(this, face);
    }

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + POSITION_INDEX]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + POSITION_INDEX + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + POSITION_INDEX + 2]);
    }

    @Override
    public int getColor(int idx) {
        return this.vertexData[vertexOffset(idx) + COLOR_INDEX];
    }

    @Override
    public Sprite getSprite() {
        return this.sprite;
    }

    @Override
    public int getNormal() {
        return this.normal;
    }

    @Override
    public float getNormX() {
        return this.normX;
    }

    @Override
    public float getNormY() {
        return this.normY;
    }

    @Override
    public float getNormZ() {
        return this.normZ;
    }

    @Override
    public void setAccurateNormal(float x, float y, float z) {
        this.normX = x;
        this.normY = y;
        this.normZ = z;
    }

    @Override
    public float getTexU(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + TEXTURE_INDEX]);
    }

    @Override
    public float getTexV(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + TEXTURE_INDEX + 1]);
    }

    @Override
    public int getFlags() {
        return this.flags;
    }

    @Override
    public int getColorIndex() {
        return this.colorIndex;
    }

    @Override
    public ModelQuadFacing getNormalFace() {
        return this.normalFace;
    }

    @Override
    public Direction getLightFace() {
        return this.face;
    }

    @Override
    @Unique(silent = true) // The target class has a function with the same name in a remapped environment
    public boolean hasShade() {
        return this.shade;
    }
}
