package me.jellysquid.mods.sodium.client.render.backends.shader.cr;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.GlShaderProgram;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class BoundingBoxShader extends GlShaderProgram {
    private static final float MODEL_SIZE = 16.0f;

    private final int uModelViewMatrix;
    private final int uProjectionMatrix;
    private final int uModelOffset;

    public final GlVertexAttributeBinding[] attributes;

    private final FloatBuffer uModelOffsetBuffer;

    protected BoundingBoxShader(Identifier name, int program,  GlVertexFormat<VertexAttribute> format) {
        super(name, program);

        this.uModelViewMatrix = this.getUniformLocation("u_ModelViewMatrix");
        this.uProjectionMatrix = this.getUniformLocation("u_ProjectionMatrix");
        this.uModelOffset = this.getUniformLocation("u_ModelOffset");

        int aPos = this.getAttributeLocation("a_Pos");

        this.attributes = new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(aPos, format, VertexAttribute.POSITION),
        };

        this.uModelOffsetBuffer = MemoryUtil.memAllocFloat(3);
    }

    static BoundingBoxShader createBoundingBoxShader() {
        GlVertexFormat<VertexAttribute> format = GlVertexAttribute.builder(VertexAttribute.class)
                .add(VertexAttribute.POSITION, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 3, false, 0))
                .build(12);

        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium:bounding_box.v.glsl"));
        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium:bounding_box.f.glsl"));

        try {
            return builder(new Identifier("sodium", "bounding_box_shader"))
                    .attach(vertShader)
                    .attach(fragShader)
                    .link((program, name) -> new BoundingBoxShader(program, name, format));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    @Override
    public void bind() {
        super.bind();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufProjection = stack.mallocFloat(16);
            GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, bufProjection);

            GL20.glUniformMatrix4fv(this.uProjectionMatrix, false, bufProjection);
        }
    }


    public void setModelMatrix(MatrixStack matrixStack, double x, double y, double z) {
        matrixStack.push();
        matrixStack.translate(-x, -y, -z);

        MatrixStack.Entry entry = matrixStack.peek();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufModelView = stack.mallocFloat(16);
            entry.getModel().writeToBuffer(bufModelView);

            GL20.glUniformMatrix4fv(this.uModelViewMatrix, false, bufModelView);
        }

        matrixStack.pop();
    }

    public void setModelOffset(ChunkSectionPos pos, int offsetX, int offsetY, int offsetZ) {
        FloatBuffer buf = this.uModelOffsetBuffer;
        buf.put(0, (pos.getX() - offsetX) * MODEL_SIZE);
        buf.put(1, (pos.getY() - offsetY) * MODEL_SIZE);
        buf.put(2, (pos.getZ() - offsetZ) * MODEL_SIZE);

        GL20.glUniform3fv(this.uModelOffset, buf);
    }

    public static enum VertexAttribute {
        POSITION
    }
}
