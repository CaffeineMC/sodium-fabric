package me.jellysquid.mods.sodium.client.render.vertex;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.render.VertexFormat;

public class VertexFormatRegistry {
    private static final Reference2ReferenceMap<VertexFormat, VertexFormatDescription> DESCRIPTIONS = new Reference2ReferenceOpenHashMap<>();

    public synchronized static VertexFormatDescription get(VertexFormat format) {
        var desc = DESCRIPTIONS.get(format);

        if (desc == null) {
            DESCRIPTIONS.put(format, desc = new VertexFormatDescription(format, DESCRIPTIONS.size()));
        }

        return desc;
    }
}
