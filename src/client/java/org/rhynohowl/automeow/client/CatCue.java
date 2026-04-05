package org.rhynohowl.automeow.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class CatCue {
    // Plays a cat meow and spawns heart particles around the given player (client-side only).
    public static void triggerCatCueAt(PlayerEntity target) {
        final MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || target == null) return;

        if (ModState.PLAY_SOUND.get()) {
            var random = mc.world.getRandom();
            float vol = clampf(jitterAround(ModConfig.CONFIG.baseVolume, ModConfig.CONFIG.volumeJitter, random), 0.0f, 2.0f);
            float pitch = clampf(jitterAround(ModConfig.CONFIG.baseVolume, ModConfig.CONFIG.pitchJitter, random), 0.5f, 2.0f);
            mc.world.playSound(target, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.PLAYERS, vol, pitch);
        }

        if (ModState.HEARTS_EFFECT.get()) {
            var random = mc.world.getRandom();
            for (int particleIndex = 0; particleIndex < 6; particleIndex++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.6; //DISCLAIMER!! FRIEND DID THIS SHIT AS WELL
                double offsetZ = (random.nextDouble() - 0.5) * 0.6;
                double offsetY = 1.6 + random.nextDouble() * 0.4;

                mc.particleManager.addParticle(
                        ParticleTypes.HEART,
                        target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ,
                        0.0, 0.02, 0.0
                );
            }
        }
    }

    // Try to resolve who sent this chat line.
    // 1) If the event gives us a sender, use it.
    // 2) Otherwise, find any world player whose name appears in the raw chat text,
    // prefer the nearest one to reduce false positives.
    public static PlayerEntity resolveSender(MinecraftClient mc, GameProfile sender, String raw) {
        if (mc.world == null) return null;

        if (sender != null) {
            PlayerEntity player = mc.world.getPlayerByUuid(sender.id());
            if (player != null) return player;
        }
        if (raw == null || raw.isEmpty()) return null;

        String cleanedLine = raw.replaceAll("§.", "").toLowerCase(java.util.Locale.ROOT);
        PlayerEntity bestMatch = null;
        double bestDistance = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            String playerName = player.getGameProfile().name();
            if (playerName == null) continue;

            if (cleanedLine.contains(playerName.toLowerCase(java.util.Locale.ROOT))) {
                double distance = (mc.player != null) ? player.squaredDistanceTo(mc.player) : 0.0;
                if (bestMatch == null || distance < bestDistance) {
                    bestMatch = player;
                    bestDistance = distance;
                }
            }
        }
        return bestMatch;
    }

    private static float clampf(float value, float min, float max) { // I HATE MATH MY FRIEND HELPED ME WITH THE MATH :C
        return Math.max(min, Math.min(max, value));
    }

    private static float jitterAround(float base, float jitterFraction, net.minecraft.util.math.random.Random random) {
        float jitter = (random.nextFloat() * 2f - 1f) * jitterFraction;
        return base * (1f + jitter);
    }
}