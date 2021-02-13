package me.jellysquid.mods.sodium.mixin.features.gui.fast_loading_screen;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.util.color.Color4;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.world.chunk.ChunkStatus;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;

import java.util.IdentityHashMap;

/**
 * Re-implements the loading screen with considerations to reduce draw calls and other sources of overhead. This can
 * improve world load times on slower processors with very few cores.
 */
@Mixin(LevelLoadingScreen.class)
public class MixinLevelLoadingScreen {
    @Mutable
    @Shadow
    @Final
    private static Object2IntMap<ChunkStatus> STATUS_TO_COLOR;

    private static IdentityHashMap<ChunkStatus, Color4> STATUS_TO_COLOR_FAST;

    private static final Color4 NULL_STATUS_COLOR = Color4.fromRGBA(0xFF000000);
    private static final Color4 DEFAULT_STATUS_COLOR = Color4.fromRGBA(0xFF0011FF);

    /**
     * This implementation differs from vanilla's in the following key ways.
     * - All tiles are batched together in one draw call, reducing CPU overhead by an order of magnitudes.
     * - Identity hashing is used for faster ChunkStatus -> Color lookup.
     * - Colors are stored in unpacked RGBA format so conversion is not necessary every tile draw
     *
     * @reason Significantly optimized implementation.
     * @author JellySquid
     */
    @Overwrite
    public static void drawChunkMap(MatrixStack matrixStack, WorldGenerationProgressTracker tracker, int mapX, int mapY, int mapScale, int mapPadding) {
        if (STATUS_TO_COLOR_FAST == null) {
            STATUS_TO_COLOR_FAST = new IdentityHashMap<>(STATUS_TO_COLOR.size());
            STATUS_TO_COLOR_FAST.put(null, NULL_STATUS_COLOR);
            STATUS_TO_COLOR.object2IntEntrySet()
                    .forEach(entry -> STATUS_TO_COLOR_FAST.put(entry.getKey(), Color4.fromRGBA(entry.getIntValue() | 0xFF000000)));
        }

        Matrix4f matrix = matrixStack.peek().getModel();

        Tessellator tessellator = Tessellator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);

        int centerSize = tracker.getCenterSize();
        int size = tracker.getSize();

        if (buffer instanceof VertexBufferView) {
            // calculate how many tiles we're going to draw
            int totalTiles = size * size;
            if (mapPadding != 0)
                totalTiles += 4;

            // expand the buffer now so we don't need to grow it mid-operation
            ((VertexBufferView) buffer).ensureBufferCapacity(VertexFormats.POSITION_COLOR.getVertexSize() * totalTiles);
        }

        int tileSize = mapScale + mapPadding;

        if (mapPadding != 0) {
            int mapRenderCenterSize = centerSize * tileSize - mapPadding;
            int radius = mapRenderCenterSize / 2 + 1;

            addRect(matrix, buffer, mapX - radius, mapY - radius, mapX - radius + 1, mapY + radius, DEFAULT_STATUS_COLOR);
            addRect(matrix, buffer, mapX + radius - 1, mapY - radius, mapX + radius, mapY + radius, DEFAULT_STATUS_COLOR);
            addRect(matrix, buffer, mapX - radius, mapY - radius, mapX + radius, mapY - radius + 1, DEFAULT_STATUS_COLOR);
            addRect(matrix, buffer, mapX - radius, mapY + radius - 1, mapX + radius, mapY + radius, DEFAULT_STATUS_COLOR);
        }

        int mapRenderSize = size * tileSize - mapPadding;
        int mapStartX = mapX - mapRenderSize / 2;
        int mapStartY = mapY - mapRenderSize / 2;

        ChunkStatus prevStatus = null;
        Color4 prevColor = NULL_STATUS_COLOR;

        for (int x = 0; x < size; ++x) {
            int tileX = mapStartX + x * tileSize;

            for (int z = 0; z < size; ++z) {
                int tileY = mapStartY + z * tileSize;

                ChunkStatus status = tracker.getChunkStatus(x, z);
                Color4 color;

                if (prevStatus == status) {
                    color = prevColor;
                } else {
                    color = STATUS_TO_COLOR_FAST.get(status);

                    prevStatus = status;
                    prevColor = color;
                }

                addRect(matrix, buffer, tileX, tileY, tileX + mapScale, tileY + mapScale, color);
            }
        }

        tessellator.draw();

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void addRect(Matrix4f matrix, BufferBuilder buffer, int x1, int y1, int x2, int y2, Color4 color) {
        buffer.vertex(matrix, x1, y2, 0).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(matrix, x2, y2, 0).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(matrix, x2, y1, 0).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(matrix, x1, y1, 0).color(color.r, color.g, color.b, color.a).next();
    }
}
