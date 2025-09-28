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



import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class AutomeowClient implements ClientModInitializer {
    // Tunables
    public static volatile int  MY_MESSAGES_REQUIRED = 3;      // you must send 3 msgs between auto-replies
    public static volatile long QUIET_AFTER_SEND_MS  = 3500;   // mute echoes after we send (and after you type meow)
    private static final int PASTEL_PINK = 0xFFC0CB; // soft pastel pink (#ffc0cb)
    public static final AtomicBoolean CHROMA_WANTED = new AtomicBoolean(false); // user toggle for chroma
    private static final int AARON_CHROMA_SENTINEL = 0xAA5500;

    // State
    public static final AtomicBoolean ENABLED = new AtomicBoolean(true);
    private static final AtomicInteger myMsgsSinceReply = new AtomicInteger(MY_MESSAGES_REQUIRED); // start "ready"
    private static final AtomicLong    quietUntil       = new AtomicLong(0);
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
        }    else {
            saveConfig(); // write defaults
        }

        ENABLED.set(CONFIG.enabled);
        CHROMA_WANTED.set(CONFIG.chroma);
        APPEND_FACE.set(CONFIG.appendFace);

        // reply text: allow anything from disk; enforce "mer" only on user edits
        if (!setReplyText(CONFIG.replyText != null ? CONFIG.replyText : "meow", /*fromUser=*/false)) {
            REPLY_TEXT = "meow";
            }

        } catch (Exception ignored) {}
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
            Files.writeString(
                    CONFIG_PATH, GSON.toJson(CONFIG),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception ignored) {}
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
                        .withColor(TextColor.fromRgb( chroma ? AARON_CHROMA_SENTINEL : PASTEL_PINK)));

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

    // Custom reply text (what we send). Default: "meow".
    public static volatile String REPLY_TEXT = "meow";

    // Allow replies that contain "mer" anywhere (case-insensitive), e.g., merp/meraow/merps.
    private static final Pattern CAT_SOUND = Pattern.compile(
            "(mer|m+r+r+p+|m+r+o+w+|ny+a+~*)",
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
                                    .then(literal("on").executes(ctx -> { APPEND_FACE.set(true);  saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal(" :3 ON").formatted(Formatting.GREEN)));
                                        return 1; }))
                                    .then(literal("off").executes(ctx -> { APPEND_FACE.set(false); saveConfig();
                                        ctx.getSource().sendFeedback(badge().append(Text.literal(" :3 OFF").formatted(Formatting.RED)));
                                        return 1; }))
                                    .then(literal("toggle").executes(ctx -> {
                                        boolean nv = !APPEND_FACE.get(); APPEND_FACE.set(nv); saveConfig();
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

        long now = System.currentTimeMillis();
        if (now < quietUntil.get()) return; // ignore immediate echoes

        String raw = message.getString();
        if (raw == null || !MEOW.matcher(raw).find()) return;

        // Ignore our own lines (when servers echo them with a sender)
        UUID me = mc.getSession().getUuidOrNull();
        if (sender != null && me != null && me.equals(sender.getId())) return;

        if (myMsgsSinceReply.get() < MY_MESSAGES_REQUIRED) return;

        mc.execute(() -> {
            if (mc.player != null && mc.player.networkHandler != null) {
                skipNextOwnIncrement.set(true); // dont count only reply
                String out = AutomeowClient.REPLY_TEXT + (APPEND_FACE.get() ? " :3" : "");
                mc.player.networkHandler.sendChatMessage(out);
                quietUntil.set(System.currentTimeMillis() + QUIET_AFTER_SEND_MS);
                myMsgsSinceReply.set(0); // require 3 of OWN msgs before next auto-reply (I was checking all before)
            }
        });
    }
}
