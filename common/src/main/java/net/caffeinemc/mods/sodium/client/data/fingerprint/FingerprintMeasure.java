package net.caffeinemc.mods.sodium.client.data.fingerprint;

import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.minecraft.client.Minecraft;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;

public record FingerprintMeasure(@NotNull String uuid, @NotNull String path) {
    private static final int SALT_LENGTH = 64;

    public static @Nullable FingerprintMeasure create() {
        var uuid = Minecraft.getInstance().getUser().getProfileId();
        var path = PlatformRuntimeInformation.getInstance().getGameDirectory();

        if (uuid == null || path == null) {
            return null;
        }

        return new FingerprintMeasure(uuid.toString(), path.toAbsolutePath().toString());
    }

    public HashedFingerprint hashed() {
        var date = Instant.now();
        var salt = createSalt();

        var uuidHashHex = sha512(salt, this.uuid());
        var pathHashHex = sha512(salt, this.path());

        return new HashedFingerprint(HashedFingerprint.CURRENT_VERSION, salt, uuidHashHex, pathHashHex, date.getEpochSecond());
    }

    public boolean looselyMatches(HashedFingerprint hashed) {
        var uuidHashHex = sha512(hashed.saltHex(), this.uuid());
        var pathHashHex = sha512(hashed.saltHex(), this.path());

        return Objects.equals(uuidHashHex, hashed.uuidHashHex()) || Objects.equals(pathHashHex, hashed.pathHashHex());
    }

    private static String sha512(@NotNull String salt, @NotNull String message) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-512");
            md.update(Hex.decodeHex(salt));
            md.update(message.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            throw new RuntimeException("Failed to hash value", t);
        }

        return Hex.encodeHexString(md.digest());
    }

    private static String createSalt() {
        var rng = new SecureRandom();

        var salt = new byte[SALT_LENGTH];
        rng.nextBytes(salt);

        return Hex.encodeHexString(salt);
    }
}
