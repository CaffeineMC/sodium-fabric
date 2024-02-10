package net.caffeinemc.mods.sodium.client.data.fingerprint;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;

/**
 * <p>The fingerprinting system provides a way to identify if the current installation of Sodium has been moved between
 * computers, such as when a mod pack distributes our mod. When the fingerprint has changed, some notices are shown
 * again to the user. This helps avoid a situation where mod pack authors accidentally distribute config files which
 * disable all the first-time messages our mod shows.</p>
 *
 * <p>Fingerprints are created from certain high-entropy sources (currently the profile UUID and game directory path),
 * which are very likely to be different between computers. These are called <i>measures</i>, and they are never stored
 * in plain-text format to the disk.</p>
 *
 * <p>Instead, we only store the SHA-512 digest of these measures. At game startup, the measures are calculated again,
 * and they are hashed using the previously stored salt value. If the hash digest is different from what was stored,
 * then the fingerprint is considered to have been *changed*, and the new fingerprint is written to disk with the
 * current timestamp.</p>
 */
public record FingerprintMeasure(
        /* The profile UUID. */
        @NotNull String uuid,
        /* The full and normalized path to the game directory. */
        @NotNull String path) {
    /**
     * The number of bytes which should be generated for each salt.
     */
    private static final int SALT_LENGTH = 64;

    /**
     * Creates a measure of the user's fingerprint (account UUID, game directory.) This measure can then be
     * hashed and compared to the existing fingerprint on disk.
     * @return The measure which was created from the current session, or null if there isn't enough entropy sources
     */
    public static @Nullable FingerprintMeasure create() {
        var uuid = Minecraft.getInstance().getUser().getProfileId();
        var path = FabricLoader.getInstance().getGameDir();

        if (uuid == null || path == null) {
            return null;
        }

        return new FingerprintMeasure(uuid.toString(), path.toAbsolutePath().toString());
    }

    /**
     * Creates a hash of this measure, with the current timestamp and a new salt. This can then be saved to disk
     * and later used to identify if the fingerprint has changed with {@link FingerprintMeasure#looselyMatches(HashedFingerprint)}.
     */
    public HashedFingerprint hashed() {
        var date = Instant.now();
        var salt = createSalt();

        var uuidHashHex = sha512(salt, this.uuid());
        var pathHashHex = sha512(salt, this.path());

        return new HashedFingerprint(HashedFingerprint.CURRENT_VERSION, salt, uuidHashHex, pathHashHex, date.getEpochSecond());
    }

    /**
     * Compares this measure against another hashed fingerprint that was produced by {@link FingerprintMeasure#hashed()}.
     * The hashes are re-calculated using the previously saved salt parameter. If too many measures no longer calculate
     * to the same values, the fingerprint is considered to be different, and this function returns <pre>false</pre>.
     *
     * @param hashed The previously hashed fingerprint
     * @return <pre>true</pre> if this measure is roughly the same as {@param hashed}, otherwise false
     */
    public boolean looselyMatches(HashedFingerprint hashed) {
        var uuidHashHex = sha512(hashed.saltHex(), this.uuid());
        var pathHashHex = sha512(hashed.saltHex(), this.path());

        return Objects.equals(uuidHashHex, hashed.uuidHashHex()) || Objects.equals(pathHashHex, hashed.pathHashHex());
    }

    /**
     * @param salt The salt value (represented as a hexadecimal string) to be prepended to the value
     * @param value The string data to be hashed (can be anything)
     * @return The SHA-512 digest (as a hexadecimal string) of the two values combined
     */
    private static String sha512(@NotNull String salt, @NotNull String value) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-512");
            md.update(Hex.decodeHex(salt));
            md.update(value.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            throw new RuntimeException("Failed to hash value", t);
        }

        return Hex.encodeHexString(md.digest());
    }

    /**
     * Generates a salt value from the system's cryptographically secure entropy source.
     * @return The generated salt, represented as a hexadecimal string
     */
    private static String createSalt() {
        var rng = new SecureRandom();

        var salt = new byte[SALT_LENGTH];
        rng.nextBytes(salt);

        return Hex.encodeHexString(salt);
    }
}
