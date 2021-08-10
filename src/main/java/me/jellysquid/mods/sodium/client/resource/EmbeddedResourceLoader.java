package me.jellysquid.mods.sodium.client.resource;

import net.minecraft.util.Identifier;

import java.io.InputStream;

public class EmbeddedResourceLoader implements ResourceLoader {
    @Override
    public InputStream open(Identifier id) {
        return EmbeddedResourceLoader.class.getResourceAsStream(String.format("/assets/%s/%s", id.getNamespace(), id.getPath()));
    }
}
