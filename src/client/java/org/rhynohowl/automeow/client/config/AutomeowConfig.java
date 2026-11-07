package org.rhynohowl.automeow.client.config;

import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;
import org.rhynohowl.automeow.client.ModState;
import java.util.ArrayList;
import java.util.List;

public final class AutomeowConfig extends Config {

    @Override
    public StructuredText getTitle() {
        return StructuredText.of("AutoMeow Settings");
    }

    @Category(name = "General", desc = "Main settings")
    public General general = new General();

    @Category(name = "Badge", desc = "Badge colours")
    public Badge badge = new Badge();

    @Category(name = "Cat Facts", desc = "!catfact replies")
    public CatFacts catFacts = new CatFacts();

    @Category(name = "Channels", desc = "Where AutoMeow replies")
    public Channels channels = new Channels();

    public static final class General {
        @ConfigOption(name = "Enabled", desc = "Turns AutoMeow on or off.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @ConfigOption(name = "My messages required", desc = "How many messages you need to send before it replies again.\n0 turns this off.")
        @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1)
        public float myMessagesRequired = 3;

        @ConfigOption(name = "Quiet window after send (ms)", desc = "How long to ignore chat after a reply goes out.")
        @ConfigEditorSlider(minValue = 0, maxValue = 20000, minStep = 100)
        public float quietAfterSendMs = 3500;

        @ConfigOption(name = "Append \" :3\" to reply", desc = "Adds \" :3\" to the end of replies.")
        @ConfigEditorBoolean
        public boolean appendFace = true;

        @ConfigOption(name = "Reply mode", desc = "Single always sends your Reply text.\nCat, Dog, Fox and Custom pick a random sound and ignore My messages required.")
        @AutomeowDropdown(AutomeowDropdown.Source.REPLY_MODE)
        public String replyMode = "";

        @ConfigOption(name = "Reply text", desc = "What gets sent in Single mode.")
        @AutomeowDropdown(AutomeowDropdown.Source.REPLY_TEXT)
        public String replyText = "";

        @ConfigOption(name = "Custom reply mode sounds", desc = "Sounds Custom mode picks from. Needs at least " + ModState.MIN_CUSTOM_REPLIES + ".")
        @AutomeowReplyList
        public List<Integer> customReplies = new ArrayList<>();

        @ConfigOption(name = "Play meow sound", desc = "Plays a meow when someone meows.")
        @ConfigEditorBoolean
        public boolean playSound = true;

        @ConfigOption(name = "Show heart particles", desc = "Hearts around whoever meowed.")
        @ConfigEditorBoolean
        public boolean heartsEffect = true;
    }

    public static final class Badge {
        @ConfigOption(name = "Style", desc = "Only lists styles for mods you have installed.\nSkyhanni Chroma needs chroma turned on in Skyhanni.")
        @AutomeowDropdown(AutomeowDropdown.Source.BADGE_STYLE)
        public String style = "";

        @ConfigOption(name = "Preset", desc = "A Meowdding gradient, or CUSTOM to use your own colours.")
        @AutomeowDropdown(AutomeowDropdown.Source.GRADIENT_PRESET)
        public String gradientPreset = "";

        @ConfigOption(name = "Colours", desc = "Hex colours separated by commas. Needs at least 2.\nOnly used with CUSTOM.")
        @ConfigEditorText
        public String gradientColours = "";

        @ConfigOption(name = "Direction", desc = "Which way the gradient moves.")
        @AutomeowDropdown(AutomeowDropdown.Source.GRADIENT_DIRECTION)
        public String gradientDirection = "";

        @ConfigOption(name = "Speed", desc = "How fast the gradient moves.")
        @ConfigEditorSlider(minValue = ModState.GRADIENT_SPEED_MIN, maxValue = ModState.GRADIENT_SPEED_MAX, minStep = 0.1f)
        public float gradientSpeed = ModState.DEFAULT_GRADIENT_SPEED;

        @ConfigOption(name = "Seamless loop", desc = "Fades the last colour back into the first.\nPresets always loop.")
        @ConfigEditorBoolean
        public boolean gradientLoop = true;
    }

    public static final class CatFacts {
        @ConfigOption(name = "Party Chat !catfact", desc = "Reply to !catfact in party chat.")
        @ConfigEditorBoolean
        public boolean party = true;

        @ConfigOption(name = "Guild Chat !catfact", desc = "Reply to !catfact in guild chat.")
        @ConfigEditorBoolean
        public boolean guild = true;

        @ConfigOption(name = "Private Message !catfact", desc = "Reply to !catfact in PMs.")
        @ConfigEditorBoolean
        public boolean pm = true;

        @ConfigOption(name = "Only own messages", desc = "Only reply to your own !catfact.")
        @ConfigEditorBoolean
        public boolean selfOnly = true;
    }

    public static final class Channels {
        @ConfigOption(name = "Public", desc = "Reply in public chat.")
        @ConfigEditorBoolean
        public boolean all = true;

        @ConfigOption(name = "Guild", desc = "Reply in guild chat.")
        @ConfigEditorBoolean
        public boolean guild = true;

        @ConfigOption(name = "Party", desc = "Reply in party chat.")
        @ConfigEditorBoolean
        public boolean party = true;

        @ConfigOption(name = "Co-op", desc = "Reply in co-op chat.")
        @ConfigEditorBoolean
        public boolean coop = true;

        @ConfigOption(name = "PM (whispers)", desc = "Reply in PMs.")
        @ConfigEditorBoolean
        public boolean pm = true;

        @ConfigOption(name = "Officer", desc = "Reply in officer chat.")
        @ConfigEditorBoolean
        public boolean officer = true;
    }
}