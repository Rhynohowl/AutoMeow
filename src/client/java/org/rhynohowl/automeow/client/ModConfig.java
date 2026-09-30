package org.rhynohowl.automeow.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class ModConfig {
    // Config state
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path CONFIG_PATH;

    // Toggles
    public static class Data {
        boolean enabled = true;
        boolean chroma = false;
        boolean gradient = false;
        String gradientPreset = ModState.CUSTOM_GRADIENT_PRESET;
        List<String> gradientColours = new ArrayList<>(ModState.DEFAULT_GRADIENT_COLOURS);
        String gradientDirection = ModState.DEFAULT_GRADIENT_DIRECTION;
        float gradientSpeed = ModState.DEFAULT_GRADIENT_SPEED;
        boolean gradientLoop = true;
        String replyText = "meow";
        boolean appendFace = false;
        boolean playSound = true;
        boolean heartsEffect = true;
        boolean channelPublic = true;
        boolean channelGuild = true;
        boolean channelParty = true;
        boolean channelCoop = true;
        boolean channelPm = true;
        boolean channelOfficer = true;
        boolean catfactParty = true;
        boolean catfactGuild = true;
        boolean catfactPM = true;
        boolean catfactSelfOnly = true;
        float baseVolume    = 0.8f;
        float volumeJitter  = 0.15f;
        float pitchJitter   = 0.10f;
        long totalReplies = 0L;
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
            ModState.GRADIENT_WANTED.set(CONFIG.gradient);
            ModState.setBadgeStyle(ModState.badgeStyle());
            ModState.setGradientPreset(CONFIG.gradientPreset);
            ModState.setGradientColours(CONFIG.gradientColours);
            ModState.setGradientDirection(CONFIG.gradientDirection);
            ModState.setGradientSpeed(CONFIG.gradientSpeed);
            ModState.GRADIENT_LOOP.set(CONFIG.gradientLoop);
            ModState.APPEND_FACE.set(CONFIG.appendFace);
            ModState.PLAY_SOUND.set(CONFIG.playSound);
            ModState.HEARTS_EFFECT.set(CONFIG.heartsEffect);
            ModState.setChannelEnabled(HpChannel.ALL, CONFIG.channelPublic);
            ModState.setChannelEnabled(HpChannel.GUILD, CONFIG.channelGuild);
            ModState.setChannelEnabled(HpChannel.PARTY, CONFIG.channelParty);
            ModState.setChannelEnabled(HpChannel.COOP, CONFIG.channelCoop);
            ModState.setChannelEnabled(HpChannel.PM, CONFIG.channelPm);
            ModState.setChannelEnabled(HpChannel.OFFICER, CONFIG.channelOfficer);
            ModState.CATFACT_PARTY.set(CONFIG.catfactParty);
            ModState.CATFACT_GUILD.set(CONFIG.catfactGuild);
            ModState.CATFACT_PM.set(CONFIG.catfactPM);
            ModState.CATFACT_SELF_ONLY.set(CONFIG.catfactSelfOnly);

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
            CONFIG.gradient = ModState.GRADIENT_WANTED.get();
            CONFIG.gradientPreset = ModState.GRADIENT_PRESET;
            CONFIG.gradientColours = new ArrayList<>(ModState.GRADIENT_COLOURS);
            CONFIG.gradientDirection = ModState.GRADIENT_DIRECTION;
            CONFIG.gradientSpeed = ModState.GRADIENT_SPEED;
            CONFIG.gradientLoop = ModState.GRADIENT_LOOP.get();
            CONFIG.replyText = ModState.REPLY_TEXT;
            CONFIG.appendFace = ModState.APPEND_FACE.get();
            CONFIG.playSound = ModState.PLAY_SOUND.get();
            CONFIG.heartsEffect = ModState.HEARTS_EFFECT.get();
            CONFIG.channelPublic = ModState.isChannelEnabled(HpChannel.ALL);
            CONFIG.channelGuild = ModState.isChannelEnabled(HpChannel.GUILD);
            CONFIG.channelParty = ModState.isChannelEnabled(HpChannel.PARTY);
            CONFIG.channelCoop = ModState.isChannelEnabled(HpChannel.COOP);
            CONFIG.channelPm = ModState.isChannelEnabled(HpChannel.PM);
            CONFIG.channelOfficer = ModState.isChannelEnabled(HpChannel.OFFICER);
            CONFIG.catfactParty = ModState.CATFACT_PARTY.get();
            CONFIG.catfactGuild = ModState.CATFACT_GUILD.get();
            CONFIG.catfactPM = ModState.CATFACT_PM.get();
            CONFIG.catfactSelfOnly = ModState.CATFACT_SELF_ONLY.get();
            Files.writeString(
                    CONFIG_PATH, GSON.toJson(CONFIG),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception ignored) {
        }
    }
}