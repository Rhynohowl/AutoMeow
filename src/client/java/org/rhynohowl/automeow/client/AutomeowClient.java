package org.rhynohowl.automeow.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Method;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.text.TextColor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.text.Style;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;


import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class AutomeowClient implements ClientModInitializer {
    // Tunables
    public static volatile int MY_MESSAGES_REQUIRED = 3;      // you must send 3 msgs between auto-replies
    public static volatile long QUIET_AFTER_SEND_MS = 3500;   // mute echoes after we send (and after you type meow)
    private static final int PASTEL_PINK = 0xFFC0CB; // soft pastel pink (#ffc0cb)
    public static final AtomicBoolean CHROMA_WANTED = new AtomicBoolean(false); // user toggle for chroma
    private static final int AARON_CHROMA_SENTINEL = 0xAA5500;
    public static final java.util.concurrent.atomic.AtomicBoolean PLAY_SOUND = new java.util.concurrent.atomic.AtomicBoolean(true);
    public static final java.util.concurrent.atomic.AtomicBoolean HEARTS_EFFECT = new java.util.concurrent.atomic.AtomicBoolean(true);
    public static final AtomicBoolean DEBUG = new AtomicBoolean(false);

    // Modrinth
    private static final String MODRINTH_SLUG = "automeow"; // your Modrinth project slug
    private static final int    UPDATE_HTTP_TIMEOUT_SEC = 6;

    // State
    public static final AtomicBoolean ENABLED = new AtomicBoolean(true);
    private static final AtomicBoolean skipNextOwnIncrement = new AtomicBoolean(false);
    public static final java.util.concurrent.atomic.AtomicBoolean APPEND_FACE = new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final AtomicLong echoUntil = new AtomicLong(0);     // hard anti-echo
    private static final AtomicLong cooldownUntil = new AtomicLong(0); // OR-gate timer

    private static final java.util.EnumMap<HpChannel, AtomicInteger> msgsSinceReply =
            new java.util.EnumMap<>(HpChannel.class);

    static {
        for (HpChannel ch : HpChannel.values()) {
            msgsSinceReply.put(ch, new AtomicInteger(MY_MESSAGES_REQUIRED)); // start "ready"
        }
    }

    private static AtomicInteger counter(HpChannel ch) {
        return msgsSinceReply.get(ch);
    }

    // Config state
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path CONFIG_PATH;

    private enum HpChannel { ALL, GUILD, PARTY, COOP, PM, IGNORE }

    // Toggles

    private static class Config {
        boolean enabled = true;
        boolean chroma = false;
        String replyText = "meow";
        boolean appendFace = false;
        boolean playSound = true;
        boolean heartsEffect = true;
        float baseVolume    = 0.8f;
        float volumeJitter  = 0.15f;
        float pitchJitter   = 0.10f;
    }

    private static final java.util.regex.Pattern LEADING_WORD =
            java.util.regex.Pattern.compile("^\\s*([A-Za-z]+(?:[-\\p{Pd}][A-Za-z]+)?)");

    private static String normaliseChat(String rawChat) {
        if (rawChat == null) return "";
        return rawChat
                .replaceAll("§.", "")
                .replace('\u00A0', ' ')
                .replaceAll("[\\u200B-\\u200F\\uFEFF\\u2060]", "")
                .trim();
    }

    public static final java.util.List<String> REPLY_PRESETS = java.util.List.of(
            "meow",
            "mrrp",
            "mrow",
            "mrraow",
            "mer",
            "nya",
            "purr",
            "bark",
            "woof",
            "wruff"
    );

    public static final String DEFAULT_REPLY_TEXT = REPLY_PRESETS.get(0);

    private static String canonicalPresetOrNull(String input) {
        if (input == null) return null;
        String trimed = input.trim();
        for (String option : REPLY_PRESETS) {
            if (option.equalsIgnoreCase(trimed)) return option;
        }
        return null;
    }

    private static void startTimer() {
        long timer = System.currentTimeMillis() + QUIET_AFTER_SEND_MS;
        echoUntil.set(timer);
        cooldownUntil.set(timer);
    }

    private static HpChannel detectHpChan(String raw) {
        if (raw == null) return HpChannel.ALL;
        String s = raw.replaceAll("§.", "");
        s = s.replaceAll("\\p{Pd}", "-");
        var m = LEADING_WORD.matcher(s);
        if (!m.find()) return HpChannel.ALL;
        String norm = m.group(1).toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z]", "");
        switch (norm) {
            case "party":   return HpChannel.PARTY;
            case "guild":   return HpChannel.GUILD;
            case "coop":    return HpChannel.COOP;
            case "from":    return HpChannel.PM;
            case "to":      return HpChannel.IGNORE;
            default:        return HpChannel.ALL;
        }
    }

    private static Config CONFIG = new Config();

    // Compare dotted numbers like "1.9.2" vs "1.10"
    private static int compareVersion(String a, String b) {
        String[] aa = a.split("\\D+"); // check non digits
        String[] bb = b.split("\\D+");
        int n = Math.max(aa.length, bb.length);
        for (int i = 0; i < n; i++) {
            int x = i < aa.length ? parseOrZero(aa[i]) : 0;
            int y = i < bb.length ? parseOrZero(bb[i]) : 0;
            if (x != y) return Integer.compare(x, y); // >0 if a>b | <0 if a<b
        }
        return 0;
    }
    private static int parseOrZero(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static void debug(String msg) {
        if (!DEBUG.get()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            if (mc.inGameHud != null) {
                mc.inGameHud.getChatHud().addMessage(
                        badge().append(Text.literal("[DBG] " + msg).formatted(Formatting.DARK_GRAY))
                );
            }
        });
    }

    private static String currentModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("automeow")              // your mod id from fabric.mod.json
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("0");
    }

    private static float clampf(float v, float min, float max) { // I HATE MATH MY FRIEND HELPED ME WITH THE MATH :C
        return Math.max(min, Math.min(max, v));
    }

    private static float jitterAround(float base, float jitterFraction, net.minecraft.util.math.random.Random r) {
        float j = (r.nextFloat() * 2f - 1f) * jitterFraction;
        return base * (1f + j);
    }

    private static boolean arrContainsString(JsonObject obj, String field, String wanted) {
        if (!obj.has(field) || !obj.get(field).isJsonArray()) return false;
        for (var el : obj.getAsJsonArray(field)) {
            if (el.isJsonPrimitive() && wanted.equalsIgnoreCase(el.getAsString())) return true;
        }
        return false;
    }

    private static void checkForUpdateAsync() {
        final String currentModVer = currentModVersion();
        final String currentMcVer = MinecraftClient.getInstance().getGameVersion();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(UPDATE_HTTP_TIMEOUT_SEC))
                .build();

        // “All versions” endpoint, we’ll pick the newest stable one.
        String url = "https://api.modrinth.com/v2/project/" + MODRINTH_SLUG + "/version";

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "AutoMeow/" + currentModVer + " (MC " + currentMcVer + "; Modrinth update check)")
                .timeout(java.time.Duration.ofSeconds(UPDATE_HTTP_TIMEOUT_SEC))
                .build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(ex -> null)
                .thenAccept(body -> {
                    if (body == null) return;
                    try {
                        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                        String bestVer = null;
                        String bestUrl = null;
                        java.time.Instant bestDate = java.time.Instant.EPOCH;

                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject v = arr.get(i).getAsJsonObject();
                            // Prefer stable releases
                            String type = v.has("version_type") ? v.get("version_type").getAsString() : "release";
                            if (!"release".equalsIgnoreCase(type)) continue;

                            if (!arrContainsString(v, "game_versions", currentMcVer)) continue;

                            String ver = v.get("version_number").getAsString();
                            java.time.Instant published = java.time.Instant.parse(v.get("date_published").getAsString());

                            if (bestVer == null || published.isAfter(bestDate)) {
                                bestVer = ver;
                                bestDate = published;
                                bestUrl = "https://modrinth.com/mod/" + MODRINTH_SLUG + "/version/" + ver;
                            }
                        }
                        if (bestVer == null) return;
                        if (compareVersion(bestVer, currentModVer) > 0) {
                            final String latest = bestVer;
                            final String dlUrl  = bestUrl;

                            // Clientside hyperlink
                            MinecraftClient mc = MinecraftClient.getInstance();
                            if (mc != null) {
                                Runnable  showUpdate = () -> mc.execute(() -> {
                                    var link = Text.literal("Download update")
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withColor(Formatting.BLUE)
                                                            .withUnderline(true)
                                                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(dlUrl)))           // ← open URL
                                                            .withHoverEvent(new HoverEvent.ShowText(Text.literal(dlUrl))) // ← tooltip
                                            );

                                    var msg = badge()
                                            .append(Text.literal(" Update available ").formatted(Formatting.YELLOW))
                                            .append(Text.literal("(MC " + currentMcVer + ", v" + currentModVer + " → v" + latest + ") ").formatted(Formatting.GRAY))
                                            .append(link);

                                    mc.inGameHud.getChatHud().addMessage(msg); // local only
                                });
                                java.util.concurrent.CompletableFuture
                                        .delayedExecutor(15, java.util.concurrent.TimeUnit.SECONDS)
                                        .execute(showUpdate);
                            }
                        }
                    } catch (Throwable ignored) { /* swallow quietly */ }
                });
    }



    // Load on startup
    private static void loadConfig() {
        try {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
            Files.createDirectories(dir);
            CONFIG_PATH = dir.resolve("automeow.json");

            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                Config loaded = GSON.fromJson(json, Config.class);
                if (loaded != null) CONFIG = loaded;
            } else {
                saveConfig(); // write defaults
            }

            ENABLED.set(CONFIG.enabled);
            CHROMA_WANTED.set(CONFIG.chroma);
            APPEND_FACE.set(CONFIG.appendFace);
            PLAY_SOUND.set(CONFIG.playSound);
            HEARTS_EFFECT.set(CONFIG.heartsEffect);

            // reply text: allow anything from disk; enforce "mer" only on user edits
            if (!setReplyText(CONFIG.replyText != null ? CONFIG.replyText : "meow")) {
                REPLY_TEXT = DEFAULT_REPLY_TEXT;
            }

        } catch (Exception ignored) {
        }
    }

    public static void saveConfig() {
        try {
            if (CONFIG_PATH == null) {
                Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
                Files.createDirectories(dir);
                CONFIG_PATH = dir.resolve("automeow.json");
            }
            CONFIG.enabled = ENABLED.get();
            CONFIG.chroma = CHROMA_WANTED.get();
            CONFIG.replyText = REPLY_TEXT;
            CONFIG.appendFace = APPEND_FACE.get();
            CONFIG.playSound = PLAY_SOUND.get();
            CONFIG.heartsEffect = HEARTS_EFFECT.get();
            Files.writeString(
                    CONFIG_PATH, GSON.toJson(CONFIG),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception ignored) {
        }
    }

    private static ClientWorld lastWorld = null;

    // Match whole word "meow" (not case-sensitive)
    private static final Pattern MEOW = Pattern.compile("(^|\\W)(?:meow+|mrrp+|mrow+|mrraow+|mer+|nya+~*|purr+|bark+|woof+|wruff+)(\\W|$)", Pattern.CASE_INSENSITIVE);

    private static boolean hasAaronMod() {
        FabricLoader fl = FabricLoader.getInstance();
        return fl.isModLoaded("aaron-mod") || fl.isModLoaded("azureaaron"); // cover both ids
    }


    public static boolean aaronChromaAvailable() {
        if (!hasAaronMod()) return false;
        try {
            Class<?> c = Class.forName("net.azureaaron.mod.features.ChromaText");
            Method m = c.getMethod("chromaColourAvailable");
            Object res = m.invoke(null);
            return res instanceof Boolean && (Boolean) res;
        } catch (Throwable ignored) {
            return false;
        }
    }

    // [AutoMeow] Prefix
    private static MutableText badge() {
        boolean chroma = CHROMA_WANTED.get() && aaronChromaAvailable();

        MutableText name = Text.literal("AutoMeow")
                .styled(s -> s.withBold(false)
                        // Aaron-mods chroma shader, if not installed then default to pastel pink
                        .withColor(TextColor.fromRgb(chroma ? AARON_CHROMA_SENTINEL : PASTEL_PINK)));

        return Text.literal("[").formatted(Formatting.GRAY)
                .append(name)
                .append(Text.literal("]").formatted(Formatting.GRAY))
                .append(Text.literal(" "));
    }

    private static MutableText statusLine(boolean enabled) {
        MutableText state = Text.literal(enabled ? "ON" : "OFF")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
        return badge()
                .append(state)
                .append(Text.literal(" | ALL " + counter(HpChannel.ALL).get() +"/" + MY_MESSAGES_REQUIRED).formatted(Formatting.GRAY))
                .append(Text.literal(" G " + counter(HpChannel.GUILD).get() + "/" + MY_MESSAGES_REQUIRED).formatted(Formatting.GRAY))
                .append(Text.literal(" C " + counter(HpChannel.COOP).get() + "/" + MY_MESSAGES_REQUIRED).formatted(Formatting.GRAY));
    }

    // Plays a cat meow and spawns heart particles around the given player (client-side only).
    private static void triggerCatCueAt(PlayerEntity target) {
        final MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || target == null) return;

        if (PLAY_SOUND.get()) {
            var r = mc.world.getRandom();
            float vol = clampf(jitterAround(CONFIG.baseVolume, CONFIG.volumeJitter, r), 0.0f, 2.0f);
            float pitch = clampf(jitterAround(CONFIG.baseVolume, CONFIG.pitchJitter, r), 0.5f, 2.0f);
            mc.world.playSound(target, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.PLAYERS, vol, pitch);
        }

        if (HEARTS_EFFECT.get()) {
            var r = mc.world.getRandom();
            for (int i = 0; i < 6; i++) {
                double dx = (r.nextDouble() - 0.5) * 0.6; //DISCAIMER!! FRIEND DID THIS SHIT ASWELL
                double dz = (r.nextDouble() - 0.5) * 0.6;
                double dy = 1.6 + r.nextDouble() * 0.4;

                mc.particleManager.addParticle(
                        ParticleTypes.HEART,
                        target.getX() + dx, target.getY() + dy, target.getZ() + dz,
                        0.0, 0.02, 0.0
                );
            }
        }
    }

    // Try to resolve who sent this chat line.
    // 1) If the event gives us a sender, use it.
    // 2) Otherwise, find any world player whose name appears in the raw chat text,
    // prefer the nearest one to reduce false positives.
    private static PlayerEntity resolveSender(MinecraftClient mc, GameProfile sender, String raw) {
        if (mc.world == null) return null;

        if (sender != null) {
            PlayerEntity p = mc.world.getPlayerByUuid(sender.id());
            if (p != null) return p;
        }
        if (raw == null || raw.isEmpty()) return null;

        String line = raw.replaceAll("§.", "").toLowerCase(java.util.Locale.ROOT);
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (PlayerEntity p : mc.world.getPlayers()) {
            String name = p.getGameProfile().name();
            if (name == null) continue;

            if (line.contains(name.toLowerCase(java.util.Locale.ROOT))) {
                double d = (mc.player != null) ? p.squaredDistanceTo(mc.player) : 0.0;
                if (best == null || d < bestDist) {
                    best = p;
                    bestDist = d;
                }
            }
        }
        return best;
    }


    // Custom reply text (what we send). Default: "meow".
    public static volatile String REPLY_TEXT = DEFAULT_REPLY_TEXT;

    // Allow replies that contain "mer" anywhere (case-insensitive), e.g., merp/meraow/merps/nya/~.
    private static final Pattern CAT_SOUND = Pattern.compile(
            "(^|\\W)(?:meow+|mrrp+|mrow+|mrraow+|mer+|nya+~*|purr+|bark+|woof+|wruff+)(\\W|$)",
            Pattern.CASE_INSENSITIVE
    );

    public static boolean setReplyText(String requestedText) {
        String canon = canonicalPresetOrNull(requestedText);
        if (canon == null) return false;
        REPLY_TEXT = canon;
        return true;
    }

    @Override
    public void onInitializeClient() {
        loadConfig();
        checkForUpdateAsync();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> saveConfig());
        // Count outgoing messages YOU type; start a quiet window if you typed "meow"
        ClientSendMessageEvents.CHAT.register(msg -> {
            if (msg == null) return;

            if (MEOW.matcher(msg).find()) {
                startTimer();
            }
            // If you typed a cat-sound, play local cue for yourself (independent of auto-reply logic)
            if (CAT_SOUND.matcher(msg).find()) {
                MinecraftClient mcc = MinecraftClient.getInstance();
                if (mcc.player != null) {
                    mcc.execute(() -> triggerCatCueAt(mcc.player));
                }
            }
        });
        ClientSendMessageEvents.COMMAND.register(cmd -> {
            if (cmd == null) return;

            String rawCmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;
            String head = rawCmd.split("\\s+", 2)[0].toLowerCase(java.util.Locale.ROOT);

            HpChannel ch = switch (head) {
                case "pc", "partychat" -> HpChannel.PARTY;
                case "gc", "guildchat" -> HpChannel.GUILD;
                case "cc", "coopchat"  -> HpChannel.COOP;
                case "ac", "allchat"   -> HpChannel.ALL;
                case "r" -> HpChannel.PM;
                default -> null;
            };
            if (ch == null) return;

            String payload = rawCmd.substring(head.length()).trim();
            if (payload.isEmpty()) return;

            if (MEOW.matcher(payload).find()) {
                startTimer();
                return;
            }

            if (CAT_SOUND.matcher(payload).find()) {
                MinecraftClient mcc = MinecraftClient.getInstance();
                if (mcc.player != null) {
                    mcc.execute(() -> triggerCatCueAt(mcc.player));
                }
            }
        });

        // React to incoming chat
        ClientReceiveMessageEvents.CHAT.register(
                (message, signedMessage, sender, params, ts) -> {
                    Text decorated = params.applyChatDecoration(message);
                    MinecraftClient.getInstance().execute(() -> handleIncoming(decorated, sender));
                }
        );
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            MinecraftClient.getInstance().execute(() -> handleIncoming(message, null));
        });

        // Reset counter on lobby/world change
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != lastWorld) {
                lastWorld = client.world;
                for (HpChannel ch : HpChannel.values()) {
                    counter(ch).set(MY_MESSAGES_REQUIRED);
                }
            }
        });

        // /automeow status & toggle, im not commenting all of this. work it out yourself
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
        {
            dispatcher.register(
                    literal("automeow")
                            .executes(ctx -> {
                                boolean on = ENABLED.get();
                                ctx.getSource().sendFeedback(statusLine(on));
                                return on ? 1 : 0;
                            })
                            .then(literal("toggle").executes(ctx -> {
                                boolean newValue = !ENABLED.get();
                                ENABLED.set(newValue);
                                saveConfig();
                                ctx.getSource().sendFeedback(statusLine(newValue));
                                return newValue ? 1 : 0;
                            }))
                            .then(literal("on").executes(ctx -> {
                                ENABLED.set(true);
                                saveConfig();
                                ctx.getSource().sendFeedback(statusLine(true));
                                return 1;
                            }))
                            .then(literal("off").executes(ctx -> {
                                ENABLED.set(false);
                                saveConfig();
                                ctx.getSource().sendFeedback(statusLine(false));
                                return 1;
                            }))
                            .then(literal("chroma").executes(ctx -> {
                                if (!hasAaronMod()) {
                                    ctx.getSource().sendFeedback(badge()
                                            .append(Text.literal("Aaron-mod not found").formatted(Formatting.RED)));
                                    return 0;
                                }
                                if (!aaronChromaAvailable()) {
                                    ctx.getSource().sendFeedback(badge()
                                            .append(Text.literal("Chroma pack is disabled").formatted(Formatting.YELLOW)));
                                    return 0;
                                }
                                boolean newValue = !CHROMA_WANTED.get();
                                CHROMA_WANTED.set(newValue);
                                saveConfig();
                                ctx.getSource().sendFeedback(badge()
                                        .append(Text.literal("Chroma " + (newValue ? "ON" : "OFF"))
                                                .formatted(newValue ? Formatting.GREEN : Formatting.RED)));
                                return newValue ? 1 : 0;
                            }))
                            .then(literal("debug").executes(ctx -> {
                                boolean newValue = !DEBUG.get();
                                DEBUG.set(newValue);
                                ctx.getSource().sendFeedback(
                                        badge().append(Text.literal("Debug " + (newValue ? "ON" : "OFF"))
                                                .formatted(newValue ? Formatting.GREEN : Formatting.RED))
                                );
                                return newValue ? 1 : 0;
                            }))
                            .then(literal("hearts")
                                    // `/automeow hearts` -> TOGGLE
                                    .executes(ctx -> {
                                        boolean newValue = !HEARTS_EFFECT.get();
                                        HEARTS_EFFECT.set(newValue);
                                        saveConfig();
                                        ctx.getSource().sendFeedback(
                                                badge().append(Text.literal("Hearts " + (newValue ? "ON" : "OFF"))
                                                        .formatted(newValue ? Formatting.GREEN : Formatting.RED)));
                                        return newValue ? 1 : 0;
                                    })
                                    .then(literal("on").executes(ctx -> {
                                        HEARTS_EFFECT.set(true); saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal("Hearts ON").formatted(Formatting.GREEN)));
                                        return 1;
                                    }))
                                    .then(literal("off").executes(ctx -> {
                                        HEARTS_EFFECT.set(false); saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal("Hearts OFF").formatted(Formatting.RED)));
                                        return 1;
                                    }))
                            )
                            .then(literal("sound")
                                    // `/automeow sound` -> TOGGLE
                                    .executes(ctx -> {
                                        boolean newValue = !PLAY_SOUND.get();
                                        PLAY_SOUND.set(newValue);
                                        saveConfig();
                                        ctx.getSource().sendFeedback(
                                                badge().append(Text.literal("Cat Sound " + (newValue ? "ON" : "OFF"))
                                                        .formatted(newValue ? Formatting.GREEN : Formatting.RED)));
                                        return newValue ? 1 : 0;
                                    })
                                    .then(literal("on").executes(ctx -> {
                                        PLAY_SOUND.set(true); saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal("Cat Sound ON").formatted(Formatting.GREEN)));
                                        return 1;
                                    }))
                                    .then(literal("off").executes(ctx -> {
                                        PLAY_SOUND.set(false); saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal("Cat Sound OFF").formatted(Formatting.RED)));
                                        return 1;
                                    }))
                            )
                            .then(literal("face")
                                    // `/automeow face` -> TOGGLE
                                    .executes(ctx -> {
                                        boolean newValue = !APPEND_FACE.get();
                                        APPEND_FACE.set(newValue);
                                        saveConfig();
                                        ctx.getSource().sendFeedback(
                                                badge().append(Text.literal("Cat Face " + (newValue ? "ON" : "OFF"))
                                                        .formatted(newValue ? Formatting.GREEN : Formatting.RED)));
                                        return newValue ? 1 : 0;
                                    })
                                    .then(literal("on").executes(ctx -> {
                                        APPEND_FACE.set(true); saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal("Cat Face ON").formatted(Formatting.GREEN)));
                                        return 1;
                                    }))
                                    .then(literal("off").executes(ctx -> {
                                        APPEND_FACE.set(false); saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal("Cat Face OFF").formatted(Formatting.RED)));
                                        return 1;
                                    }))
                            )
                            .then(literal("say")
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                            .argument("preset", StringArgumentType.greedyString())
                                            .suggests((ctx, suggestion) -> {
                                                for (String opt : REPLY_PRESETS) suggestion.suggest(opt);
                                                return suggestion.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                String wanted = StringArgumentType.getString(ctx, "preset");
                                                boolean ok = setReplyText(wanted);

                                                if (ok) {
                                                    saveConfig();
                                                    ctx.getSource().sendFeedback(
                                                            badge().append(Text.literal("reply preset set to \"" + REPLY_TEXT + "\"")
                                                                    .formatted(Formatting.GREEN))
                                                    );
                                                    return 1;
                                                }

                                                ctx.getSource().sendFeedback(
                                                        badge().append(Text.literal("Invalid preset. Choose one of: " + String.join(", ", REPLY_PRESETS))
                                                                .formatted(Formatting.RED))
                                                );
                                                return 0;
                                            })
                                    )
                            )
            );
        });
    }

    private void handleIncoming(Text message, GameProfile sender) {
        if (!ENABLED.get()) { debug("blocked: disabled"); return; }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { debug("blocked: no player"); return; }

        String raw = message.getString();
        String clean = normaliseChat(raw);

        if (raw == null) { debug("blocked: raw null"); return; }

        // play SFX at play who meows & self
        if (CAT_SOUND.matcher(raw).find()) {
            PlayerEntity src = resolveSender(mc, sender, raw);
            if (src != null) {
                UUID me = mc.getSession().getUuidOrNull();
                if (me == null || !me.equals(src.getUuid())) {
                    mc.execute(() -> triggerCatCueAt(src));
                }
            }
        }

        HpChannel ch = detectHpChan(clean);
        if (ch == HpChannel.IGNORE) return;

        long now = System.currentTimeMillis();

        UUID meUUID = mc.getSession().getUuidOrNull();
        String myName = mc.player.getGameProfile().name();

        boolean isMe =
                (sender != null && meUUID != null && meUUID.equals(sender.id())) ||
                        (myName != null && clean.contains(myName + ":"));

        if (isMe) {
            if (skipNextOwnIncrement.getAndSet(false)) {
                debug("own echo skipped");
                return;
            }

            if (MEOW.matcher(clean).find()) {
                if (ch != HpChannel.PARTY) counter(ch).set(0);
                debug("own meow, reset counter chan=" + ch);
                return;
            }
            if (ch != HpChannel.PARTY) {
                counter(ch).incrementAndGet();
                debug("own msg -> " + ch + " = " + counter(ch).get());
            }
            return;
        }

        if (now < echoUntil.get()) {
            debug("blocked: echo quiet (" + (echoUntil.get() - now) + "ms left) chan =" + ch);
            return;
        }

        if (!MEOW.matcher(clean).find()) {
            debug("ignored: no meow in: '" + clean + "'");
            return;
        }

        // ignore our own lines (when CHAT provides a sender)
        UUID me = mc.getSession().getUuidOrNull();
        if (sender != null && me != null && me.equals(sender.id())) {
            debug("ignored: own message");
            return;
        }

        if (ch != HpChannel.PARTY) {
            int have = counter(ch).get();

            if (have < MY_MESSAGES_REQUIRED && now < cooldownUntil.get()){
                debug("blocked: need msgs OR time (have=" + have + "/" + MY_MESSAGES_REQUIRED +
                        ", timeLeft=" + (cooldownUntil.get() - now) + "ms, chan=" + ch + ")");
                return;
            }
        } else {
            if (now < cooldownUntil.get()) {
                debug("blocked: party cooldown (" + (cooldownUntil.get() - now) + "ms left)");
                return;
            }
        }

        debug("TRIGGER: chan=" + ch + " raw='" + raw + "'");

        if (mc.player != null && mc.player.networkHandler != null) {
            skipNextOwnIncrement.set(true);

            String out = REPLY_TEXT + (APPEND_FACE.get() ? " :3" : "");
            String toSend = switch (ch) {
                case GUILD -> "/gc " + out;
                case PARTY -> "/pc " + out;
                case COOP  -> "/cc " + out;
                case PM -> "/r " + out;
                case ALL -> "/ac " + out;
                case IGNORE -> throw new IllegalStateException("IGNORE should have returned early");
            };

            debug("sending: " + toSend);

            skipNextOwnIncrement.set(true);

            startTimer();
            mc.player.networkHandler.sendChatCommand(toSend.substring(1));
            triggerCatCueAt(mc.player);
            if (ch != HpChannel.PARTY) counter(ch).set(0);
        } else {
            debug("blocked: no networkHandler");
        }
    }
}
