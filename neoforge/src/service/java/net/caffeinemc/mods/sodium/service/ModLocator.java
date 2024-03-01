package net.caffeinemc.mods.sodium.service;

import com.sun.jna.platform.unix.LibC;
import net.neoforged.fml.loading.moddiscovery.AbstractJarFileModLocator;
import org.lwjgl.system.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class ModLocator extends AbstractJarFileModLocator {
    @Override
    public Stream<Path> scanCandidates() {
        URL jarLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
        try {
            Path path = Path.of(jarLocation.toURI()).resolve("META-INF").resolve("jarjar");
            return Files.walk(path).filter(p -> p.getFileName().toString().startsWith("sodium") || p.getFileName().toString().equalsIgnoreCase("jarjar"));
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "sodium-locator";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {

    }
}