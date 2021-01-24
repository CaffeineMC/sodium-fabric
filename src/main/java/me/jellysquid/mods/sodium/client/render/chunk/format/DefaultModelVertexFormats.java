package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.render.chunk.format.hfp.HFPModelVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.sfp.SFPModelVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.xhfp.XHFPModelVertexType;

public class DefaultModelVertexFormats {
    public static final HFPModelVertexType MODEL_VERTEX_HFP = null;
    public static final XHFPModelVertexType MODEL_VERTEX_XHFP = new XHFPModelVertexType();
    public static final SFPModelVertexType MODEL_VERTEX_SFP = null;
}
