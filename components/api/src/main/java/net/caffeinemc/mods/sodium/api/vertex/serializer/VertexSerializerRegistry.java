package net.caffeinemc.mods.sodium.api.vertex.serializer;

import net.caffeinemc.mods.sodium.api.internal.DependencyInjection;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;

public interface VertexSerializerRegistry {
    VertexSerializerRegistry INSTANCE = DependencyInjection.load(VertexSerializerRegistry.class,
            "me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializerRegistryImpl");;

    static VertexSerializerRegistry instance() {
        return INSTANCE;
    }

    VertexSerializer get(VertexFormatDescription srcFormat, VertexFormatDescription dstFormat);
}
