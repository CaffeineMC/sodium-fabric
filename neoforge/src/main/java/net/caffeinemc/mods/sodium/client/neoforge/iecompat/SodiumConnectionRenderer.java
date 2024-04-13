/*
 * BluSunrize
 * Copyright (c) 2022
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package net.caffeinemc.mods.sodium.client.neoforge.iecompat;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.utils.ResettableLazy;
import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.Connection.CatenaryData;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireCollisionData.ConnectionSegments;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import malte0811.modelsplitter.model.UVCoords;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SodiumConnectionRenderer implements ResourceManagerReloadListener
{
    private static final LoadingCache<SectionKey, List<RenderedSegment>> SEGMENT_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(120, TimeUnit.SECONDS)
            .build(CacheLoader.from(SodiumConnectionRenderer::renderSectionForCache));
    private static final ResettableLazy<TextureAtlasSprite> WIRE_TEXTURE = new ResettableLazy<>(
            () -> Minecraft.getInstance().getModelManager()
                    .getAtlas(InventoryMenu.BLOCK_ATLAS)
                    .getSprite(ImmersiveEngineering.rl("block/wire"))
    );

    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager pResourceManager)
    {
        WIRE_TEXTURE.reset();
        resetCache();
    }

    public static void resetCache()
    {
        SEGMENT_CACHE.invalidateAll();
    }

    public static void renderConnectionsInSection(
            ChunkBuildBuffers buffers,
            @Nullable BlockAndTintGetter region, SectionPos renderChunk
    )
    {
        if(region==null)
            return;
        renderConnectionsInSection(
                renderType -> buffers.get(DefaultMaterials.forRenderLayer(renderType)),
                region,
                renderChunk.origin()
        );
    }

    public static boolean sectionNeedsRendering(SectionPos section)
    {
        GlobalWireNetwork globalNet = GlobalWireNetwork.getNetwork(Minecraft.getInstance().level);
        List<ConnectionSegments> connectionParts = globalNet.getCollisionData().getWiresIn(section);
        return connectionParts!=null&&!connectionParts.isEmpty();
    }

    public static void renderConnectionsInSection(
            Function<RenderType, ChunkModelBuilder> getBuffer, BlockAndTintGetter region, BlockPos sectionOrigin
    )
    {
        SectionPos section = SectionPos.of(sectionOrigin);
        GlobalWireNetwork globalNet = GlobalWireNetwork.getNetwork(Minecraft.getInstance().level);
        List<ConnectionSegments> connectionParts = globalNet.getCollisionData().getWiresIn(section);
        if(connectionParts==null||connectionParts.isEmpty())
            return;
        RenderType renderType = RenderType.solid();
        ChunkModelBuilder builder = getBuffer.apply(renderType);
        for(ConnectionSegments connection : connectionParts)
        {
            ConnectionPoint connectionOrigin = connection.connection().getEndA();
            renderSegments(
                    builder, connection,
                    connectionOrigin.getX()-sectionOrigin.getX(),
                    connectionOrigin.getY()-sectionOrigin.getY(),
                    connectionOrigin.getZ()-sectionOrigin.getZ(),
                    region
            );
        }
    }

    public static void renderSegments(
            ChunkModelBuilder out, ConnectionSegments toRender, int offX, int offY, int offZ, BlockAndTintGetter level
    )
    {
        Connection connection = toRender.connection();
        int color = connection.type.getColour(connection);
        double radius = connection.type.getRenderDiameter()/2;
        int lastLight = 0;
        List<RenderedSegment> renderedSection = SEGMENT_CACHE.getUnchecked(new SectionKey(
                radius, color, connection.getCatenaryData(), toRender.firstPointToRender(), toRender.lastPointToRender()
        ));
        for(int i = 0; i < renderedSection.size(); ++i)
        {
            RenderedSegment segment = renderedSection.get(i);
            if(i==0)
                lastLight = getLight(connection, segment.offsetStart, level);
            int nextLight = getLight(connection, segment.offsetEnd, level);
            segment.render(lastLight, nextLight, OverlayTexture.NO_OVERLAY, offX, offY, offZ, out);
            lastLight = nextLight;
        }
    }

    public static void renderConnection(
            ChunkModelBuilder out,
            CatenaryData catenaryData, double radius, int color,
            int light, int overlay
    )
    {
        final List<RenderedSegment> section = SEGMENT_CACHE.getUnchecked(new SectionKey(
                radius, color, catenaryData, 0, Connection.RENDER_POINTS_PER_WIRE
        ));
        for(RenderedSegment renderedSegment : section)
            renderedSegment.render(light, light, overlay, 0, 0, 0, out);
    }

    private static List<RenderedSegment> renderSectionForCache(SectionKey key)
    {
        CatenaryData catenaryData = key.catenaryShape();
        List<RenderedSegment> segments = new ArrayList<>(key.lastIndex-key.firstIndex);
        for(int startIndex = key.firstIndex; startIndex < key.lastIndex; ++startIndex)
        {
            List<Vertex> vertices = new ArrayList<>(4*4);
            Vec3 start = key.catenaryShape().getRenderPoint(startIndex);
            Vec3 end = key.catenaryShape().getRenderPoint(startIndex+1);
            Vec3 horNormal;
            if(key.catenaryShape().isVertical())
                horNormal = new Vec3(1, 0, 0);
            else
                horNormal = new Vec3(-catenaryData.delta().z, 0, catenaryData.delta().x).normalize();
            Vec3 verticalNormal = start.subtract(end).cross(horNormal).normalize();
            Vec3 horRadius = horNormal.scale(key.radius());
            Vec3 verticalRadius = verticalNormal.scale(-key.radius());

            renderBidirectionalQuad(vertices, start, end, horRadius, key.color(), verticalNormal);
            renderBidirectionalQuad(vertices, start, end, verticalRadius, key.color(), horNormal);
            segments.add(new RenderedSegment(
                    vertices, BlockPos.containing(start), BlockPos.containing(end)
            ));
        }
        return segments;
    }

    private static int getLight(Connection connection, Vec3i point, BlockAndTintGetter level)
    {
        return LevelRenderer.getLightColor(level, connection.getEndA().position().offset(point));
    }

    //TODO move somewhere else
    private static int getByte(int value, int lowestBit)
    {
        return (value >> lowestBit)&255;
    }

    private static void renderBidirectionalQuad(
            List<Vertex> out, Vec3 start, Vec3 end, Vec3 radius, int color, Vec3 positiveNormal
    )
    {
        TextureAtlasSprite texture = WIRE_TEXTURE.get();
        UVCoords[] uvs = {
                new UVCoords(texture.getU0(), texture.getV0()),
                new UVCoords(texture.getU1(), texture.getV0()),
                new UVCoords(texture.getU1(), texture.getV1()),
                new UVCoords(texture.getU0(), texture.getV1()),
        };
        Vec3[] vertices = {start.add(radius), end.add(radius), end.subtract(radius), start.subtract(radius),};
        for(int i = 0; i < vertices.length; i++)
            out.add(vertex(vertices[i], uvs[i], color, positiveNormal, i==0||i==3));
        for(int i = vertices.length-1; i >= 0; i--)
            out.add(vertex(vertices[i], uvs[i], color, positiveNormal.scale(-1), i==0||i==3));
    }

    private static Vertex vertex(Vec3 point, UVCoords uv, int color, Vec3 normal, boolean lightForStart)
    {
        return new Vertex(
                (float)point.x, (float)point.y, (float)point.z,
                (float)uv.u(), (float)uv.v(),
                getByte(color, 16)/255f, getByte(color, 8)/255f, getByte(color, 0)/255f,
                (float)normal.x, (float)normal.y, (float)normal.y,
                ModelQuadFacing.fromNormal((float) normal.x, (float) normal.y, (float) normal.z),
                lightForStart
        );
    }

    private record SectionKey(double radius, int color, CatenaryData catenaryShape, int firstIndex, int lastIndex)
    {
    }

    private record Vertex(
            float posX, float posY, float posZ,
            float texU, float texV,
            float red, float green, float blue,
            float normalX, float normalY, float normalZ,
            ModelQuadFacing facing,
            boolean lightForStart
    )
    {
    }

    private record RenderedSegment(List<Vertex> vertices, Vec3i offsetStart, Vec3i offsetEnd)
    {
        public void render(int lightStart, int lightEnd, int overlay, int offX, int offY, int offZ, ChunkModelBuilder out)
        {
            if (vertices.size() % 4 != 0) {
                throw new IllegalStateException("IE wires must be quads!");
            }

            ChunkVertexEncoder.Vertex[] sVertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

            int quadCount = 0;
            for (int i = 0; i < vertices.size() / 4; i++) {
                for (int e = 0; e < 4; e++) {
                    Vertex v = vertices.get((i * 4) + e);
                    sVertices[e].x = offX+v.posX;
                    sVertices[e].y = offY+v.posY;
                    sVertices[e].z = offZ+v.posZ;
                    sVertices[e].u = v.texU;
                    sVertices[e].v = v.texV;
                    sVertices[e].light = v.lightForStart?lightStart: lightEnd;
                    sVertices[e].color = ColorABGR.pack(v.red, v.green, v.blue, 1);
                }
                out.getVertexBuffer(vertices.get((i * 4)).facing).push(sVertices, DefaultMaterials.SOLID);
                quadCount++;
            }
        }
    }
}