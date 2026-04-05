package org.rhynohowl.automeow.client;

import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Method;

public final class ChromaHelper {
    private static final int AARON_CHROMA_SENTINEL = 0xAA5500;

    public static boolean hasAaronMod() {
        FabricLoader fabricLoader = FabricLoader.getInstance();
        return fabricLoader.isModLoaded("aaron-mod") || fabricLoader.isModLoaded("azureaaron"); // cover both ids
    }

    public static boolean aaronChromaAvailable() {
        if (!hasAaronMod()) return false;
        try {
            Class<?> chromaTextClass = Class.forName("net.azureaaron.mod.features.ChromaText");
            Method chromaAvailableMethod = chromaTextClass.getMethod("chromaColourAvailable");
            Object result = chromaAvailableMethod.invoke(null);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getChromaSentinel() {
        return AARON_CHROMA_SENTINEL;
    }
}