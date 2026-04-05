package org.rhynohowl.automeow.client;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class AutomeowClient implements ClientModInitializer {
    private static ClientWorld lastWorld = null;
    private static String lastWhisperFrom = null;

    // Match whole word "meow" (not case-sensitive)
    private static final Pattern MEOW = Pattern.compile("(^|\\W)(?:meow+|mrrp+|mrow+|mrraow+|mer+|nya+~*|purr+|bark+|woof+|wruff+)(\\W|$)", Pattern.CASE_INSENSITIVE);

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        UpdateChecker.checkForUpdateAsync();
        AutomeowCommands.register();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ModConfig.save());

        // Count outgoing messages YOU type; start a quiet window if you typed "meow"
        ClientSendMessageEvents.CHAT.register(msg -> {
            if (msg == null) return;

            // If you typed a cat-sound, play local cue for yourself (independent of auto-reply logic)
            if (MEOW.matcher(msg).find()) {
                ModState.startTimer();
                ModState.manualSendPending.set(true);
                MinecraftClient mcc = MinecraftClient.getInstance();
                if (mcc.player != null) {
                    mcc.execute(() -> CatCue.triggerCatCueAt(mcc.player));
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
                ModState.startTimer();
                ModState.manualSendPending.set(true);
                MinecraftClient mcc = MinecraftClient.getInstance();
                if (mcc.player != null) {
                    mcc.execute(() -> CatCue.triggerCatCueAt(mcc.player));
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
                refreshServerMode(client);

                for (HpChannel ch : HpChannel.values()) {
                    if (ch == HpChannel.IGNORE) continue;
                    ModState.counter(ch).set(ModState.MY_MESSAGES_REQUIRED);
                }
            }
        });
    }

    private static void refreshServerMode(MinecraftClient mc) {
        var servercheck = mc.getCurrentServerEntry();
        boolean hypixel = servercheck != null
                && servercheck.address != null
                && servercheck.address.toLowerCase(java.util.Locale.ROOT).contains("hypixel.net");
        ModState.ON_HYPIXEL.set(hypixel);
    }

    private void handleIncoming(Text message, GameProfile sender) {
        if (!ModState.ENABLED.get()) { ChatUtil.debug("blocked: disabled"); return; }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { ChatUtil.debug("blocked: no player"); return; }

        String raw = message.getString();

        if (raw == null) { ChatUtil.debug("blocked: raw null"); return; }

        String clean = ChatUtil.normaliseChat(raw);

        boolean isVanillaWhisper = false;

        var whisper_matcher = HpChannel.vanillaWhisperPattern().matcher(clean);
        if (whisper_matcher.find()) {
            lastWhisperFrom = whisper_matcher.group(1);
            isVanillaWhisper = true;
            ChatUtil.debug("vanilla pm from=" + lastWhisperFrom);
        }

        HpChannel ch = HpChannel.detect(clean);

        if (ch == HpChannel.IGNORE) return;

        // play SFX at play who meows & self
        if (MEOW.matcher(raw).find()) {
            PlayerEntity src = CatCue.resolveSender(mc, sender, raw);
            if (src != null) {
                UUID me = mc.getSession().getUuidOrNull();
                if (me == null || !me.equals(src.getUuid())) {
                    mc.execute(() -> CatCue.triggerCatCueAt(src));
                }
            }
        }

        long now = System.currentTimeMillis();

        UUID meUUID = mc.getSession().getUuidOrNull();
        String myName = mc.player.getGameProfile().name();

        if (now < ModState.echoUntil.get()) {
            ChatUtil.debug("blocked: echo quiet (" + (ModState.echoUntil.get() - now) + "ms left) chan =" + ch);
            return;
        }

        boolean isMe =
                (sender != null && meUUID != null && meUUID.equals(sender.id())) ||
                        (myName != null && clean.contains(myName + ":"));

        if (isMe) {
            if (ModState.skipNextOwnIncrement.getAndSet(false)) {
                ChatUtil.debug("own echo skipped");
                return;
            }

            if (MEOW.matcher(clean).find()) {
                if (ch != HpChannel.PARTY) ModState.counter(ch).set(0);
                ModState.startTimer();
                ChatUtil.debug("own meow, reset counter chan=" + ch);
                return;
            }
            if (ch != HpChannel.PARTY) {
                ModState.counter(ch).incrementAndGet();
                ChatUtil.debug("own msg -> " + ch + " = " + ModState.counter(ch).get());
            }
            return;
        }

        if (!MEOW.matcher(clean).find()) {
            ChatUtil.debug("ignored: no meow in: '" + clean + "'");
            return;
        }

        // ignore our own lines (when CHAT provides a sender)
        UUID me = mc.getSession().getUuidOrNull();
        if (sender != null && me != null && me.equals(sender.id())) {
            ChatUtil.debug("ignored: own message");
            return;
        }

        if (ch != HpChannel.PARTY) {
            int have = ModState.counter(ch).get();

            if (have < ModState.MY_MESSAGES_REQUIRED && now < ModState.cooldownUntil.get()){
                ChatUtil.debug("blocked: need msgs OR time (have=" + have + "/" + ModState.MY_MESSAGES_REQUIRED +
                        ", timeLeft=" + (ModState.cooldownUntil.get() - now) + "ms, chan=" + ch + ")");
                return;
            }
        } else {
            if (now < ModState.cooldownUntil.get()) {
                ChatUtil.debug("blocked: party cooldown (" + (ModState.cooldownUntil.get() - now) + "ms left)");
                return;
            }
        }

        if (ch == HpChannel.PM && !isVanillaWhisper) {
            if (!HpChannel.vanillaWhisperPattern().matcher(clean).find()) {
                String header = clean.split(":", 2)[0];

                java.util.regex.Matcher usernameFinder = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_]{3,16})\\b").matcher(header);
                String found = null;
                while (usernameFinder.find()) found = usernameFinder.group(1);
                if (found != null) lastWhisperFrom = found;
            }
        }

        ChatUtil.debug("TRIGGER: chan=" + ch + " raw='" + raw + "'");

        if (mc.player != null && mc.player.networkHandler != null) {
            ModState.skipNextOwnIncrement.set(true);

            String out = ModState.REPLY_TEXT + (ModState.APPEND_FACE.get() ? " :3" : "");

            if (!ModState.ON_HYPIXEL.get() && ch == HpChannel.PM) {
                String target = lastWhisperFrom;

                if (target == null || target.isBlank()) {
                    ChatUtil.debug("PM send blocked: lastWhisperFrom is null/blank. clean='" + clean + "'");
                    return;
                }

                String cmd = "msg " + target + " " + out;
                ChatUtil.debug("sending: /" + cmd);

                ModState.skipNextOwnIncrement.set(true);
                ModState.counter(ch).set(0);
                ModState.startTimer();
                mc.player.networkHandler.sendChatCommand(cmd);

                CatCue.triggerCatCueAt(mc.player);
                return;
            }

            if (ModState.ON_HYPIXEL.get()) {
                String cmd = switch (ch) {
                    case GUILD -> "gc " + out;
                    case PARTY -> "pc " + out;
                    case COOP -> "cc " + out;
                    case PM -> "r " + out;
                    case ALL -> "ac " + out;
                    case IGNORE -> throw new IllegalStateException("IGNORE should have returned early");
                };
                ChatUtil.debug("sending: " + cmd);
                ChatUtil.debug("detected channel: " + ch);
                ModState.skipNextOwnIncrement.set(true);
                ModState.startTimer();
                ModState.manualSendPending.set(false);
                final HpChannel finalCh = ch;
                java.util.concurrent.CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS)
                        .execute(() -> mc.execute(() -> {
                            if (ModState.manualSendPending.get()) {
                                ModState.manualSendPending.set(false);
                                ChatUtil.debug("blocked: manual send detected");
                                return;
                            }
                            if (mc.player != null && mc.player.networkHandler != null) {
                                mc.player.networkHandler.sendChatCommand(cmd);
                                CatCue.triggerCatCueAt(mc.player);
                                if (finalCh != HpChannel.PARTY) ModState.counter(finalCh).set(0);
                            }
                        }));
                return;
            } else {
                ChatUtil.debug("sending: " + out);
                ChatUtil.debug("detected channel: " + ch);
                ModState.skipNextOwnIncrement.set(true);
                ModState.startTimer();
                ModState.manualSendPending.set(false);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                mc.player.networkHandler.sendChatMessage(out);
            }

            CatCue.triggerCatCueAt(mc.player);
            if (ch != HpChannel.PARTY) ModState.counter(ch).set(0);
        } else {
            ChatUtil.debug("blocked: no networkHandler");
        }
    }
}