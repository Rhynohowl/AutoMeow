package org.rhynohowl.automeow.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Style;
import java.util.List;

public final class MeowddingHelper {
    public static boolean hasMeowdding() {
        return FabricLoader.getInstance().isModLoaded("meowdding-lib");
    }

    public static List<String> presetNames() {
        if (!hasMeowdding()) return List.of();
        return MeowddingShader.presetNames();
    }

    public static Style applyGradient(Style style) {
        if (!hasMeowdding()) return style;
        return MeowddingShader.withGradient(style);
    }
}