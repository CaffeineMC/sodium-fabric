package me.jellysquid.mods.sodium.client;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.api.SodiumGameOptionApi;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SodiumClientMod implements ClientModInitializer {
    private static SodiumGameOptions CONFIG;
    private static Logger LOGGER;

    @Override
    public void onInitializeClient() {

    }

    public static SodiumGameOptions options() {
        if (CONFIG == null) {
            CONFIG = loadConfig();
        }

        return CONFIG;
    }

    public static ImmutableList<OptionPage> getOptionPages(){
        List<OptionPage> factories = new ArrayList<>();
        FabricLoader.getInstance().getEntrypointContainers("sodium", SodiumGameOptionApi.class).forEach(entrypoint -> {
            SodiumGameOptionApi api = entrypoint.getEntrypoint();
            factories.add(api.getOptionPage());
            factories.addAll(api.getProvidedOptionPages());
        });
        return new ImmutableList.Builder<OptionPage>().addAll(factories).build();
    }

    public static Logger logger() {
        if (LOGGER == null) {
            LOGGER = LogManager.getLogger("Sodium");
        }

        return LOGGER;
    }

    private static SodiumGameOptions loadConfig() {
        SodiumGameOptions config = SodiumGameOptions.load(new File("config/sodium-options.json"));
        onConfigChanged(config);

        return config;
    }

    public static void onConfigChanged(SodiumGameOptions options) {
        UnsafeUtil.setEnabled(options.advanced.useMemoryIntrinsics);
    }
}
