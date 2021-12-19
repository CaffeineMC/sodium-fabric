package me.jellysquid.mods.sodium.mixin.features.entity.instancing;

import me.jellysquid.mods.sodium.interop.vanilla.layer.MultiPhaseParametersAccessor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderLayer.MultiPhaseParameters.class)
public class MixinMultiPhaseParameters implements MultiPhaseParametersAccessor {

    // privates gone because of aw?
    @Shadow
    @Final
    RenderPhase.TextureBase texture;

    @Shadow
    @Final
    private RenderPhase.Transparency transparency;

    @Shadow
    @Final
    private RenderPhase.DepthTest depthTest;

    @Shadow
    @Final
    RenderPhase.Cull cull;

    @Shadow
    @Final
    private RenderPhase.Lightmap lightmap;

    @Shadow
    @Final
    private RenderPhase.Overlay overlay;

    @Shadow
    @Final
    private RenderPhase.Layering layering;

    @Shadow
    @Final
    private RenderPhase.Target target;

    @Shadow
    @Final
    private RenderPhase.Texturing texturing;

    @Shadow
    @Final
    private RenderPhase.WriteMaskState writeMaskState;

    @Shadow
    @Final
    private RenderPhase.LineWidth lineWidth;

    @Shadow
    @Final
    private RenderPhase.Shader shader;

    @Shadow
    @Final
    RenderLayer.OutlineMode outlineMode;

    @Override
    public RenderPhase.TextureBase getTexture() {
        return texture;
    }

    @Override
    public RenderPhase.Transparency getTransparency() {
        return transparency;
    }

    @Override
    public RenderPhase.DepthTest getDepthTest() {
        return depthTest;
    }

    @Override
    public RenderPhase.Cull getCull() {
        return cull;
    }

    @Override
    public RenderPhase.Lightmap getLightmap() {
        return lightmap;
    }

    @Override
    public RenderPhase.Overlay getOverlay() {
        return overlay;
    }

    @Override
    public RenderPhase.Layering getLayering() {
        return layering;
    }

    @Override
    public RenderPhase.Target getTarget() {
        return target;
    }

    @Override
    public RenderPhase.Texturing getTexturing() {
        return texturing;
    }

    @Override
    public RenderPhase.WriteMaskState getWriteMaskState() {
        return writeMaskState;
    }

    @Override
    public RenderPhase.LineWidth getLineWidth() {
        return lineWidth;
    }

    @Override
    public RenderLayer.OutlineMode getOutlineMode() {
        return outlineMode;
    }

    @Override
    public RenderPhase.Shader getShader() {
        return shader;
    }
}
