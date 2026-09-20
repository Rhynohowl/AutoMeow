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
    public static final java.util.concurrent.atomic.AtomicBoolean APPEND_FACE = new java.util.concurrent.atomic.AtomicBoolean(true);
    public static final AtomicLong echoUntil = new AtomicLong(0);     // hard anti-echo
    public static final AtomicLong cooldownUntil = new AtomicLong(0); // OR-gate timer
    public static final AtomicBoolean CATFACT_PARTY = new AtomicBoolean(true);
    public static final AtomicBoolean CATFACT_GUILD = new AtomicBoolean(true);
    public static final AtomicBoolean CATFACT_PM = new AtomicBoolean(true);
    public static final AtomicBoolean CATFACT_SELF_ONLY = new AtomicBoolean(true);

    public static final java.util.EnumMap<HpChannel, AtomicInteger> msgsSinceReply =
            new java.util.EnumMap<>(HpChannel.class);

    static {
        for (HpChannel ch : HpChannel.values()) {
            msgsSinceReply.put(ch, new AtomicInteger(MY_MESSAGES_REQUIRED)); // start "ready"
        }
    }

    public static final java.util.EnumMap<HpChannel, AtomicBoolean> channelEnabled =
            new java.util.EnumMap<>(HpChannel.class);

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

    public static final java.util.List<String> REPLY_PRESETS = java.util.List.of(
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

    // Hypixel rejects a message identical to your last one ("You cannot say the same
    // message twice!"), so rotation cycles the reply instead of repeating it.
    public static final java.util.List<String> DEFAULT_REPLY_ROTATION =
            java.util.List.of("meow", "mrrp", "mrow", "mrraow", "mew");

    public static final AtomicBoolean ROTATE_REPLIES = new AtomicBoolean(false);

    private static final java.util.List<String> REPLY_ROTATION =
            new java.util.concurrent.CopyOnWriteArrayList<>(DEFAULT_REPLY_ROTATION);

    private static final AtomicInteger ROTATION_INDEX = new AtomicInteger(0);

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

    private static String canonicalPresetOrNull(String input) {
        if (input == null) return null;
        String trimed = input.trim();
        for (String option : REPLY_PRESETS) {
            if (option.equalsIgnoreCase(trimed)) return option;
        }
        return null;
    }

    public static java.util.List<String> getReplyRotation() {
        return java.util.List.copyOf(REPLY_ROTATION);
    }

    // drops anything that isn't a preset, and any repeats. returns how many entries survived
    public static int setReplyRotation(java.util.List<String> requested) {
        java.util.LinkedHashSet<String> kept = new java.util.LinkedHashSet<>();
        if (requested != null) {
            for (String entry : requested) {
                String canon = canonicalPresetOrNull(entry);
                if (canon != null) kept.add(canon);
            }
        }
        REPLY_ROTATION.clear();
        REPLY_ROTATION.addAll(kept);
        ROTATION_INDEX.set(0);
        return REPLY_ROTATION.size();
    }

    public static boolean addRotationEntry(String requestedText) {
        String canon = canonicalPresetOrNull(requestedText);
        if (canon == null || REPLY_ROTATION.contains(canon)) return false;
        REPLY_ROTATION.add(canon);
        return true;
    }

    public static boolean removeRotationEntry(String requestedText) {
        String canon = canonicalPresetOrNull(requestedText);
        if (canon == null) return false;
        boolean removed = REPLY_ROTATION.remove(canon);
        if (removed) ROTATION_INDEX.set(0);
        return removed;
    }

    public static void clearReplyRotation() {
        REPLY_ROTATION.clear();
        ROTATION_INDEX.set(0);
    }

    // text for the next auto-reply: steps the rotation along, or REPLY_TEXT when rotation is off
    public static String nextReplyText() {
        if (!ROTATE_REPLIES.get()) return REPLY_TEXT;
        java.util.List<String> rotation = java.util.List.copyOf(REPLY_ROTATION);
        if (rotation.isEmpty()) return REPLY_TEXT;
        return rotation.get(Math.floorMod(ROTATION_INDEX.getAndIncrement(), rotation.size()));
    }
}