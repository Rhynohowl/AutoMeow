package org.rhynohowl.automeow.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Style;
import java.util.List;

public final class MeowddingHelper {
    public static boolean hasMeowdding() {
        //? if >=26.1 {
        /*return FabricLoader.getInstance().isModLoaded("meowdding-lib");
        *///?} else {
        return false;
        //?}
    }

    public static boolean supportedVersion() {
        //? if >=26.1 {
        /*return true;
        *///?} else {
        return false;
        //?}
    }

    public static List<String> presetNames() {
        //? if >=26.1 {
        /*if (!hasMeowdding()) return List.of();
        return MeowddingShader.presetNames();
        *///?} else {
        return List.of();
        //?}
    }

    public static Style applyGradient(Style style) {
        //? if >=26.1 {
        /*if (!hasMeowdding()) return style;
        return MeowddingShader.withGradient(style);
        *///?} else {
        return style;
        //?}
    }
}