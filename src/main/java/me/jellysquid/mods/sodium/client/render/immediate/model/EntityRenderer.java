package me.jellysquid.mods.sodium.client.render.immediate.model;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class EntityRenderer {

    private static final int NUM_CUBE_VERTICES = 8;
    private static final int NUM_CUBE_FACES = 6;
    private static final int NUM_FACE_VERTICES = 4;

    private static final int
            FACE_NEG_Y = 0, // DOWN
            FACE_POS_Y = 1, // UP
            FACE_NEG_Z = 2, // NORTH
            FACE_POS_Z = 3, // SOUTH
            FACE_NEG_X = 4, // WEST
            FACE_POS_X = 5; // EAST

    private static final int
            VERTEX_X1_Y1_Z1 = 0,
            VERTEX_X2_Y1_Z1 = 1,
            VERTEX_X2_Y2_Z1 = 2,
            VERTEX_X1_Y2_Z1 = 3,
            VERTEX_X1_Y1_Z2 = 4,
            VERTEX_X2_Y1_Z2 = 5,
            VERTEX_X2_Y2_Z2 = 6,
            VERTEX_X1_Y2_Z2 = 7;


    private static final long SCRATCH_BUFFER = MemoryUtil.nmemAlignedAlloc(64, NUM_CUBE_FACES * NUM_FACE_VERTICES * ModelVertex.STRIDE);

    private static final Vector3f[] CUBE_CORNERS = new Vector3f[NUM_CUBE_VERTICES];
    private static final int[][] CUBE_VERTICES = new int[][] {
            { VERTEX_X2_Y1_Z2, VERTEX_X1_Y1_Z2, VERTEX_X1_Y1_Z1, VERTEX_X2_Y1_Z1 },
            { VERTEX_X2_Y2_Z1, VERTEX_X1_Y2_Z1, VERTEX_X1_Y2_Z2, VERTEX_X2_Y2_Z2 },
            { VERTEX_X2_Y1_Z1, VERTEX_X1_Y1_Z1, VERTEX_X1_Y2_Z1, VERTEX_X2_Y2_Z1 },
            { VERTEX_X1_Y1_Z2, VERTEX_X2_Y1_Z2, VERTEX_X2_Y2_Z2, VERTEX_X1_Y2_Z2 },
            { VERTEX_X2_Y1_Z2, VERTEX_X2_Y1_Z1, VERTEX_X2_Y2_Z1, VERTEX_X2_Y2_Z2 },
            { VERTEX_X1_Y1_Z1, VERTEX_X1_Y1_Z2, VERTEX_X1_Y2_Z2, VERTEX_X1_Y2_Z1 },
    };

    private static final Vector3f[][] VERTEX_POSITIONS = new Vector3f[NUM_CUBE_FACES][NUM_FACE_VERTICES];
    private static final Vector3f[][] VERTEX_POSITIONS_MIRRORED = new Vector3f[NUM_CUBE_FACES][NUM_FACE_VERTICES];

    private static final Vector2f[][] VERTEX_TEXTURES = new Vector2f[NUM_CUBE_FACES][NUM_FACE_VERTICES];
    private static final Vector2f[][] VERTEX_TEXTURES_MIRRORED = new Vector2f[NUM_CUBE_FACES][NUM_FACE_VERTICES];

    private static final Vector3f[] CUBE_NORMALS = new Vector3f[NUM_CUBE_FACES];
    private static final Vector3f[] CUBE_NORMALS_MIRRORED = new Vector3f[NUM_CUBE_FACES];

    static {
        for (int cornerIndex = 0; cornerIndex < NUM_CUBE_VERTICES; cornerIndex++) {
            CUBE_CORNERS[cornerIndex] = new Vector3f();
        }

        for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
            CUBE_NORMALS[quadIndex] = new Vector3f();

            for (int vertexIndex = 0; vertexIndex < NUM_FACE_VERTICES; vertexIndex++) {
                VERTEX_TEXTURES[quadIndex][vertexIndex] = new Vector2f();
                VERTEX_POSITIONS[quadIndex][vertexIndex] = CUBE_CORNERS[CUBE_VERTICES[quadIndex][vertexIndex]];
            }
        }

        for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
            for (int vertexIndex = 0; vertexIndex < NUM_FACE_VERTICES; vertexIndex++) {
                VERTEX_TEXTURES_MIRRORED[quadIndex][vertexIndex] = VERTEX_TEXTURES[quadIndex][3 - vertexIndex];
                VERTEX_POSITIONS_MIRRORED[quadIndex][vertexIndex] = VERTEX_POSITIONS[quadIndex][3 - vertexIndex];
            }
        }

        // When mirroring is used, the normals for EAST and WEST are swapped.
        CUBE_NORMALS_MIRRORED[FACE_NEG_Y] = CUBE_NORMALS[FACE_NEG_Y];
        CUBE_NORMALS_MIRRORED[FACE_POS_Y] = CUBE_NORMALS[FACE_POS_Y];
        CUBE_NORMALS_MIRRORED[FACE_NEG_Z] = CUBE_NORMALS[FACE_NEG_Z];
        CUBE_NORMALS_MIRRORED[FACE_POS_Z] = CUBE_NORMALS[FACE_POS_Z];
        CUBE_NORMALS_MIRRORED[FACE_POS_X] = CUBE_NORMALS[FACE_NEG_X]; // mirrored
        CUBE_NORMALS_MIRRORED[FACE_NEG_X] = CUBE_NORMALS[FACE_POS_X]; // mirrored
    }

    public static void render(MatrixStack matrixStack, VertexBufferWriter writer, ModelPart part, int light, int overlay, int color) {
        ModelPartData accessor = ModelPartData.from(part);
        
        if (!accessor.isVisible()) {
            return;
        }

        var cuboids = accessor.getCuboids();
        var children = accessor.getChildren();

        if (ArrayUtils.isEmpty(cuboids) && ArrayUtils.isEmpty(children)) {
            return;
        }

        matrixStack.push();

        part.rotate(matrixStack);

        if (!accessor.isHidden()) {
            renderCuboids(matrixStack.peek(), writer, cuboids, light, overlay, color);
        }

        renderChildren(matrixStack, writer, light, overlay, color, children);

        matrixStack.pop();
    }

    private static void renderChildren(MatrixStack matrices, VertexBufferWriter writer, int light, int overlay, int color, ModelPart[] children) {
        for (ModelPart part : children) {
            render(matrices, writer, part, light, overlay, color);
        }
    }

    private static void renderCuboids(MatrixStack.Entry matrices, VertexBufferWriter writer, ModelCuboid[] cuboids, int light, int overlay, int color) {
        prepareNormals(matrices);

        for (ModelCuboid cuboid : cuboids) {
            prepareVertices(matrices, cuboid);

            var vertexCount = renderCuboid(cuboid, color, overlay, light);

            try (MemoryStack stack = MemoryStack.stackPush()) {
                writer.push(stack, SCRATCH_BUFFER, vertexCount, ModelVertex.FORMAT);
            }
        }
    }

    private static int renderCuboid(ModelCuboid cuboid, int color, int overlay, int light) {
        final int faces = cuboid.faces & getVisibleFaces();

        if (faces == 0) {
            // No faces are visible, so zero vertices will be produced.
            return 0;
        }

        final var positions = cuboid.mirror ? VERTEX_POSITIONS_MIRRORED : VERTEX_POSITIONS;
        final var textures = cuboid.mirror ? VERTEX_TEXTURES_MIRRORED : VERTEX_TEXTURES;
        final var normals = cuboid.mirror ? CUBE_NORMALS_MIRRORED :  CUBE_NORMALS;

        var vertexCount = 0;

        long ptr = SCRATCH_BUFFER;

        for (int quadIndex = 0; quadIndex < NUM_CUBE_FACES; quadIndex++) {
            if ((faces & (1 << quadIndex)) != 0) {
                continue;
            }

            var normal = NormI8.pack(normals[quadIndex]);

            emitVertex(ptr, positions[quadIndex][0], color, textures[quadIndex][0], overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            emitVertex(ptr, positions[quadIndex][1], color, textures[quadIndex][1], overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            emitVertex(ptr, positions[quadIndex][2], color, textures[quadIndex][2], overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            emitVertex(ptr, positions[quadIndex][3], color, textures[quadIndex][3], overlay, light, normal);
            ptr += ModelVertex.STRIDE;

            vertexCount += 4;
        }

        return vertexCount;
    }

    private static void emitVertex(long ptr, Vector3f pos, int color, Vector2f tex, int overlay, int light, int normal) {
        ModelVertex.write(ptr, pos.x, pos.y, pos.z, color, tex.x, tex.y, overlay, light, normal);
    }


    private static int getVisibleFaces() {
        final var min = CUBE_CORNERS[VERTEX_X1_Y1_Z1];
        final var max = CUBE_CORNERS[VERTEX_X2_Y2_Z2];

        int faces = 0;

        // If the dot product between any vertex of a primitive and the normal is positive, then the primitive
        // is front facing, and considered "visible".
        //
        // Since we only need to perform a dot product against *one* vertex of the primitive, we simply use the
        // minimum or maximum vertex of the cube, depending on which face is being tested.
        //
        // For cubes, faces with a positive direction will use vertex (max.x, max.y, max.z), and faces with a negative
        // direction will use vertex (min.x, min.y, min.z). Furthermore, a single face will *never* use both the
        // minimum and maximum vertex.
        //
        // However, Minecraft is not a reasonable game, and cube normals are defined arbitrarily. Worse yet, the faces
        // -X and +X also have their normals flipped when the cuboid is using "mirrored" texturing. So we have to
        // completely ignore the normals Minecraft *normally* uses for rendering (which affects lighting) and use the
        // "true" normals here. This is why you don't see (cuboid.mirror ? CUBE_NORMALS_MIRRORED : CUBE_NORMALS) used.
        //
        // The vertex used for each face in the comparisons below will depend on whether the indices for CUBE_NORMAL[face]
        // contain either the minimum or maximum vertex.
        if (min.dot(CUBE_NORMALS[FACE_NEG_Y]) > 0.0f) {
            faces |= 1 << FACE_NEG_Y;
        }

        if (max.dot(CUBE_NORMALS[FACE_POS_Y]) > 0.0f) {
            faces |= 1 << FACE_POS_Y;
        }

        if (min.dot(CUBE_NORMALS[FACE_NEG_Z]) > 0.0f) {
            faces |= 1 << FACE_NEG_Z;
        }

        if (min.dot(CUBE_NORMALS[FACE_POS_X]) > 0.0f) {
            faces |= 1 << FACE_POS_X;
        }

        if (max.dot(CUBE_NORMALS[FACE_NEG_X]) > 0.0f) {
            faces |= 1 << FACE_NEG_X;
        }

        if (max.dot(CUBE_NORMALS[FACE_POS_Z]) > 0.0f) {
            faces |= 1 << FACE_POS_Z;
        }

        return faces;
    }

    private static void prepareVertices(MatrixStack.Entry matrices, ModelCuboid cuboid) {
        buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y1_Z1], cuboid.x1, cuboid.y1, cuboid.z1, matrices.getPositionMatrix());
        buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y1_Z1], cuboid.x2, cuboid.y1, cuboid.z1, matrices.getPositionMatrix());
        buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y2_Z1], cuboid.x2, cuboid.y2, cuboid.z1, matrices.getPositionMatrix());
        buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y2_Z1], cuboid.x1, cuboid.y2, cuboid.z1, matrices.getPositionMatrix());
        buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y1_Z2], cuboid.x1, cuboid.y1, cuboid.z2, matrices.getPositionMatrix());
        buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y1_Z2], cuboid.x2, cuboid.y1, cuboid.z2, matrices.getPositionMatrix());
        buildVertexPosition(CUBE_CORNERS[VERTEX_X2_Y2_Z2], cuboid.x2, cuboid.y2, cuboid.z2, matrices.getPositionMatrix());
        buildVertexPosition(CUBE_CORNERS[VERTEX_X1_Y2_Z2], cuboid.x1, cuboid.y2, cuboid.z2, matrices.getPositionMatrix());

        buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Y], cuboid.u1, cuboid.v0, cuboid.u2, cuboid.v1);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Y], cuboid.u2, cuboid.v1, cuboid.u3, cuboid.v0);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_Z], cuboid.u1, cuboid.v1, cuboid.u2, cuboid.v2);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_Z], cuboid.u4, cuboid.v1, cuboid.u5, cuboid.v2);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_NEG_X], cuboid.u2, cuboid.v1, cuboid.u4, cuboid.v2);
        buildVertexTexCoord(VERTEX_TEXTURES[FACE_POS_X], cuboid.u0, cuboid.v1, cuboid.u1, cuboid.v2);
    }

    private static void prepareNormals(MatrixStack.Entry matrices) {
        MatrixHelper.transformNormal(CUBE_NORMALS[FACE_NEG_Y], matrices.getNormalMatrix(), Direction.DOWN);
        MatrixHelper.transformNormal(CUBE_NORMALS[FACE_POS_Y], matrices.getNormalMatrix(), Direction.UP);
        MatrixHelper.transformNormal(CUBE_NORMALS[FACE_NEG_Z], matrices.getNormalMatrix(), Direction.NORTH);
        MatrixHelper.transformNormal(CUBE_NORMALS[FACE_POS_Z], matrices.getNormalMatrix(), Direction.SOUTH);
        MatrixHelper.transformNormal(CUBE_NORMALS[FACE_POS_X], matrices.getNormalMatrix(), Direction.WEST);
        MatrixHelper.transformNormal(CUBE_NORMALS[FACE_NEG_X], matrices.getNormalMatrix(), Direction.EAST);
    }

    private static void buildVertexPosition(Vector3f vector, float x, float y, float z, Matrix4f matrix) {
        vector.x = MatrixHelper.transformPositionX(matrix, x, y, z);
        vector.y = MatrixHelper.transformPositionY(matrix, x, y, z);
        vector.z = MatrixHelper.transformPositionZ(matrix, x, y, z);
    }

    private static void buildVertexTexCoord(Vector2f[] uvs, float u1, float v1, float u2, float v2) {
        uvs[0].set(u2, v1);
        uvs[1].set(u1, v1);
        uvs[2].set(u1, v2);
        uvs[3].set(u2, v2);
    }
}
