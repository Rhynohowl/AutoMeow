package org.rhynohowl.automeow.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ModState {
    // Tunables
    public static volatile int MY_MESSAGES_REQUIRED = 3;      // you must send 3 msgs between auto-replies
    public static volatile long QUIET_AFTER_SEND_MS = 3500;   // mute echoes after we send (and after you type meow)
    public static final AtomicBoolean CHROMA_WANTED = new AtomicBoolean(false); // user toggle for chroma
    public static final AtomicBoolean GRADIENT_WANTED = new AtomicBoolean(false);
    public static final AtomicBoolean GRADIENT_LOOP = new AtomicBoolean(true);
    public static final List<String> DEFAULT_GRADIENT_COLOURS = List.of("FFCCE5", "FF99CC", "FF66B2", "CC0066", "99004C", "660033"); // OG fairy dye :3
    public static volatile List<String> GRADIENT_COLOURS = DEFAULT_GRADIENT_COLOURS;
    public static final List<String> GRADIENT_DIRECTIONS = List.of(
            "LEFT", "RIGHT", "UP", "DOWN", "UP_LEFT", "UP_RIGHT", "DOWN_LEFT", "DOWN_RIGHT"
    );
    public static final String CUSTOM_GRADIENT_PRESET = "CUSTOM";
    public static volatile String GRADIENT_PRESET = CUSTOM_GRADIENT_PRESET;
    public static final String DEFAULT_GRADIENT_DIRECTION = "RIGHT";
    public static volatile String GRADIENT_DIRECTION = DEFAULT_GRADIENT_DIRECTION;
    public static final float GRADIENT_SPEED_MIN = 0.1f;
    public static final float GRADIENT_SPEED_MAX = 5.0f;
    public static final float DEFAULT_GRADIENT_SPEED = 1.0f;
    public static volatile float GRADIENT_SPEED = DEFAULT_GRADIENT_SPEED;

    // State
    public static final AtomicBoolean ENABLED = new AtomicBoolean(true);
    public static final AtomicBoolean skipNextOwnIncrement = new AtomicBoolean(false);
    public static final AtomicBoolean PLAY_SOUND = new AtomicBoolean(true);
    public static final AtomicBoolean HEARTS_EFFECT = new AtomicBoolean(true);
    public static final AtomicBoolean manualSendPending = new AtomicBoolean(false);
    public static final AtomicBoolean DEBUG = new AtomicBoolean(false);
    public static final AtomicBoolean ON_HYPIXEL = new AtomicBoolean(false);
    public static final AtomicBoolean APPEND_FACE = new AtomicBoolean(true);
    public static final AtomicLong echoUntil = new AtomicLong(0);     // hard anti-echo
    public static final AtomicLong cooldownUntil = new AtomicLong(0); // OR-gate timer
    public static final AtomicBoolean CATFACT_PARTY = new AtomicBoolean(true);
    public static final AtomicBoolean CATFACT_GUILD = new AtomicBoolean(true);
    public static final AtomicBoolean CATFACT_PM = new AtomicBoolean(true);
    public static final AtomicBoolean CATFACT_SELF_ONLY = new AtomicBoolean(true);

    public static final EnumMap<HpChannel, AtomicInteger> msgsSinceReply =
            new EnumMap<>(HpChannel.class);

    static {
        for (HpChannel ch : HpChannel.values()) {
            msgsSinceReply.put(ch, new AtomicInteger(MY_MESSAGES_REQUIRED)); // start "ready"
        }
    }

    public static final EnumMap<HpChannel, AtomicBoolean> channelEnabled =
            new EnumMap<>(HpChannel.class);

    static {
        for (HpChannel ch : HpChannel.values()) {
            channelEnabled.put(ch, new AtomicBoolean(true)); // every channel on by default
        }
    }

    public static boolean isChannelEnabled(HpChannel ch) {
        return channelEnabled.get(ch).get();
    }

    public static void setChannelEnabled(HpChannel ch, boolean value) {
        channelEnabled.get(ch).set(value);
    }

    public static AtomicInteger counter(HpChannel ch) {
        return msgsSinceReply.get(ch);
    }

    public static void startTimer() {
        long timer = System.currentTimeMillis() + QUIET_AFTER_SEND_MS;
        echoUntil.set(timer);
        cooldownUntil.set(timer);
    }

    public static final List<String> REPLY_PRESETS = List.of(
            "meow",
            "mrrp",
            "mrrrp",
            "mrow",
            "mrraow",
            "mew",
            "nya",
            "purr",
            "bark",
            "woof",
            "wruff",
            "grrr",
            "arf",
            "awoo",
            "awrf",
            "awruf"
    );

    public static void incrementReplyCount() {
        ModConfig.CONFIG.totalReplies++;
        ModConfig.save();
    }

    public static final String DEFAULT_REPLY_TEXT = REPLY_PRESETS.get(0);

    // Custom reply text (what we send). Default: "meow".
    public static volatile String REPLY_TEXT = DEFAULT_REPLY_TEXT;

    public static boolean setReplyText(String requestedText) {
        String canon = canonicalPresetOrNull(requestedText);
        if (canon == null) return false;
        REPLY_TEXT = canon;
        return true;
    }

    public static void setGradientColours(List<String> colours) {
        GRADIENT_COLOURS = colours == null ? DEFAULT_GRADIENT_COLOURS : colours.stream().filter(Objects::nonNull).toList();
    }

    public static final String BADGE_DEFAULT = "DEFAULT";
    public static final String BADGE_CHROMA = "SKYHANNI_CHROMA";
    public static final String BADGE_GRADIENT = "MEOWDDING_GRADIENT";

    public static String badgeStyle() {
        if (GRADIENT_WANTED.get()) return BADGE_GRADIENT;
        if (CHROMA_WANTED.get()) return BADGE_CHROMA;
        return BADGE_DEFAULT;
    }

    public static void setBadgeStyle(String style) {
        CHROMA_WANTED.set(BADGE_CHROMA.equals(style));
        GRADIENT_WANTED.set(BADGE_GRADIENT.equals(style));
    }

    public static String badgeStyleName(String style) {
        if (BADGE_CHROMA.equals(style)) return "Skyhanni Chroma";
        if (BADGE_GRADIENT.equals(style)) return "Meowdding Gradient";
        return "Default (pink)";
    }

    public static List<String> badgeStyleOptions() {
        List<String> options = new ArrayList<>();
        options.add(BADGE_DEFAULT);
        if (ChromaHelper.hasSkyhanni()) options.add(BADGE_CHROMA);
        if (MeowddingHelper.hasMeowdding()) options.add(BADGE_GRADIENT);
        return options;
    }

    public static String matchBadgeStyle(String input, List<String> options) {
        if (input == null) return null;
        String wanted = input.trim();
        for (String option : options) {
            if (option.equalsIgnoreCase(wanted.replace(' ', '_')) || badgeStyleName(option).equalsIgnoreCase(wanted)) return option;
        }
        return null;
    }

    public static String matchOption(String input, List<String> options) {
        if (input == null) return null;
        String wanted = input.trim().replace(' ', '_');
        for (String option : options) {
            if (option.equalsIgnoreCase(wanted)) return option;
        }
        return null;
    }

    public static List<String> gradientPresetOptions() {
        List<String> options = new ArrayList<>();
        options.add(CUSTOM_GRADIENT_PRESET);
        options.addAll(MeowddingHelper.presetNames());
        return options;
    }

    public static void setGradientPreset(String preset) {
        GRADIENT_PRESET = preset == null || preset.isBlank() ? CUSTOM_GRADIENT_PRESET : preset;
    }

    public static void setGradientDirection(String direction) {
        String matched = matchOption(direction, GRADIENT_DIRECTIONS);
        GRADIENT_DIRECTION = matched != null ? matched : DEFAULT_GRADIENT_DIRECTION;
    }

    public static void setGradientSpeed(float speed) {
        GRADIENT_SPEED = Math.max(GRADIENT_SPEED_MIN, Math.min(speed, GRADIENT_SPEED_MAX));
    }

    private static String canonicalPresetOrNull(String input) {
        if (input == null) return null;
        String trimed = input.trim();
        for (String option : REPLY_PRESETS) {
            if (option.equalsIgnoreCase(trimed)) return option;
        }
        return null;
    }
}