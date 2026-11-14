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

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayDeque;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TidalData {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static String coordinates = "";
    private static String direction = "";
    private static String armor = "";
    private static String armorDurability = "";
    private static String potions = "";
    private static String inGameClock = "";
    private static String realWorldClock = "";
    private static String fps = "0";
    private static String frameTime = "0.0 ms";
    private static String ping = "";
    private static String memory = "";
    private static String serverAddress = "";
    private static String playerCount = "";
    private static String health = "";
    private static String hunger = "";
    private static String saturation = "";
    private static String combinedHunger = "";
    private static String absorption = "";
    private static String yawPitch = "";
    private static String compass = "";
    private static String biome = "";
    private static String dimension = "";
    private static String resourcePacks = "";
    private static String xpLevel = "";
    private static String heldItem = "";
    private static String itemDurability = "";
    private static String totems = "";
    private static String arrows = "";
    private static String pearls = "";
    private static String goldenApples = "";
    private static String dayCounter = "";
    private static String movementSpeed = "";
    private static String sessionElapsed = "";
    private static Object previousLevel;
    private static long sessionStartedNanos;
    private static long previousPositionNanos;
    private static double previousX;
    private static double previousZ;
    private static double horizontalSpeed;
    private static final ArrayDeque<Long> LEFT_CLICKS = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT_CLICKS = new ArrayDeque<>();
    private static int cachedLeftCps = -1;
    private static int cachedRightCps = -1;
    private static String cachedCps = "L 0  ·  R 0";
    static final int KEY_W = 1;
    static final int KEY_A = 1 << 1;
    static final int KEY_S = 1 << 2;
    static final int KEY_D = 1 << 3;
    static final int KEY_SPACE = 1 << 4;
    static final int KEY_LMB = 1 << 5;
    static final int KEY_RMB = 1 << 6;
    private static int inputState;
    private static String potionTimers = "";
    private static long lastFrameSampleNanos;
    private static long lastFrameTimeFormatNanos;
    private static double smoothedFrameTimeNanos;

    private TidalData() {}

    public static void refresh(Minecraft client) {
        fps = Integer.toString(client.getFps());
        realWorldClock = LocalTime.now().format(CLOCK);
        long now = System.nanoTime();
        long usedMemory = RUNTIME.totalMemory() - RUNTIME.freeMemory();
        memory = String.format(Locale.ROOT, "%d / %d MB",
                usedMemory / 1_048_576L, RUNTIME.maxMemory() / 1_048_576L);
        if (client.level != null) {
            long dayTime = client.level.getOverworldClockTime() % 24000L;
            int hour = (int) ((dayTime / 1000L + 6L) % 24L);
            int minute = (int) (dayTime % 1000L * 60L / 1000L);
            inGameClock = String.format(java.util.Locale.ROOT, "%02d:%02d", hour, minute);
            dayCounter = Long.toString(client.level.getOverworldClockTime() / 24000L + 1);
        } else {
            inGameClock = dayCounter = "";
        }
        resourcePacks = client.getResourcePackRepository().getSelectedPacks().stream()
                .map(pack -> pack.getTitle().getString())
                .filter(name -> !name.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));
        LocalPlayer player = client.player;
        if (player == null) {
            LEFT_CLICKS.clear();
            RIGHT_CLICKS.clear();
            coordinates = direction = armor = armorDurability = potions = "";
            ping = serverAddress = playerCount = health = hunger = saturation = "";
            xpLevel = heldItem = itemDurability = totems = arrows = pearls = "";
            goldenApples = movementSpeed = "";
            combinedHunger = absorption = yawPitch = compass = biome = dimension = "";
            sessionElapsed = "";
            previousLevel = null;
            sessionStartedNanos = 0;
            previousPositionNanos = 0;
            horizontalSpeed = 0;
            inputState = 0;
            potionTimers = "";
            return;
        }
        if (sessionStartedNanos == 0 || previousLevel != client.level) {
            previousLevel = client.level;
            sessionStartedNanos = now;
            previousPositionNanos = now;
            previousX = player.getX();
            previousZ = player.getZ();
            horizontalSpeed = 0;
        } else if (previousPositionNanos != 0) {
            double elapsed = (now - previousPositionNanos) / 1_000_000_000.0;
            if (elapsed > 0) {
                horizontalSpeed = Math.hypot(player.getX() - previousX,
                        player.getZ() - previousZ) / elapsed;
            }
            previousPositionNanos = now;
            previousX = player.getX();
            previousZ = player.getZ();
        }
        coordinates = String.format(java.util.Locale.ROOT, "%.1f, %.1f, %.1f",
                player.getX(), player.getY(), player.getZ());
        direction = player.getDirection().getName();
        armor = Integer.toString(player.getArmorValue());
        potions = Integer.toString(player.getActiveEffects().size());
        potionTimers = player.getActiveEffects().stream()
                .limit(4)
                .map(effect -> {
                    String name = effect.getEffect().unwrapKey()
                            .map(key -> TidalUi.displayWords(key.identifier().getPath()))
                            .orElse("Effect");
                    int seconds = effect.getDuration() / 20;
                    String duration = effect.isInfiniteDuration() ? "∞"
                            : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                    return name + " " + duration;
                })
                .collect(java.util.stream.Collectors.joining(" · "));
        armorDurability = armorDurability(player);
        health = String.format(Locale.ROOT, "%.1f / %.1f",
                player.getHealth(), player.getMaxHealth());
        hunger = Integer.toString(player.getFoodData().getFoodLevel());
        saturation = String.format(Locale.ROOT, "%.1f", player.getFoodData().getSaturationLevel());
        combinedHunger = String.format(Locale.ROOT, "%d hunger, %.1f saturation",
                player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel());
        absorption = String.format(Locale.ROOT, "%.1f", player.getAbsorptionAmount());
        yawPitch = String.format(Locale.ROOT, "%.1f°, %.1f°", player.getYRot(), player.getXRot());
        compass = compassHeading(player.getYRot());
        if (client.level == null) {
            biome = dimension = "";
        } else {
            biome = client.level.getBiome(player.blockPosition()).unwrapKey()
                    .map(key -> TidalUi.displayWords(key.identifier().getPath()))
                    .orElse("Unknown");
            String dimensionId = client.level.dimension().identifier().getPath();
            dimension = switch (dimensionId) {
                case "overworld" -> "Overworld";
                case "the_nether" -> "The Nether";
                case "the_end" -> "The End";
                default -> TidalUi.displayWords(dimensionId);
            };
        }
        xpLevel = String.format(Locale.ROOT, "%d · %d%%",
                player.experienceLevel, Math.round(player.experienceProgress * 100));
        ItemStack held = player.getMainHandItem();
        heldItem = held.isEmpty() ? "Empty"
                : held.getHoverName().getString() + " ×" + held.getCount();
        itemDurability = held.isEmpty() || held.getMaxDamage() <= 0 ? ""
                : String.format(Locale.ROOT, "%d%%",
                Math.round(100.0f * (held.getMaxDamage() - held.getDamageValue())
                        / held.getMaxDamage()));
        totems = Integer.toString(countItem(player, Items.TOTEM_OF_UNDYING));
        arrows = Integer.toString(countItem(player, Items.ARROW)
                + countItem(player, Items.SPECTRAL_ARROW)
                + countItem(player, Items.TIPPED_ARROW));
        pearls = Integer.toString(countItem(player, Items.ENDER_PEARL));
        goldenApples = String.format(Locale.ROOT, "%d + %d",
                countItem(player, Items.GOLDEN_APPLE),
                countItem(player, Items.ENCHANTED_GOLDEN_APPLE));
        movementSpeed = String.format(Locale.ROOT, "%.2f b/s", horizontalSpeed);
        sessionElapsed = formatSessionTime(now);

        var server = client.getCurrentServer();
        serverAddress = server == null ? "" : server.ip;
        var connection = client.getConnection();
        var playerInfo = connection == null || server == null
                ? null : connection.getPlayerInfo(player.getUUID());
        ping = playerInfo == null ? "Offline" : playerInfo.getLatency() + " ms";
        playerCount = connection == null ? "1" : Integer.toString(
                Math.max(1, connection.getOnlinePlayers().size()));
    }

    static void recordFrame() {
        long now = System.nanoTime();
        if (lastFrameSampleNanos != 0) {
            long elapsed = now - lastFrameSampleNanos;
            if (elapsed > 0 && elapsed < 1_000_000_000L) {
                smoothedFrameTimeNanos = smoothedFrameTimeNanos == 0
                        ? elapsed : smoothedFrameTimeNanos * 0.9 + elapsed * 0.1;
                if (lastFrameTimeFormatNanos == 0
                        || now - lastFrameTimeFormatNanos >= 100_000_000L) {
                    frameTime = String.format(Locale.ROOT, "%.1f ms",
                            smoothedFrameTimeNanos / 1_000_000.0);
                    lastFrameTimeFormatNanos = now;
                }
            }
        }
        lastFrameSampleNanos = now;
    }

    public static String value(String id) {
        if (id.equals("cps")) {
            long now = System.nanoTime();
            trimClicks(LEFT_CLICKS, now);
            trimClicks(RIGHT_CLICKS, now);
            int left = LEFT_CLICKS.size();
            int right = RIGHT_CLICKS.size();
            if (left != cachedLeftCps || right != cachedRightCps) {
                cachedLeftCps = left;
                cachedRightCps = right;
                cachedCps = "L " + left + "  ·  R " + right;
            }
            return cachedCps;
        }
        return switch (id) {
            case "keystrokes" -> "Keystrokes";
            case "fps" -> fps;
            case "frame_time" -> frameTime;
            case "ping" -> ping;
            case "memory_usage" -> memory;
            case "server_address" -> serverAddress;
            case "player_count" -> playerCount;
            case "coordinates" -> coordinates;
            case "direction" -> direction;
            case "armor_status" -> armor;
            case "armor_durability" -> armorDurability;
            case "potion_effects" -> potions + " active";
            case "potion_timers" -> potionTimers;
            case "health" -> health;
            case "hunger" -> hunger;
            case "saturation" -> saturation;
            case "combined_hunger" -> combinedHunger;
            case "absorption" -> absorption;
            case "yaw_pitch" -> yawPitch;
            case "compass" -> compass;
            case "biome" -> biome;
            case "dimension" -> dimension;
            case "resource_pack_display" -> resourcePacks;
            case "xp_level" -> xpLevel;
            case "held_item" -> heldItem;
            case "item_durability" -> itemDurability;
            case "totem_counter" -> totems;
            case "arrow_counter" -> arrows;
            case "ender_pearl_counter" -> pearls;
            case "golden_apple_counter" -> goldenApples;
            case "movement_speed" -> movementSpeed;
            case "day_counter" -> dayCounter;
            case "session_timer" -> sessionElapsed;
            case "in_game_clock" -> inGameClock;
            case "real_world_clock" -> realWorldClock;
            default -> "";
        };
    }

    public static void refreshInput(Minecraft client) {
        if (client.player == null || client.gui.screen() != null) {
            inputState = 0;
            return;
        }
        var options = client.options;
        inputState = (options.keyUp.isDown() ? KEY_W : 0)
                | (options.keyLeft.isDown() ? KEY_A : 0)
                | (options.keyDown.isDown() ? KEY_S : 0)
                | (options.keyRight.isDown() ? KEY_D : 0)
                | (options.keyJump.isDown() ? KEY_SPACE : 0)
                | (client.mouseHandler.isLeftPressed() ? KEY_LMB : 0)
                | (client.mouseHandler.isRightPressed() ? KEY_RMB : 0);
    }

    static boolean inputDown(int key) {
        return (inputState & key) != 0;
    }

    static int clicksPerSecond(int button) {
        long now = System.nanoTime();
        trimClicks(button == 0 ? LEFT_CLICKS : RIGHT_CLICKS, now);
        return button == 0 ? LEFT_CLICKS.size() : RIGHT_CLICKS.size();
    }

    public static void recordMouseClick(int button) {
        long now = System.nanoTime();
        if (button == 0) LEFT_CLICKS.addLast(now);
        else if (button == 1) RIGHT_CLICKS.addLast(now);
        trimClicks(LEFT_CLICKS, now);
        trimClicks(RIGHT_CLICKS, now);
    }

    private static void trimClicks(ArrayDeque<Long> clicks, long now) {
        while (!clicks.isEmpty()
                && now - clicks.peekFirst() >= 1_000_000_000L) clicks.removeFirst();
    }

    private static String armorDurability(LocalPlayer player) {
        int lowestPercent = 100;
        boolean found = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || stack.getMaxDamage() <= 0) continue;
            found = true;
            int remaining = Math.round(100.0f
                    * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage());
            lowestPercent = Math.min(lowestPercent, remaining);
        }
        return found ? lowestPercent + "%" : "";
    }

    private static int countItem(LocalPlayer player,
                                 net.minecraft.world.item.Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) count += stack.getCount();
        }
        if (player.getOffhandItem().is(item)) count += player.getOffhandItem().getCount();
        return count;
    }

    private static String formatSessionTime(long now) {
        if (sessionStartedNanos == 0) return "";
        long seconds = (now - sessionStartedNanos) / 1_000_000_000L;
        long hours = seconds / 3600;
        long minutes = seconds / 60 % 60;
        return hours > 0 ? String.format(Locale.ROOT, "%d:%02d h", hours, minutes)
                : String.format(Locale.ROOT, "%d min", minutes);
    }

    private static String compassHeading(float yaw) {
        String[] headings = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        float degrees = (yaw + 180.0f) % 360.0f;
        if (degrees < 0) degrees += 360.0f;
        int index = Math.round(degrees / 22.5f) & 15;
        return String.format(Locale.ROOT, "%s %.0f°", headings[index], degrees);
    }
}
