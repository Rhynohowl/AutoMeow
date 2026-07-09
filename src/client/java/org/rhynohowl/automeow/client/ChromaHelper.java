package org.rhynohowl.automeow.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.TextColor;

public final class ChromaHelper {
    private static final TextColor SKYHANNI_CHROMA = makeNamedColor(0xFFFFFF, "chroma");

    private static TextColor makeNamedColor(int rgb, String name) {
        try {
            var constructor = TextColor.class.getDeclaredConstructor(int.class, String.class);
            constructor.setAccessible(true);
            return (TextColor) constructor.newInstance(rgb, name);
        } catch (Throwable ignored) {
            return TextColor.fromRgb(rgb);
        }
    }

    public static boolean hasSkyhanni() {
        return FabricLoader.getInstance().isModLoaded("skyhanni");
    }

    public static TextColor getChromaTextColor() {
        return SKYHANNI_CHROMA;
    }
}