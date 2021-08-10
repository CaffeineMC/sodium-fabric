package me.jellysquid.mods.sodium.client.resource;

import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public interface ResourceLoader {
    ResourceLoader EMBEDDED = new EmbeddedResourceLoader();

    InputStream open(Identifier id) throws IOException;

    default String readString(Identifier id) throws IOException {
        try (InputStream in = this.open(id)) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
