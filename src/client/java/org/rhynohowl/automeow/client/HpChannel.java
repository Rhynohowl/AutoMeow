package org.rhynohowl.automeow.client;

import java.util.regex.Pattern;

public enum HpChannel {
    ALL, GUILD, PARTY, COOP, PM, IGNORE;

    private static final Pattern VANILLA_WHISPER_IN = Pattern.compile("^\\s*<?([A-Za-z0-9_]{3,16})>?\\s+whispers\\s+to\\s+you\\s*:", Pattern.CASE_INSENSITIVE);

    private static final java.util.regex.Pattern LEADING_WORD =
            java.util.regex.Pattern.compile("^\\s*([A-Za-z]+(?:[-\\p{Pd}][A-Za-z]+)?)");

    public static Pattern vanillaWhisperPattern() {
        return VANILLA_WHISPER_IN;
    }

    public static HpChannel detect(String raw) {
        if (raw == null) return ALL;
        String strippedFormatting = raw.replaceAll("§.", "");

        if (VANILLA_WHISPER_IN.matcher(strippedFormatting).find()) {
            return PM;
        }

        strippedFormatting = strippedFormatting.replaceAll("\\p{Pd}", "-");
        var leadingWordMatcher = LEADING_WORD.matcher(strippedFormatting);
        if (!leadingWordMatcher.find()) return ALL;
        String normalisedWord = leadingWordMatcher.group(1).toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z]", "");
        switch (normalisedWord) {
            case "party":   return PARTY;
            case "guild":   return GUILD;
            case "coop":    return COOP;
            case "from":    return PM;
            case "to":      return IGNORE;
            default:        return ALL;
        }
    }
}