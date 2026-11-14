/*
 * tidal — proprietary source-viewing license
 * maintained by graphine studios
 * see LICENSE and CREDITS.md for licensing and attribution.
 *
 * provided here for inspection only.
 * this notice does not claim ownership of other contributors' material.
 * no general permission is granted by graphine studios to reuse, copy,
 * modify, redistribute or create derivative works from this file.
 */

package cc.tidal.tidal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class TidalAquaIntegration implements AutoCloseable {
    private static final boolean COSMETICS_ENABLED = false;
    public static final int SCHEMA_VERSION = 1;
    public static final String PROFILE_FILE_NAME = "profile.json";
    public static final String NOTIFICATIONS_FILE_NAME = "notifications.jsonl";
    public static final String ACKNOWLEDGEMENTS_FILE_NAME = "notifications-ack.json";
    private static final System.Logger LOGGER =
            System.getLogger(TidalAquaIntegration.class.getName());
    private static final long MAX_PROFILE_BYTES = 64 * 1024;
    private static final long MAX_EVENTS_BYTES = 256 * 1024;
    private static final Path DIRECTORY = applicationDataDirectory()
            .resolve("AquaClient").resolve("Tidal").resolve("bridge-v1");
    private static final Path PROFILE_FILE = DIRECTORY.resolve(PROFILE_FILE_NAME);
    private static final Path EVENTS_FILE = DIRECTORY.resolve(NOTIFICATIONS_FILE_NAME);
    private static final Path ACK_FILE = DIRECTORY.resolve(ACKNOWLEDGEMENTS_FILE_NAME);

    public static Path contractDirectory() {
        return DIRECTORY;
    }

    public record SkinMetadata(String assetId, String textureHash) {}
    public record CapeEntitlement(UUID accountUuid, String capeAssetId, long issuedAt,
                                  long expiresAt, String signature) {}
    public record CapeMetadata(String assetId, String assetHash,
                               CapeEntitlement entitlement) {}
    public record IdentityAssertion(UUID accountUuid, String status, String aquaUsername,
                                    long issuedAt, long expiresAt, String signature) {}
    public record ProfileState(int schemaVersion, long updatedAt, UUID accountUuid,
                               String profileName, SkinMetadata skin, CapeMetadata cape,
                               IdentityAssertion identityAssertion) {}
    public record NotificationEvent(UUID eventId, String type, UUID accountUuid,
                                   String title, String message, long createdAt) {}

    public enum AccountStatus {
        CONNECTED, NOT_SIGNED_IN, SESSION_EXPIRED, CONNECTION_UNAVAILABLE
    }

    @FunctionalInterface
    public interface IdentityAssertionVerifier {
        boolean verifySignature(IdentityAssertion assertion);
    }

    @FunctionalInterface
    public interface CapeEntitlementVerifier {
        boolean verify(CapeEntitlement entitlement, UUID expectedAccount, long nowMillis);
    }

    private static final CapeEntitlementVerifier DENY_CAPES =
            (entitlement, account, now) -> false;
    // No backend-issued public key is provisioned yet; unsigned/local assertions never authenticate.
    private static final IdentityAssertionVerifier DENY_IDENTITY_ASSERTIONS = assertion -> false;

    private final ConcurrentLinkedQueue<NotificationEvent> pending = new ConcurrentLinkedQueue<>();
    private final Set<UUID> queuedIds = new java.util.HashSet<>();
    private final Set<UUID> acknowledgedIds = new java.util.LinkedHashSet<>();
    private volatile ProfileState profile;
    private volatile boolean closed;
    private WatchService watchService;
    private Thread watcherThread;

    private TidalAquaIntegration() {}

    public static TidalAquaIntegration start() {
        TidalAquaIntegration integration = new TidalAquaIntegration();
        try {
            Files.createDirectories(DIRECTORY);
            integration.loadAcknowledgements();
            integration.reloadProfile();
            integration.reloadNotifications();
            integration.watchService = FileSystems.getDefault().newWatchService();
            DIRECTORY.register(integration.watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            integration.watcherThread = new Thread(integration::watch,
                    "Tidal-Aqua-Bridge");
            integration.watcherThread.setDaemon(true);
            integration.watcherThread.start();
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "Aqua Client integration could not start", exception);
            integration.close();
        }
        return integration;
    }

    public ProfileState profileFor(UUID activeAccount) {
        ProfileState current = profile;
        return activeAccount != null && current != null
                && activeAccount.equals(current.accountUuid()) ? current : null;
    }

    public void refresh() {
        try {
            reloadProfile();
        } catch (IOException exception) {
            profile = null;
            LOGGER.log(System.Logger.Level.WARNING,
                    "Could not refresh Aqua cosmetic state", exception);
        }
    }

    public AccountStatus accountStatus() {
        ProfileState current = profile;
        if (current == null) return AccountStatus.CONNECTION_UNAVAILABLE;
        IdentityAssertion assertion = current.identityAssertion();
        if (assertion == null || !DENY_IDENTITY_ASSERTIONS.verifySignature(assertion)) {
            return AccountStatus.CONNECTION_UNAVAILABLE;
        }
        long now = System.currentTimeMillis();
        if (!assertion.accountUuid().equals(current.accountUuid())
                || assertion.issuedAt() > now
                || assertion.expiresAt() <= assertion.issuedAt()
                || assertion.expiresAt() - assertion.issuedAt() > 600_000L) {
            return AccountStatus.CONNECTION_UNAVAILABLE;
        }
        if (assertion.status().equals("SIGNED_OUT")) return AccountStatus.NOT_SIGNED_IN;
        if (!assertion.status().equals("SIGNED_IN") || assertion.aquaUsername().isBlank()) {
            return AccountStatus.CONNECTION_UNAVAILABLE;
        }
        return assertion.expiresAt() <= System.currentTimeMillis()
                ? AccountStatus.SESSION_EXPIRED : AccountStatus.CONNECTED;
    }

    public String authenticatedUsername() {
        return accountStatus() == AccountStatus.CONNECTED
                ? profile.identityAssertion().aquaUsername() : null;
    }

    public boolean hasVerifiedCape(ProfileState state, long nowMillis) {
        if (!COSMETICS_ENABLED) return false;
        if (state == null || state.cape() == null || state.cape().entitlement() == null) {
            return false;
        }
        CapeEntitlement entitlement = state.cape().entitlement();
        return entitlement.accountUuid().equals(state.accountUuid())
                && entitlement.capeAssetId().equals(state.cape().assetId())
                && nowMillis >= entitlement.issuedAt() && nowMillis < entitlement.expiresAt()
                && DENY_CAPES.verify(entitlement, state.accountUuid(), nowMillis);
    }

    public List<NotificationEvent> drainNotifications(UUID activeAccount) {
        if (activeAccount == null) return List.of();
        List<NotificationEvent> result = new ArrayList<>();
        NotificationEvent event;
        while ((event = pending.poll()) != null) {
            synchronized (this) {
                queuedIds.remove(event.eventId());
                if (!acknowledgedIds.add(event.eventId())) continue;
                if (acknowledgedIds.size() > 1024) {
                    acknowledgedIds.remove(acknowledgedIds.iterator().next());
                }
            }
            if (event.accountUuid().equals(activeAccount)) result.add(event);
        }
        persistAcknowledgements();
        return result;
    }

    private void watch() {
        while (!closed) {
            try {
                WatchKey key = watchService.take();
                boolean profileChanged = false;
                boolean eventsChanged = false;
                for (var event : key.pollEvents()) {
                    if (!(event.context() instanceof Path changed)) continue;
                    profileChanged |= changed.equals(PROFILE_FILE.getFileName());
                    eventsChanged |= changed.equals(EVENTS_FILE.getFileName());
                }
                if (!key.reset()) break;
                if (profileChanged) reloadProfile();
                if (eventsChanged) reloadNotifications();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException | RuntimeException exception) {
                if (!closed) LOGGER.log(System.Logger.Level.WARNING,
                        "Aqua Client integration update was rejected", exception);
            }
        }
    }

    private void reloadProfile() throws IOException {
        if (!Files.isRegularFile(PROFILE_FILE, LinkOption.NOFOLLOW_LINKS)
                || Files.size(PROFILE_FILE) > MAX_PROFILE_BYTES) {
            profile = null;
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(PROFILE_FILE)).getAsJsonObject();
            if (integer(root, "schemaVersion") != SCHEMA_VERSION) {
                profile = null;
                return;
            }
            long updatedAt = number(root, "updatedAt");
            if (updatedAt <= 0 || updatedAt > System.currentTimeMillis() + 300_000L) {
                profile = null;
                return;
            }
            UUID account = UUID.fromString(text(root, "accountUuid", 36));
            String name = text(root, "profileName", 32);
            if (name.isBlank()) {
                profile = null;
                return;
            }
            SkinMetadata skin = null;
            CapeMetadata cape = null;
            if (COSMETICS_ENABLED) {
                skin = parseSkin(optionalObject(root, "skin"));
                cape = parseCape(optionalObject(root, "cape"));
            }
            IdentityAssertion assertion =
                    parseIdentityAssertion(optionalObject(root, "identityAssertion"));
            profile = new ProfileState(SCHEMA_VERSION, updatedAt, account, name, skin, cape, assertion);
        } catch (RuntimeException exception) {
            profile = null;
            LOGGER.log(System.Logger.Level.WARNING, "Invalid Aqua profile contract was rejected");
        }
    }

    private void reloadNotifications() throws IOException {
        if (!Files.isRegularFile(EVENTS_FILE, LinkOption.NOFOLLOW_LINKS)
                || Files.size(EVENTS_FILE) > MAX_EVENTS_BYTES) return;
        long now = System.currentTimeMillis();
        int count = 0;
        for (String line : Files.readAllLines(EVENTS_FILE)) {
            if (++count > 1000 || line.length() > 8192) break;
            try {
                JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                UUID id = UUID.fromString(text(json, "eventId", 36));
                UUID account = UUID.fromString(text(json, "accountUuid", 36));
                String type = text(json, "type", 32).toUpperCase(Locale.ROOT);
                if (!Set.of("DIRECT_MESSAGE", "FRIEND_REQUEST", "FRIEND_ACTIVITY",
                        "ACCOUNT_UPDATE").contains(type)
                        && !(COSMETICS_ENABLED && type.equals("COSMETIC_UPDATE"))) continue;
                String title = cleanText(text(json, "title", 120), 120);
                String message = cleanText(text(json, "message", 400), 400);
                long createdAt = number(json, "createdAt");
                if (title.isBlank() || message.isBlank() || createdAt <= 0
                        || createdAt > now + 300_000L || now - createdAt > 86_400_000L) continue;
                synchronized (this) {
                    if (acknowledgedIds.contains(id) || !queuedIds.add(id)) continue;
                }
                pending.offer(new NotificationEvent(id, type, account, title, message, createdAt));
            } catch (RuntimeException exception) {
                LOGGER.log(System.Logger.Level.DEBUG,
                        "Malformed Aqua notification event was ignored", exception);
            }
        }
    }

    private void loadAcknowledgements() throws IOException {
        if (!Files.isRegularFile(ACK_FILE, LinkOption.NOFOLLOW_LINKS)
                || Files.size(ACK_FILE) > 64 * 1024) return;
        try {
            JsonArray ids = JsonParser.parseString(Files.readString(ACK_FILE)).getAsJsonArray();
            for (JsonElement value : ids) {
                if (acknowledgedIds.size() == 1024) break;
                acknowledgedIds.add(UUID.fromString(value.getAsString()));
            }
        } catch (RuntimeException exception) {
            acknowledgedIds.clear();
            LOGGER.log(System.Logger.Level.WARNING, "Invalid Aqua notification cursor was ignored");
        }
    }

    private void persistAcknowledgements() {
        JsonArray ids = new JsonArray();
        synchronized (this) {
            for (UUID id : acknowledgedIds) ids.add(id.toString());
        }
        try {
            writeAtomically(ACK_FILE, ids.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Could not persist Aqua notification cursor", exception);
        }
    }

    private static SkinMetadata parseSkin(JsonObject json) {
        if (json == null) return null;
        return new SkinMetadata(assetId(text(json, "assetId", 128)),
                hash(text(json, "textureHash", 64)));
    }

    private static CapeMetadata parseCape(JsonObject json) {
        if (json == null) return null;
        CapeEntitlement entitlement = null;
        JsonObject signed = optionalObject(json, "entitlement");
        if (signed != null) {
            UUID account = UUID.fromString(text(signed, "accountUuid", 36));
            String capeId = assetId(text(signed, "capeAssetId", 128));
            long issuedAt = number(signed, "issuedAt");
            long expiresAt = number(signed, "expiresAt");
            String signature = text(signed, "signature", 256);
            byte[] decoded = Base64.getDecoder().decode(signature);
            if (decoded.length != 64 || expiresAt <= issuedAt) {
                throw new IllegalArgumentException("Invalid cape entitlement");
            }
            entitlement = new CapeEntitlement(account, capeId, issuedAt, expiresAt, signature);
        }
        return new CapeMetadata(assetId(text(json, "assetId", 128)),
                hash(text(json, "assetHash", 64)), entitlement);
    }

    private static IdentityAssertion parseIdentityAssertion(JsonObject json) {
        if (json == null) return null;
        UUID account = UUID.fromString(text(json, "accountUuid", 36));
        String status = text(json, "status", 16);
        if (!status.equals("SIGNED_IN") && !status.equals("SIGNED_OUT")) {
            throw new IllegalArgumentException("Invalid Aqua session status");
        }
        String username = status.equals("SIGNED_IN")
                ? text(json, "aquaUsername", 32) : "";
        if (status.equals("SIGNED_IN") && username.isBlank()) {
            throw new IllegalArgumentException("Invalid Aqua username");
        }
        long issuedAt = number(json, "issuedAt");
        long expiresAt = number(json, "expiresAt");
        String signature = text(json, "signature", 256);
        if (Base64.getDecoder().decode(signature).length != 64 || expiresAt <= issuedAt
                || expiresAt - issuedAt > 600_000L) {
            throw new IllegalArgumentException("Invalid Aqua identity assertion");
        }
        return new IdentityAssertion(account, status, username, issuedAt, expiresAt, signature);
    }

    private static JsonObject optionalObject(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsJsonObject();
    }

    private static String text(JsonObject object, String key, int maxLength) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        String text = value.getAsString();
        if (text.length() > maxLength) throw new IllegalArgumentException("Oversized field: " + key);
        return text;
    }

    private static int integer(JsonObject object, String key) {
        return object.get(key).getAsInt();
    }

    private static long number(JsonObject object, String key) {
        return object.get(key).getAsLong();
    }

    private static String assetId(String value) {
        if (!value.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new IllegalArgumentException("Invalid asset identifier");
        }
        return value;
    }

    private static String hash(String value) {
        if (!value.matches("[A-Fa-f0-9]{64}")) throw new IllegalArgumentException("Invalid asset hash");
        return value.toLowerCase(Locale.ROOT);
    }

    private static String cleanText(String value, int limit) {
        StringBuilder cleaned = new StringBuilder(Math.min(value.length(), limit));
        for (int i = 0; i < value.length() && cleaned.length() < limit; i++) {
            char character = value.charAt(i);
            if (!Character.isISOControl(character)) cleaned.append(character);
        }
        return cleaned.toString().trim();
    }

    private static Path applicationDataDirectory() {
        String appData = System.getenv("APPDATA");
        return appData == null || appData.isBlank()
                ? Path.of(System.getProperty("user.home"), ".config")
                : Path.of(appData);
    }

    private static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), "tidal-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void close() {
        closed = true;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException exception) {
                LOGGER.log(System.Logger.Level.WARNING, "Could not close Aqua file watcher", exception);
            }
        }
        if (watcherThread != null) watcherThread.interrupt();
    }
}
