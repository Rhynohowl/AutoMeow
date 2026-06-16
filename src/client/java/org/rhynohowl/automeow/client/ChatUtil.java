package org.rhynohowl.automeow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;

public final class ChatUtil {
    private static final int PASTEL_PINK = 0xFFC0CB; // soft pastel pink (#ffc0cb)

    public static String normaliseChat(String rawChat) {
        if (rawChat == null) return "";
        return rawChat
                .replaceAll("§.", "")
                .replace('\u00A0', ' ')
                .replaceAll("[\\u200B-\\u200F\\uFEFF\\u2060]", "")
                .trim();
    }

    public static void debug(String msg) {
        if (!ModState.DEBUG.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
                mc.gui.chatListener().handleSystemMessage(
                        badge().append(Component.literal("[DBG] " + msg).withStyle(ChatFormatting.DARK_GRAY)), false
                );
        });
    }

    // [AutoMeow] Prefix
    public static MutableComponent badge() {
        boolean chroma = ModState.CHROMA_WANTED.get() && ChromaHelper.aaronChromaAvailable();

        MutableComponent name = Component.literal("AutoMeow")
                .withStyle(s -> s.withBold(false)
                        // Aaron-mods chroma shader, if not installed then default to pastel pink
                        .withColor(TextColor.fromRgb(chroma ? ChromaHelper.getChromaSentinel() : PASTEL_PINK)));

        return Component.literal("[").withStyle(ChatFormatting.GRAY)
                .append(name)
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "));
    }

    public static MutableComponent statusLine(boolean enabled) {
        MutableComponent state = Component.literal(enabled ? "ON" : "OFF")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        return badge()
                .append(state)
                .append(Component.literal(" | ALL " + ModState.counter(HpChannel.ALL).get() +"/" + ModState.MY_MESSAGES_REQUIRED).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" G " + ModState.counter(HpChannel.GUILD).get() + "/" + ModState.MY_MESSAGES_REQUIRED).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" C " + ModState.counter(HpChannel.COOP).get() + "/" + ModState.MY_MESSAGES_REQUIRED).withStyle(ChatFormatting.GRAY));
    }
}