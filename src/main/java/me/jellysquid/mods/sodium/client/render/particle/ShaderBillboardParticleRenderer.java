package me.jellysquid.mods.sodium.client.render.particle;

import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.render.particle.shader.ParticleShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.particle.shader.ParticleShaderInterface;
import net.minecraft.util.Identifier;

public class ShaderBillboardParticleRenderer {
    protected GlProgram<ParticleShaderInterface> activeProgram;

    public ShaderBillboardParticleRenderer() {
        this.activeProgram = createShader("particles/particle");
    }

    public GlProgram<ParticleShaderInterface> getActiveProgram() {
        return activeProgram;
    }

    private GlProgram<ParticleShaderInterface> createShader(String path) {
        ShaderConstants constants = ShaderConstants.builder().build();

        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX,
                new Identifier("sodium", path + ".vsh"), constants);

        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT,
                new Identifier("sodium", path + ".fsh"), constants);

        try {
            return GlProgram.builder(new Identifier("sodium", "billboard_particle_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("in_Position", ParticleShaderBindingPoints.ATTRIBUTE_POSITION)
                    .bindAttribute("in_Size", ParticleShaderBindingPoints.ATTRIBUTE_SIZE)
                    .bindAttribute("in_TexCoord", ParticleShaderBindingPoints.ATTRIBUTE_TEXTURE)
                    .bindAttribute("in_Color", ParticleShaderBindingPoints.ATTRIBUTE_COLOR)
                    .bindAttribute("in_Light", ParticleShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
                    .bindAttribute("in_Angle", ParticleShaderBindingPoints.ATTRIBUTE_ANGLE)
                    .bindFragmentData("out_FragColor", ParticleShaderBindingPoints.FRAG_COLOR)
                    .link(ParticleShaderInterface::new);
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    public void begin() {
        // pass.startDrawing(); .. Do I need a pass?

        this.activeProgram.bind();
        this.activeProgram.getInterface().setupState();
    }

    public void end() {
        this.activeProgram.unbind();
    }
}
