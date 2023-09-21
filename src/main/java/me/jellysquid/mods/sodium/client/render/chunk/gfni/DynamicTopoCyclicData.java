package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * TODO: implement
 */
public class DynamicTopoCyclicData extends PresentTranslucentData {
    public final ReferenceArrayList<SortOrder> orders;

    public static record SortOrder(VertexRange range, Vector3fc normal, double distance) {
    }

    public DynamicTopoCyclicData(ChunkSectionPos sectionPos,
            NativeBuffer buffer, ReferenceArrayList<SortOrder> orders) {
        super(sectionPos, buffer);
        this.orders = orders;
    }

    @Override
    public SortType getSortType() {
        throw new UnsupportedOperationException();
    }
}
