package org.rhynohowl.automeow.client;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ModState {
    // Tunables
    public static volatile int MY_MESSAGES_REQUIRED = 3;      // you must send 3 msgs between auto-replies
    public static volatile long QUIET_AFTER_SEND_MS = 3500;   // mute echoes after we send (and after you type meow)
    public static final AtomicBoolean CHROMA_WANTED = new AtomicBoolean(false); // user toggle for chroma

    // State
    public static final AtomicBoolean ENABLED = new AtomicBoolean(true);
    public static final AtomicBoolean skipNextOwnIncrement = new AtomicBoolean(false);
    public static final java.util.concurrent.atomic.AtomicBoolean PLAY_SOUND = new java.util.concurrent.atomic.AtomicBoolean(true);
    public static final java.util.concurrent.atomic.AtomicBoolean HEARTS_EFFECT = new java.util.concurrent.atomic.AtomicBoolean(true);
    public static final AtomicBoolean manualSendPending = new AtomicBoolean(false);
    public static final AtomicBoolean DEBUG = new AtomicBoolean(false);
    public static final AtomicBoolean ON_HYPIXEL = new AtomicBoolean(false);
    public static final java.util.concurrent.atomic.AtomicBoolean APPEND_FACE = new java.util.concurrent.atomic.AtomicBoolean(false);
    public static final AtomicLong echoUntil = new AtomicLong(0);     // hard anti-echo
    public static final AtomicLong cooldownUntil = new AtomicLong(0); // OR-gate timer

    public static final java.util.EnumMap<HpChannel, AtomicInteger> msgsSinceReply =
            new java.util.EnumMap<>(HpChannel.class);

    static {
        for (HpChannel ch : HpChannel.values()) {
            msgsSinceReply.put(ch, new AtomicInteger(MY_MESSAGES_REQUIRED)); // start "ready"
        }
    }

    public static AtomicInteger counter(HpChannel ch) {
        return msgsSinceReply.get(ch);
    }

    public static void startTimer() {
        long timer = System.currentTimeMillis() + QUIET_AFTER_SEND_MS;
        echoUntil.set(timer);
        cooldownUntil.set(timer);
    }

    public static final java.util.List<String> REPLY_PRESETS = java.util.List.of(
            "meow",
            "mrrp",
            "mrow",
            "mrraow",
            "mer",
            "nya",
            "purr",
            "bark",
            "woof",
            "wruff"
    );

    public static final String DEFAULT_REPLY_TEXT = REPLY_PRESETS.get(0);

    // Custom reply text (what we send). Default: "meow".
    public static volatile String REPLY_TEXT = DEFAULT_REPLY_TEXT;

    public static boolean setReplyText(String requestedText) {
        String canon = canonicalPresetOrNull(requestedText);
        if (canon == null) return false;
        REPLY_TEXT = canon;
        return true;
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