package me.jellysquid.mods.sodium.client.data.fingerprint;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import me.jellysquid.mods.sodium.client.util.FileUtil;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public record HashedFingerprint(
        @SerializedName("v")
        int version,

        @NotNull
        @SerializedName("s")
        String saltHex,

        @NotNull
        @SerializedName("u")
        String uuidHashHex,

        @NotNull
        @SerializedName("p")
        String pathHashHex,

        @SerializedName("t")
        long timestamp)
{
    public static final int CURRENT_VERSION = 1;

    public static @Nullable HashedFingerprint loadFromDisk() {
        Path path = getFilePath();

        if (!Files.exists(path)) {
            return null;
        }

        HashedFingerprint data;

        try {
            data = new Gson()
                    .fromJson(Files.readString(path), HashedFingerprint.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data file", e);
        }

        if (data.version() != CURRENT_VERSION) {
            return null;
        }

        return data;
    }

    public static void writeToDisk(@NotNull HashedFingerprint data) {
        Objects.requireNonNull(data);

        try {
            FileUtil.writeTextRobustly(new Gson()
                    .toJson(data), getFilePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save data file", e);
        }
    }

    private static Path getFilePath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("sodium-fingerprint.json");
    }
}
