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
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import java.time.Instant;
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


    // State
    public static final AtomicBoolean ENABLED = new AtomicBoolean(true);
    private static final AtomicInteger myMsgsSinceReply = new AtomicInteger(MY_MESSAGES_REQUIRED); // start "ready"
    private static final AtomicLong quietUntil = new AtomicLong(0);
    private static final AtomicBoolean skipNextOwnIncrement = new AtomicBoolean(false);
    public static final java.util.concurrent.atomic.AtomicBoolean APPEND_FACE = new java.util.concurrent.atomic.AtomicBoolean(false);


    // Config state
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path CONFIG_PATH;

    // Toggles

    private static class Config {
        boolean enabled = true;
        boolean chroma = false;
        String replyText = "meow";
        boolean appendFace = false;
        boolean playSound = true;
        boolean heartsEffect = true;
    }

    private static Config CONFIG = new Config();


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
            if (!setReplyText(CONFIG.replyText != null ? CONFIG.replyText : "meow", /*fromUser=*/false)) {
                REPLY_TEXT = "meow";
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
    private static final Pattern MEOW = Pattern.compile("\\bmeow\\b", Pattern.CASE_INSENSITIVE);

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

    private static MutableText statusLine(boolean enabled, int have, int need) {
        MutableText state = Text.literal(enabled ? "ON" : "OFF")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
        return badge().append(state);
    }

    // Plays a cat meow and spawns heart particles around the given player (client-side only).
    private static void triggerCatCueAt(PlayerEntity target) {
        final MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || target == null) return;

        if (PLAY_SOUND.get()) {
            mc.world.playSound(target, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.PLAYERS, 0.8f, 1.0f);
        }

        if (HEARTS_EFFECT.get()) {
            var r = mc.world.getRandom();
            for (int i = 0; i < 6; i++) {
                double dx = (r.nextDouble() - 0.5) * 0.6;
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
            PlayerEntity p = mc.world.getPlayerByUuid(sender.getId());
            if (p != null) return p;
        }
        if (raw == null || raw.isEmpty()) return null;

        String line = raw.toLowerCase(java.util.Locale.ROOT);
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (PlayerEntity p : mc.world.getPlayers()) {
            String name = p.getGameProfile().getName();
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
    public static volatile String REPLY_TEXT = "meow";

    // Allow replies that contain "mer" anywhere (case-insensitive), e.g., merp/meraow/merps/nya/~.
    private static final Pattern CAT_SOUND = Pattern.compile(
            "(m+e+o+w|mer|m+r+r+p+|m+r+o+w+|ny+a+~*)",
            Pattern.CASE_INSENSITIVE
    );

    // Validate & set. If fromUser=true we enforce the "mer" rule; default load can stay "meow".
    public static boolean setReplyText(String s, boolean fromUser) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty() || t.length() > 32) return false;
        if (fromUser && !CAT_SOUND.matcher(t).find()) return false;
        REPLY_TEXT = t;
        return true;
    }

    @Override
    public void onInitializeClient() {
        loadConfig();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> saveConfig());
        // Count outgoing messages YOU type; start a quiet window if you typed "meow"
        ClientSendMessageEvents.CHAT.register(msg -> {
            if (msg == null || msg.startsWith("/")) return;
            if (MEOW.matcher(msg).find()) {
                long now = System.currentTimeMillis();
                quietUntil.set(now + QUIET_AFTER_SEND_MS);
                myMsgsSinceReply.set(0); // after meow, require 3 of OWN msgs before next autoreply
            }
            if (!skipNextOwnIncrement.getAndSet(false)) {
                myMsgsSinceReply.incrementAndGet();
            }
            // If you typed a cat-sound, play local cue for yourself (independent of auto-reply logic)
            if (CAT_SOUND.matcher(msg).find()) {
                MinecraftClient mcc = MinecraftClient.getInstance();
                if (mcc.player != null) {
                    mcc.execute(() -> triggerCatCueAt(mcc.player));
                }
            }
        });

        // React to incoming chat
        ClientReceiveMessageEvents.CHAT.register(this::onChat);

        // Reset counter on lobby/world change
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != lastWorld) {
                lastWorld = client.world;
                myMsgsSinceReply.set(MY_MESSAGES_REQUIRED); // first meow in new lobby replies instantly
            }
        });

        // /automeow status & toggle, im not commenting all of this. work it out yourself
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("automeow")
                            .executes(ctx -> {
                                boolean on = ENABLED.get();
                                ctx.getSource().sendFeedback(statusLine(on, myMsgsSinceReply.get(), MY_MESSAGES_REQUIRED));
                                return on ? 1 : 0;
                            })
                            .then(literal("toggle").executes(ctx -> {
                                boolean newValue = !ENABLED.get();
                                ENABLED.set(newValue);
                                saveConfig();
                                ctx.getSource().sendFeedback(statusLine(newValue, myMsgsSinceReply.get(), MY_MESSAGES_REQUIRED));
                                return newValue ? 1 : 0;
                            }))
                            .then(literal("on").executes(ctx -> {
                                ENABLED.set(true);
                                saveConfig();
                                ctx.getSource().sendFeedback(statusLine(true, myMsgsSinceReply.get(), MY_MESSAGES_REQUIRED));
                                return 1;
                            }))
                            .then(literal("off").executes(ctx -> {
                                ENABLED.set(false);
                                saveConfig();
                                ctx.getSource().sendFeedback(statusLine(false, myMsgsSinceReply.get(), MY_MESSAGES_REQUIRED));
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
                            .then(literal("face")
                                    .executes(ctx -> {
                                        boolean on = APPEND_FACE.get();
                                        ctx.getSource().sendFeedback(
                                                badge().append(Text.literal(" :3 " + (on ? "ON" : "OFF"))
                                                        .formatted(on ? Formatting.GREEN : Formatting.RED)));
                                        return on ? 1 : 0;
                                    })
                                    .then(literal("on").executes(ctx -> {
                                        APPEND_FACE.set(true);
                                        saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal(" :3 ON").formatted(Formatting.GREEN)));
                                        return 1;
                                    }))
                                    .then(literal("off").executes(ctx -> {
                                        APPEND_FACE.set(false);
                                        saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal(" :3 OFF").formatted(Formatting.RED)));
                                        return 1;
                                    }))
                                    .then(literal("toggle").executes(ctx -> {
                                        boolean nv = !APPEND_FACE.get();
                                        APPEND_FACE.set(nv);
                                        saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal(" :3 " + (nv ? "ON" : "OFF"))
                                                .formatted(nv ? Formatting.GREEN : Formatting.RED)));
                                        return nv ? 1 : 0;
                                    }))
                            )
                            .then(literal("say")
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                            .argument("text", StringArgumentType.greedyString())
                                            .executes(ctx -> {
                                                String wanted = StringArgumentType.getString(ctx, "text");
                                                boolean ok = setReplyText(wanted, true);
                                                if (ok) {
                                                    saveConfig();
                                                    ctx.getSource().sendFeedback(
                                                            badge().append(Text.literal("reply set to \"" + REPLY_TEXT + "\"").formatted(Formatting.GREEN))
                                                    );
                                                    return 1;
                                                } else {
                                                    ctx.getSource().sendFeedback(
                                                            badge().append(Text.literal("reply must contain a cat sound: mer / mrrp / mrow / nya(a~)")
                                                                    .formatted(Formatting.RED))
                                                    );
                                                    return 0;
                                                }
                                            })
                                    )
                            )

            );
        });
    }

    private void onChat(
            Text message, SignedMessage signedMessage, GameProfile sender,
            MessageType.Parameters params, Instant receptionTimestamp
    ) {
        if (!ENABLED.get()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Get the plain string once
        String raw = message.getString();
        if (raw == null) return;

        // If this line looks like a cat sound, try to show SFX/VFX at the speaker
        if (CAT_SOUND.matcher(raw).find()) {
            PlayerEntity src = resolveSender(mc, sender, raw);
            if (src != null) {
                // don’t double-play for our own echo
                UUID me = mc.getSession().getUuidOrNull();
                if (me == null || !me.equals(src.getUuid())) {
                    mc.execute(() -> triggerCatCueAt(src));
                }
            }
        }

        long now = System.currentTimeMillis();
        if (now < quietUntil.get()) return;

        if (!MEOW.matcher(raw).find()) return;

        // ignore our own lines if the server echoes them with a sender
        UUID me = mc.getSession().getUuidOrNull();
        if (sender != null && me != null && me.equals(sender.getId())) return;

        if (myMsgsSinceReply.get() < MY_MESSAGES_REQUIRED) return;

        mc.execute(() -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                skipNextOwnIncrement.set(true);
                String out = REPLY_TEXT + (APPEND_FACE.get() ? " :3" : "");
                mc.player.networkHandler.sendChatMessage(out);

                // cue at us when we reply
                triggerCatCueAt(mc.player);

                quietUntil.set(System.currentTimeMillis() + QUIET_AFTER_SEND_MS);
                myMsgsSinceReply.set(0);
            }
        });
    }
}
