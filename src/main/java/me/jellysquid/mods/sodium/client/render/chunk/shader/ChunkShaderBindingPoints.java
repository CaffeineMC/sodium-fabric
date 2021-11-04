package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderBindingPoint;

public class ChunkShaderBindingPoints {
    public static final ShaderBindingPoint ATTRIBUTE_POSITION_ID = new ShaderBindingPoint(1);
    public static final ShaderBindingPoint ATTRIBUTE_COLOR = new ShaderBindingPoint(2);

    public static final ShaderBindingPoint ATTRIBUTE_BLOCK_TEXTURE = new ShaderBindingPoint(3);
    public static final ShaderBindingPoint ATTRIBUTE_LIGHT_TEXTURE = new ShaderBindingPoint(4);

    public static final ShaderBindingPoint FRAG_COLOR = new ShaderBindingPoint(0);
}
