package me.jellysquid.mods.sodium.mixin.core;

import me.jellysquid.mods.sodium.interop.vanilla.mixin.ShaderExtended;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.shader.ShaderDescription;
import me.jellysquid.mods.sodium.opengl.shader.ShaderType;
import me.jellysquid.mods.sodium.render.immediate.VanillaShaderInterface;
import net.minecraft.client.gl.GlBlendState;
import net.minecraft.client.gl.Program;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.resource.ResourceFactory;
import org.lwjgl.opengl.GL45C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Shader.class)
public class MixinShader implements ShaderExtended {
    @Shadow
    @Final
    private Program vertexShader;

    @Shadow
    @Final
    private Program fragmentShader;

    @Shadow
    @Final
    private List<String> samplerNames;

    @Shadow
    @Final
    private GlBlendState blendState;

    @Unique
    private me.jellysquid.mods.sodium.opengl.shader.Program<VanillaShaderInterface> sodium$program;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sodium$init(ResourceFactory factory, String name, VertexFormat format, CallbackInfo ci) {
        var desc = ShaderDescription.builder();

        if (this.vertexShader != null) {
            desc.addShaderSource(ShaderType.VERTEX, getShaderSource(this.vertexShader));
        }

        if (this.fragmentShader != null) {
            desc.addShaderSource(ShaderType.FRAGMENT, getShaderSource(this.fragmentShader));
        }

        int nextAttributeIndex = 0;

        for (String attributeName : format.getShaderAttributes()) {
            desc.addAttributeBinding(attributeName, nextAttributeIndex++);
        }

        this.sodium$program = RenderDevice.INSTANCE.createProgram(desc.build(), context -> new VanillaShaderInterface(context, this.samplerNames));
    }

    private static String getShaderSource(Program program) {
        return GL45C.glGetShaderSource(program.getShaderRef());
    }

    @Override
    public me.jellysquid.mods.sodium.opengl.shader.Program<VanillaShaderInterface> sodium$getProgram() {
        return this.sodium$program;
    }

    @Override
    public void setup() {
        this.blendState.enable();
    }
}
