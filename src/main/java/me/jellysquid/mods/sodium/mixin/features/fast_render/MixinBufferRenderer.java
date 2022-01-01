package me.jellysquid.mods.sodium.mixin.features.fast_render;

import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.immediate.RenderImmediate;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferRenderer.class)
public class MixinBufferRenderer {
    @Inject(method = "draw(Ljava/nio/ByteBuffer;Lnet/minecraft/client/render/VertexFormat$DrawMode;Lnet/minecraft/client/render/VertexFormat;ILnet/minecraft/client/render/VertexFormat$IntType;IZ)V", at = @At("HEAD"), cancellable = true)
    private static void sodium$draw(ByteBuffer buffer, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat, int count, VertexFormat.IntType elementFormat, int vertexCount, boolean textured, CallbackInfo ci) {
        ci.cancel();

        RenderDevice.enterManagedCode();

        try {
            RenderImmediate.getInstance()
                    .draw(buffer, drawMode, vertexFormat, count, elementFormat, vertexCount, textured);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }
}
