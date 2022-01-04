package me.jellysquid.mods.sodium.render.immediate;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.opengl.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.opengl.shader.uniform.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class VanillaShaderInterface {
    @Nullable
    public final UniformMatrix4 modelViewMat;
    @Nullable
    public final UniformMatrix4 projectionMat;
    @Nullable
    public final UniformMatrix3 inverseViewRotationMat;
    @Nullable
    public final UniformMatrix4 textureMat;
    @Nullable
    public final UniformFloatArray screenSize;
    @Nullable
    public final UniformFloatArray colorModulator;
    @Nullable
    public final UniformFloatArray light0Direction;
    @Nullable
    public final UniformFloatArray light1Direction;
    @Nullable
    public final UniformFloat fogStart;
    @Nullable
    public final UniformFloat fogEnd;
    public final UniformFloatArray fogColor;
    @Nullable
    public final UniformFloat lineWidth;
    @Nullable
    public final UniformFloat gameTime;
    @Nullable
    public final UniformFloatArray chunkOffset;

    private final Object2ReferenceMap<String, UniformInt> samplerUniforms = new Object2ReferenceOpenHashMap<>();
    private final Object2IntMap<String> samplerLocations = new Object2IntOpenHashMap<>();

    public VanillaShaderInterface(ShaderBindingContext context, List<String> samplerNames) {
        this.samplerLocations.defaultReturnValue(-1);

        this.modelViewMat = context.bindUniform("ModelViewMat", UniformMatrix4.of());
        this.projectionMat = context.bindUniform("ProjMat", UniformMatrix4.of());
        this.inverseViewRotationMat = context.bindUniform("IViewRotMat", UniformMatrix3.of());
        this.textureMat = context.bindUniform("TextureMat", UniformMatrix4.of());
        this.screenSize = context.bindUniform("ScreenSize", UniformFloatArray.ofSize(2));
        this.colorModulator = context.bindUniform("ColorModulator", UniformFloatArray.ofSize(4));
        this.light0Direction = context.bindUniform("Light0_Direction", UniformFloatArray.ofSize(3));
        this.light1Direction = context.bindUniform("Light1_Direction", UniformFloatArray.ofSize(3));
        this.fogStart = context.bindUniform("FogStart", UniformFloat.of());
        this.fogEnd = context.bindUniform("FogEnd", UniformFloat.of());
        this.fogColor = context.bindUniform("FogColor", UniformFloatArray.ofSize(4));
        this.lineWidth = context.bindUniform("LineWidth", UniformFloat.of());
        this.gameTime = context.bindUniform("GameTime", UniformFloat.of());
        this.chunkOffset = context.bindUniform("ChunkOffset", UniformFloatArray.ofSize(3));

        for (var name : samplerNames) {
            var location = this.samplerLocations.size();

            var uniform = context.bindUniform(name, UniformInt.of());
            uniform.setInt(location);

            this.samplerUniforms.put(name, uniform);
            this.samplerLocations.put(name, location);
        }
    }

    public int getSamplerLocation(String name) {
        return this.samplerLocations.getInt(name);
    }
}
