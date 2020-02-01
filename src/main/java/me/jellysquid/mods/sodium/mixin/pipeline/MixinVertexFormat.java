package me.jellysquid.mods.sodium.mixin.pipeline;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.render.vertex.ExtendedVertexFormat;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexFormat.class)
public abstract class MixinVertexFormat implements ExtendedVertexFormat {
    @Shadow
    public abstract int getVertexSize();

    @Shadow
    public abstract ImmutableList<VertexFormatElement> getElements();

    @Override
    public void setupVertexArrayState(long pointer) {
        int size = this.getVertexSize();
        int offset = 0;

        for (VertexFormatElement element : this.getElements()) {
            setupVertexArrayState(element.getType(), element.getCount(), element.getFormat().getGlId(), pointer + offset, size, element.getIndex());
            offset += element.getSize();
        }
    }

    private static void setupVertexArrayState(VertexFormatElement.Type type, int count, int format, long pointer, int stride, int index) {
        switch (type) {
            case POSITION:
                GlStateManager.vertexPointer(count, format, stride, pointer);
                GL30.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                break;
            case NORMAL:
                GlStateManager.normalPointer(format, stride, pointer);
                GL30.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                break;
            case COLOR:
                GlStateManager.colorPointer(count, format, stride, pointer);
                GL30.glEnableClientState(GL11.GL_COLOR_ARRAY);
                break;
            case UV:
                GlStateManager.clientActiveTexture(GL13.GL_TEXTURE0 + index);
                GlStateManager.texCoordPointer(count, format, stride, pointer);
                GL30.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                break;
            case PADDING:
                break;
            default:
                throw new UnsupportedOperationException("Type does not support setting up a Vertex Array state");
        }
    }


}
