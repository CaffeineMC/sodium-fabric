package me.jellysquid.mods.sodium.client.compatibility.checks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;

import java.util.List;

/**
 * Metadata read from a resource pack's `pack.mcmeta` file that defines the names
 * of all shaders in the resource pack that the author has confirmed are compatible
 * with Sodium. The specified names will be passed over when checking for incompatibilities.
 */
public record SafeShadersMetadata(List<String> names) {
    public static final Codec<SafeShadersMetadata> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(Codec.STRING.listOf().fieldOf("names")
                    .forGetter(SafeShadersMetadata::names))
                    .apply(instance, SafeShadersMetadata::new)
    );
    public static final ResourceMetadataSerializer<SafeShadersMetadata> SERIALIZER =
            ResourceMetadataSerializer.fromCodec("safe_shaders", CODEC);
}
