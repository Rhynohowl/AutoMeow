package org.rhynohowl.automeow.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class UpdateChecker {
    // Modrinth
    private static final String MODRINTH_SLUG = "automeow"; // Modrinth project slug
    private static final int    UPDATE_HTTP_TIMEOUT_SEC = 6;

    // Compare dotted numbers like "1.9.2" vs "1.10"
    private static int compareVersion(String versionA, String versionB) {
        String[] versionAParts = versionA.split("\\D+"); // check non digits
        String[] versionBParts = versionB.split("\\D+");
        int maxLength = Math.max(versionAParts.length, versionBParts.length);
        for (int partIndex = 0; partIndex < maxLength; partIndex++) {
            int partA = partIndex < versionAParts.length ? parseOrZero(versionAParts[partIndex]) : 0;
            int partB = partIndex < versionBParts.length ? parseOrZero(versionBParts[partIndex]) : 0;
            if (partA != partB) return Integer.compare(partA, partB); // >0 if a>b | <0 if a<b
        }
        return 0;
    }

    private static int parseOrZero(String input) {
        try {
            return Integer.parseInt(input);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String currentModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("automeow")              // your mod id from fabric.mod.json
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0");
    }

    private static boolean arrContainsString(JsonObject obj, String field, String wanted) {
        if (!obj.has(field) || !obj.get(field).isJsonArray()) return false;
        for (var element : obj.getAsJsonArray(field)) {
            if (element.isJsonPrimitive() && wanted.equalsIgnoreCase(element.getAsString())) return true;
        }
        return false;
    }

    public static void checkForUpdateAsync() {
        final String currentModVer = currentModVersion();
        final String currentMcVer = MinecraftClient.getInstance().getGameVersion();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(UPDATE_HTTP_TIMEOUT_SEC))
                .build();

        // "All versions" endpoint, we'll pick the newest stable one.
        String url = "https://api.modrinth.com/v2/project/" + MODRINTH_SLUG + "/version";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "AutoMeow/" + currentModVer + " (MC " + currentMcVer + "; Modrinth update check)")
                .timeout(java.time.Duration.ofSeconds(UPDATE_HTTP_TIMEOUT_SEC))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(ex -> null)
                .thenAccept(body -> {
                    if (body == null) return;
                    try {
                        JsonArray versionsArray = JsonParser.parseString(body).getAsJsonArray();
                        String bestVer = null;
                        String bestUrl = null;
                        java.time.Instant bestDate = java.time.Instant.EPOCH;

                        for (int versionIndex = 0; versionIndex < versionsArray.size(); versionIndex++) {
                            JsonObject versionObject = versionsArray.get(versionIndex).getAsJsonObject();
                            // Prefer stable releases
                            String releaseType = versionObject.has("version_type") ? versionObject.get("version_type").getAsString() : "release";
                            if (!"release".equalsIgnoreCase(releaseType)) continue;

                            if (!arrContainsString(versionObject, "game_versions", currentMcVer)) continue;

                            String versionNumber = versionObject.get("version_number").getAsString();
                            java.time.Instant publishedDate = java.time.Instant.parse(versionObject.get("date_published").getAsString());

                            if (bestVer == null || publishedDate.isAfter(bestDate)) {
                                bestVer = versionNumber;
                                bestDate = publishedDate;
                                bestUrl = "https://modrinth.com/mod/" + MODRINTH_SLUG + "/version/" + versionNumber;
                            }
                        }
                        if (bestVer == null) return;
                        if (compareVersion(bestVer, currentModVer) > 0) {
                            final String latest = bestVer;
                            final String dlUrl  = bestUrl;

                            // Clientside hyperlink
                            MinecraftClient mc = MinecraftClient.getInstance();
                            if (mc != null) {
                                Runnable  showUpdate = () -> mc.execute(() -> {
                                    var link = Text.literal("Download update")
                                            .setStyle(
                                                    Style.EMPTY
                                                            .withColor(Formatting.BLUE)
                                                            .withUnderline(true)
                                                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(dlUrl)))           // ← open URL
                                                            .withHoverEvent(new HoverEvent.ShowText(Text.literal(dlUrl))) // ← tooltip
                                            );

                                    var msg = ChatUtil.badge()
                                            .append(Text.literal(" Update available ").formatted(Formatting.YELLOW))
                                            .append(Text.literal("(MC " + currentMcVer + ", v" + currentModVer + " → v" + latest + ") ").formatted(Formatting.GRAY))
                                            .append(link);

                                    mc.inGameHud.getChatHud().addMessage(msg); // local only
                                });
                                java.util.concurrent.CompletableFuture
                                        .delayedExecutor(15, java.util.concurrent.TimeUnit.SECONDS)
                                        .execute(showUpdate);
                            }
                        }
                    } catch (Throwable ignored) { /* swallow quietly */ }
                });
    }
}