package me.jellysquid.mods.sodium.client.render.immediate.model;

import me.jellysquid.mods.sodium.common.util.MatrixHelper;
import net.minecraft.util.math.Direction;
import org.joml.*;

import java.util.Set;

public class ModelCuboid {
    public final Quad[] quads;

    private final Vector3f[] vertices;
    private final Vector3f[] shared;

    public ModelCuboid(int u, int v, float x1, float y1, float z1, float sizeX, float sizeY, float sizeZ, float extraX, float extraY, float extraZ, boolean mirror, float textureWidth, float textureHeight, Set<Direction> renderDirections) {
        float x2 = x1 + sizeX;
        float y2 = y1 + sizeY;
        float z2 = z1 + sizeZ;

        x1 -= extraX;
        y1 -= extraY;
        z1 -= extraZ;

        x2 += extraX;
        y2 += extraY;
        z2 += extraZ;

        if (mirror) {
            float i = x2;
            x2 = x1;
            x1 = i;
        }

        Vector3f[] vertices = new Vector3f[8];
        vertices[0] = new Vector3f(x1, y1, z1);
        vertices[1] = new Vector3f(x2, y1, z1);
        vertices[2] = new Vector3f(x2, y2, z1);
        vertices[3] = new Vector3f(x1, y2, z1);
        vertices[4] = new Vector3f(x1, y1, z2);
        vertices[5] = new Vector3f(x2, y1, z2);
        vertices[6] = new Vector3f(x2, y2, z2);
        vertices[7] = new Vector3f(x1, y2, z2);

        Vector3f[] shared = new Vector3f[8];

        for (int i = 0; i < 8; i++) {
            vertices[i].div(16.0f);
            shared[i] = new Vector3f(Float.NaN);
        }

        float u0 = (float) u;
        float u1 = (float) u + sizeZ;
        float u2 = (float) u + sizeZ + sizeX;
        float u3 = (float) u + sizeZ + sizeX + sizeX;
        float u4 = (float) u + sizeZ + sizeX + sizeZ;
        float u5 = (float) u + sizeZ + sizeX + sizeZ + sizeX;

        float v0 = (float) v;
        float v1 = (float) v + sizeZ;
        float v2 = (float) v + sizeZ + sizeY;

        var sides = new Quad[6];

        if (renderDirections.contains(Direction.DOWN)) {
            sides[2] = new Quad(new Vector3f[] { shared[5], shared[4], shared[0], shared[1] }, u1, v0, u2, v1, textureWidth, textureHeight, mirror, Direction.DOWN);
        }

        if (renderDirections.contains(Direction.UP)) {
            sides[3] = new Quad(new Vector3f[] { shared[2], shared[3], shared[7], shared[6] }, u2, v1, u3, v0, textureWidth, textureHeight, mirror, Direction.UP);
        }

        if (renderDirections.contains(Direction.WEST)) {
            sides[1] = new Quad(new Vector3f[] { shared[0], shared[4], shared[7], shared[3] }, u0, v1, u1, v2, textureWidth, textureHeight, mirror, Direction.WEST);
        }

        if (renderDirections.contains(Direction.NORTH)) {
            sides[4] = new Quad(new Vector3f[] { shared[1], shared[0], shared[3], shared[2] }, u1, v1, u2, v2, textureWidth, textureHeight, mirror, Direction.NORTH);
        }

        if (renderDirections.contains(Direction.EAST)) {
            sides[0] = new Quad(new Vector3f[] { shared[5], shared[1], shared[2], shared[6] }, u2, v1, u4, v2, textureWidth, textureHeight, mirror, Direction.EAST);
        }

        if (renderDirections.contains(Direction.SOUTH)) {
            sides[5] = new Quad(new Vector3f[] { shared[4], shared[5], shared[6], shared[7] }, u4, v1, u5, v2, textureWidth, textureHeight, mirror, Direction.SOUTH);
        }

        this.quads = sides;

        this.vertices = vertices;
        this.shared = shared;
    }

    public void updateVertices(Matrix4f mat) {
        for (int i = 0; i < 8; i++) {
            var src = this.vertices[i];
            var dst = this.shared[i];

            src.mulPosition(mat, dst);
        }
    }

    public static class Quad {
        public final Vector3f[] positions;
        public final Vector2f[] textures;


        public final Vector3f direction;

        public Quad(Vector3f[] positions, float u1, float v1, float u2, float v2, float textureWidth, float textureHeight, boolean flip, Direction direction) {
            var textures = new Vector2f[4];
            textures[0] = new Vector2f(u2 / textureWidth, v1 / textureHeight);
            textures[1] = new Vector2f(u1 / textureWidth, v1 / textureHeight);
            textures[2] = new Vector2f(u1 / textureWidth, v2 / textureHeight);
            textures[3] = new Vector2f(u2 / textureWidth, v2 / textureHeight);

            if (flip) {
                int len = positions.length;

                for (int i = 0; i < len / 2; ++i) {
                    var pos = positions[i];
                    positions[i] = positions[len - 1 - i];
                    positions[len - 1 - i] = pos;

                    var tex = textures[i];
                    textures[i] = textures[len - 1 - i];
                    textures[len - 1 - i] = tex;
                }
            }

            this.positions = positions;
            this.textures = textures;

            this.direction = direction.getUnitVector();

            if (flip) {
                this.direction.mul(-1.0F, 1.0F, 1.0F);
            }
        }

        public int getNormal(Matrix3f mat) {
            return MatrixHelper.transformNormal(mat, this.direction.x, this.direction.y, this.direction.z);
        }
    }
}
