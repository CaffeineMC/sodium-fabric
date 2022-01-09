package me.jellysquid.mods.sodium.render.immediate;

import me.jellysquid.mods.sodium.opengl.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.opengl.shader.uniform.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VanillaShaderInterface {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable public final UniformMatrix4 modelViewMat;
    @Nullable public final UniformMatrix4 projectionMat;
    @Nullable public final UniformMatrix3 inverseViewRotationMat;
    @Nullable public final UniformMatrix4 textureMat;
    @Nullable public final UniformFloatArray screenSize;
    @Nullable public final UniformFloatArray colorModulator;
    @Nullable public final UniformFloatArray light0Direction;
    @Nullable public final UniformFloatArray light1Direction;
    @Nullable public final UniformFloat fogStart;
    @Nullable public final UniformFloat fogEnd;
    @Nullable public final UniformFloatArray fogColor;
    @Nullable public final UniformFloat lineWidth;
    @Nullable public final UniformFloat gameTime;
    @Nullable public final UniformFloatArray chunkOffset;

    private final List<SamplerUniform> samplerUniforms;

    public VanillaShaderInterface(ShaderBindingContext context, List<String> samplerNames) {
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

        this.samplerUniforms = this.initSamplers(context, samplerNames);
    }

    private List<SamplerUniform> initSamplers(ShaderBindingContext context, List<String> samplerNames) {
        var samplers = new ArrayList<SamplerUniform>();

        for (var name : samplerNames) {
            var target = getTextureTarget(name);

            if (target < 0) {
                LOGGER.warn("Couldn't find suitable texture target for sampler uniform: {}", name);
                continue;
            }

            var unit = samplers.size();

            var uniform = context.bindUniform(name, UniformInt.of());
            uniform.setInt(unit);

            samplers.add(new SamplerUniform(unit, target));
        }

        return Collections.unmodifiableList(samplers);
    }

    private static int getTextureTarget(String samplerName) {
        if (samplerName.startsWith("Sampler")) {
            return Integer.parseUnsignedInt(samplerName.substring("Sampler".length()));
        }

        return -1;
    }

    public Iterable<SamplerUniform> getSamplerUniforms() {
        return this.samplerUniforms;
    }

    public enum BufferTarget {
        VERTICES
    }

    public record SamplerUniform(int unit, int target) {

    }
}
