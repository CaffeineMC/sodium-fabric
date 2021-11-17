package me.jellysquid.mods.sodium.interop.vanilla.layer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;

public interface MultiPhaseParametersAccessor {

    RenderPhase.TextureBase getTexture();

    RenderPhase.Transparency getTransparency();

    RenderPhase.DepthTest getDepthTest();

    RenderPhase.Cull getCull();

    RenderPhase.Lightmap getLightmap();

    RenderPhase.Overlay getOverlay();

    RenderPhase.Layering getLayering();

    RenderPhase.Target getTarget();

    RenderPhase.Texturing getTexturing();

    RenderPhase.WriteMaskState getWriteMaskState();

    RenderPhase.LineWidth getLineWidth();

    RenderLayer.OutlineMode getOutlineMode();

    RenderPhase.Shader getShader();
}
