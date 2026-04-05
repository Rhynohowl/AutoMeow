package org.rhynohowl.automeow.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

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

    // [AutoMeow] Prefix
    public static MutableText badge() {
        boolean chroma = ModState.CHROMA_WANTED.get() && ChromaHelper.aaronChromaAvailable();

        MutableText name = Text.literal("AutoMeow")
                .styled(s -> s.withBold(false)
                        // Aaron-mods chroma shader, if not installed then default to pastel pink
                        .withColor(TextColor.fromRgb(chroma ? ChromaHelper.getChromaSentinel() : PASTEL_PINK)));

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
}