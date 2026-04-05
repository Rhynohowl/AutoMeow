package org.rhynohowl.automeow.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ModConfig {
    // Config state
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path CONFIG_PATH;

    // Toggles
    public static class Data {
        boolean enabled = true;
        boolean chroma = false;
        String replyText = "meow";
        boolean appendFace = false;
        boolean playSound = true;
        boolean heartsEffect = true;
        float baseVolume    = 0.8f;
        float volumeJitter  = 0.15f;
        float pitchJitter   = 0.10f;
    }

    public static Data CONFIG = new Data();

    // Load on startup
    public static void load() {
        try {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
            Files.createDirectories(dir);
            CONFIG_PATH = dir.resolve("automeow.json");

            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                Data loaded = GSON.fromJson(json, Data.class);
                if (loaded != null) CONFIG = loaded;
            } else {
                save(); // write defaults
            }

            ModState.ENABLED.set(CONFIG.enabled);
            ModState.CHROMA_WANTED.set(CONFIG.chroma);
            ModState.APPEND_FACE.set(CONFIG.appendFace);
            ModState.PLAY_SOUND.set(CONFIG.playSound);
            ModState.HEARTS_EFFECT.set(CONFIG.heartsEffect);

            // reply text: allow anything from disk; enforce "mer" only on user edits
            if (!ModState.setReplyText(CONFIG.replyText != null ? CONFIG.replyText : "meow")) {
                ModState.REPLY_TEXT = ModState.DEFAULT_REPLY_TEXT;
            }

        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            if (CONFIG_PATH == null) {
                Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
                Files.createDirectories(dir);
                CONFIG_PATH = dir.resolve("automeow.json");
            }
            CONFIG.enabled = ModState.ENABLED.get();
            CONFIG.chroma = ModState.CHROMA_WANTED.get();
            CONFIG.replyText = ModState.REPLY_TEXT;
            CONFIG.appendFace = ModState.APPEND_FACE.get();
            CONFIG.playSound = ModState.PLAY_SOUND.get();
            CONFIG.heartsEffect = ModState.HEARTS_EFFECT.get();
            Files.writeString(
                    CONFIG_PATH, GSON.toJson(CONFIG),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception ignored) {
        }
    }
}