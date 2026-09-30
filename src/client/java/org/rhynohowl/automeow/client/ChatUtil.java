package org.rhynohowl.automeow.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        mc.execute(() -> {
            //? if <26.1 {
            if (mc.inGameHud != null) {
                mc.inGameHud.getChatHud().addMessage(
                        badge().append(Text.literal("[DBG] " + msg).formatted(Formatting.DARK_GRAY))
                );
            }
            //?} else if <26.2 {
            /*mc.getChatListener().handleSystemMessage(
                    badge().append(Text.literal("[DBG] " + msg).formatted(Formatting.DARK_GRAY)), false
            );
            *///?} else {
            /*mc.gui.chatListener().handleSystemMessage(
                    badge().append(Text.literal("[DBG] " + msg).formatted(Formatting.DARK_GRAY)), false
            );*/
            //?}
        });
    }

    // [AutoMeow] Prefix
    public static MutableText badge() {
        boolean gradient = ModState.GRADIENT_WANTED.get() && MeowddingHelper.hasMeowdding();
        boolean chroma = !gradient && ModState.CHROMA_WANTED.get() && ChromaHelper.hasSkyhanni();

        MutableText name = Text.literal("AutoMeow")
                .styled(s -> s.withBold(false)
                        .withColor(chroma ? ChromaHelper.getChromaTextColor() : TextColor.fromRgb(PASTEL_PINK)));
        if (gradient) {
            name = name.styled(s -> MeowddingHelper.applyGradient(s));
        }

        return Text.literal("[").formatted(Formatting.GRAY)
                .append(name)
                .append(Text.literal("]").formatted(Formatting.GRAY))
                .append(Text.literal(" "));
    }

    public static MutableText statusLine(boolean enabled) {
        MutableText state = Text.literal(enabled ? "ON" : "OFF")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
        return badge()
                .append(state)
                .append(Text.literal(" | ALL " + ModState.counter(HpChannel.ALL).get() +"/" + ModState.MY_MESSAGES_REQUIRED).formatted(Formatting.GRAY))
                .append(Text.literal(" G " + ModState.counter(HpChannel.GUILD).get() + "/" + ModState.MY_MESSAGES_REQUIRED).formatted(Formatting.GRAY))
                .append(Text.literal(" C " + ModState.counter(HpChannel.COOP).get() + "/" + ModState.MY_MESSAGES_REQUIRED).formatted(Formatting.GRAY));
    }

    public static MutableText channelsStatusLine() {
        HpChannel[] shown = { HpChannel.ALL, HpChannel.GUILD, HpChannel.PARTY, HpChannel.COOP, HpChannel.PM };
        MutableText result = badge();
        for (int i = 0; i < shown.length; i++) {
            boolean on = ModState.isChannelEnabled(shown[i]);
            result = result
                    .append(Text.literal((i == 0 ? "" : " | ") + shown[i].displayName() + ": ").formatted(Formatting.GRAY))
                    .append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED));
        }
        return result;
    }
}