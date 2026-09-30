package org.rhynohowl.automeow.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.mojang.authlib.GameProfile;

public final class CatFact {
    private static final HttpClient catFactHttpClient = HttpClient.newHttpClient();

    // yes, seriously. the API says this shit ... evil evil evil >:C
    private static final List<String> BANNED_WORDS = List.of("hitler", "sex", "sexual", "sexually", "mating", "mate", "heat", "degrees,with", "blackie", "get cancer", "Main Coon");

    public static final Pattern PM_TARGET_PATTERN = Pattern.compile("(?i)(?:To|From)\\s+(?:\\[[^\\]]+\\]\\s*)*([A-Za-z0-9_]{3,16})\\s*:");

    public static void handle(MinecraftClient mc, HpChannel ch, String clean, GameProfile sender, String lastWhisperFrom) {
        String target = null;
        if (ch == HpChannel.PM) {
            String fallback = (sender != null && !sender.name().isBlank())
                    ? sender.name()
                    : (lastWhisperFrom != null ? lastWhisperFrom : "");
            target = parsePmTarget(clean, fallback);
            if (target == null || target.isBlank()) {
                return;
            }
        }

        String commandPrefix = switch (ch) {
            case PARTY -> "pc ";
            case GUILD -> "gc ";
            case PM -> "msg " + target + " ";
            default -> null;
        };

        fetchCatFact(fact -> {
            CompletableFuture.delayedExecutor(600, TimeUnit.MILLISECONDS)
                    .execute(() -> mc.execute(() -> {
                        if (mc.player != null && mc.player.networkHandler != null) {
                            mc.player.networkHandler.sendChatCommand(commandPrefix + fact);
                        }
                    }));
        });
    }

    public static boolean shouldHandle(HpChannel ch, String clean, boolean isMe) {
        if (!clean.contains("!catfact")) return false;
        if (ModState.CATFACT_SELF_ONLY.get() && !isMe) return false;
        return (ch == HpChannel.PARTY && ModState.CATFACT_PARTY.get())
                || (ch == HpChannel.GUILD && ModState.CATFACT_GUILD.get())
                || (ch == HpChannel.PM && ModState.CATFACT_PM.get());
    }

    private static String parsePmTarget(String clean, String fallbackName) {
        if (clean == null) return fallbackName;
        Matcher matcher = PM_TARGET_PATTERN.matcher(clean);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return fallbackName;
    }

    private static void fetchCatFact(Consumer<String> onFactRetrieved) {
        retryFetchCatFact(onFactRetrieved, 0);
    }

    private static void retryFetchCatFact(Consumer<String> onFactRetrieved, int currentRetryCount) {
        if (currentRetryCount >= 5) {
            return;
        }

        HttpRequest apiRequest = HttpRequest.newBuilder(URI.create("https://catfact.ninja/fact?max_length=200"))
                .timeout(Duration.ofSeconds(5))
                .build();
        catFactHttpClient.sendAsync(apiRequest, HttpResponse.BodyHandlers.ofString())
                .thenAccept(apiHttpResponse -> {
                    if (apiHttpResponse.statusCode() == 200) {
                        JsonObject parsedJsonResponse = JsonParser.parseString(apiHttpResponse.body()).getAsJsonObject();
                        String factText = parsedJsonResponse.get("fact").getAsString();

                        String lowerFactText = factText.toLowerCase(Locale.ROOT);
                        boolean hasBannedWord = BANNED_WORDS.stream().anyMatch(lowerFactText::contains);

                        if (hasBannedWord) {
                            retryFetchCatFact(onFactRetrieved, currentRetryCount + 1);
                            return;
                        }
                        ChatUtil.debug("attempted fact: " + factText);
                        System.out.println("attempted fact: " + factText);
                        onFactRetrieved.accept(factText);
                    }
                })
                .exceptionally(networkException -> null);
    }
}