package org.rhynohowl.automeow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import java.util.Locale;

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

    public static Integer parseHexColour(String input) {
        if (input == null) return null;
        String hex = input.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.toLowerCase(Locale.ROOT).startsWith("0x")) {
            hex = hex.substring(2);
        }
        if (!hex.matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(hex, 16);
    }

    public static void debug(String msg) {
        if (!ModState.DEBUG.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            //? if <26.2 {
            mc.getChatListener().handleSystemMessage(
                    badge().append(Component.literal("[DBG] " + msg).withStyle(ChatFormatting.DARK_GRAY)), false
            );
            //?} else {
            /*mc.gui.chatListener().handleSystemMessage(
                    badge().append(Component.literal("[DBG] " + msg).withStyle(ChatFormatting.DARK_GRAY)), false
            );*/
            //?}
        });
    }

    // [AutoMeow] Prefix
    public static MutableComponent badge() {
        boolean gradient = ModState.GRADIENT_WANTED.get() && MeowddingHelper.hasMeowdding();
        boolean chroma = !gradient && ModState.CHROMA_WANTED.get() && ChromaHelper.hasSkyhanni();

        MutableComponent name = Component.literal("AutoMeow")
                .withStyle(s -> s.withBold(false)
                        .withColor(chroma ? ChromaHelper.getChromaTextColor() : TextColor.fromRgb(PASTEL_PINK)));
        if (gradient) {
            name = name.withStyle(s -> MeowddingHelper.applyGradient(s));
        }

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

    public static MutableComponent channelsStatusLine() {
        HpChannel[] shown = { HpChannel.ALL, HpChannel.GUILD, HpChannel.PARTY, HpChannel.COOP, HpChannel.PM };
        MutableComponent result = badge();
        for (int i = 0; i < shown.length; i++) {
            boolean on = ModState.isChannelEnabled(shown[i]);
            result = result
                    .append(Component.literal((i == 0 ? "" : " | ") + shown[i].displayName() + ": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        return result;
    }
}