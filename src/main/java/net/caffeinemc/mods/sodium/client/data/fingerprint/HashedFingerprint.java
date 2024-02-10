package net.caffeinemc.mods.sodium.client.data.fingerprint;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import net.caffeinemc.mods.sodium.client.util.FileUtil;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * <p>The hashed fingerprint, which is serialized to disk in JSON format. The original measures cannot (easily) be
 * derived from these hashed values, and must be re-calculated by taking a new measure.</p>
 */
public record HashedFingerprint(
        /* The version of the data file, which is incremented each time incompatible changes are made. */
        @SerializedName("v")
        int version,

        /* The salt used to calculate the following fingerprint hashes, in hexadecimal format. */
        @NotNull
        @SerializedName("s")
        String saltHex,

        /* The hashed profile UUID in hexadecimal format. */
        @NotNull
        @SerializedName("u")
        String uuidHashHex,

        /* The hashed game directory path in hexadecimal format. */
        @NotNull
        @SerializedName("p")
        String pathHashHex,

        /* The epoch of when this fingerprint was first written to disk. This is also updated when the
        fingerprint changes. It can be used to calculate the time since first installation, since it will
        change when the mod is installed to a new computer. */
        @SerializedName("t")
        long timestamp)
{
    /**
     * The current version of the file format. This should be incremented when backwards-incompatible changes are made.
     */
    public static final int CURRENT_VERSION = 1;

    /**
     * Loads a previously saved fingerprint hash from the default location on disk.
     * @return The hashed fingerprint, or null if it doesn't exist on disk
     * @throws RuntimeException If the fingerprint on disk is invalid or an I/O exception occurs while reading it
     */
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

    /**
     * Writes the hashed fingerprint to disk, so that it can be loaded by {@link HashedFingerprint#loadFromDisk()} on
     * subsequent launches of the game.
     * @param data The hashed fingerprint to write to disk
     */
    public static void writeToDisk(@NotNull HashedFingerprint data) {
        Objects.requireNonNull(data);

        try {
            FileUtil.writeTextRobustly(new Gson()
                    .toJson(data), getFilePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save data file", e);
        }
    }

    /**
     * @return The path (relative to the game directory) which the hashed fingerprint is saved to
     */
    private static Path getFilePath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("sodium-fingerprint.json");
    }
}
