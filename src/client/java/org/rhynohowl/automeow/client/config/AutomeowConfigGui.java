package org.rhynohowl.automeow.client.config;

import io.github.notenoughupdates.moulconfig.gui.CloseEventListener;
import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDraggableList;
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown;
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.rhynohowl.automeow.client.*;

import java.util.ArrayList;
import java.util.List;

public final class AutomeowConfigGui {

    public static void open() {
        Minecraft.getInstance().schedule(() -> show(screen(null)));
    }

    public static Screen screen(Screen parent) {
        AutomeowConfig config = fromState();
        MoulConfigProcessor<AutomeowConfig> processor = MoulConfigProcessor.withDefaults(config);
        processor.registerConfigEditor(AutomeowDropdown.class, (ProcessedOption option, AutomeowDropdown dropdown) ->
                new GuiOptionEditorDropdown(option, options(dropdown.value()).toArray(new String[0]), true));
        processor.registerConfigEditor(AutomeowReplyList.class, (ProcessedOption option, AutomeowReplyList list) ->
                new GuiOptionEditorDraggableList(option, ModState.REPLY_PRESETS.toArray(new String[0]), true));
        ConfigProcessorDriver driver = new ConfigProcessorDriver(processor);
        driver.checkExpose = false;
        driver.processConfig(config);
        MoulConfigEditor<AutomeowConfig> editor = new MoulConfigEditor<>(processor);
        return new MoulConfigScreenComponent(Component.literal("AutoMeow Settings"), new GuiContext(new GuiElementComponent(editor)), parent) {
            @Override
            public void onClose() {
                if (getGuiContext().onBeforeClose() == CloseEventListener.CloseAction.NO_OBJECTIONS_TO_CLOSE) show(parent);
            }

            @Override
            public void removed() {
                super.removed();
                apply(config);
            }
        };
    }

    private static void show(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.2 {
        /*mc.gui.setScreen(screen);
         *///?} else {
        mc.setScreen(screen);
        //?}
    }

    private static List<String> options(AutomeowDropdown.Source source) {
        return switch (source) {
            case BADGE_STYLE -> ModState.badgeStyleOptions().stream().map(ModState::badgeStyleName).toList();
            case GRADIENT_PRESET -> ModState.gradientPresetOptions().stream().map(AutomeowConfigGui::spaced).toList();
            case GRADIENT_DIRECTION -> ModState.GRADIENT_DIRECTIONS.stream().map(AutomeowConfigGui::spaced).toList();
            case REPLY_MODE -> ModState.REPLY_MODES.stream().map(ModState::replyModeName).toList();
            case REPLY_TEXT -> ModState.REPLY_PRESETS;
        };
    }

    private static String spaced(String option) {
        return option.replace('_', ' ');
    }

    private static String shownBadgeStyle() {
        return ModState.badgeStyleOptions().contains(ModState.badgeStyle()) ? ModState.badgeStyle() : ModState.BADGE_DEFAULT;
    }

    private static String shownGradientPreset() {
        return ModState.gradientPresetOptions().contains(ModState.GRADIENT_PRESET) ? ModState.GRADIENT_PRESET : ModState.CUSTOM_GRADIENT_PRESET;
    }

    private static AutomeowConfig fromState() {
        AutomeowConfig config = new AutomeowConfig();

        AutomeowConfig.General general = config.general;
        general.enabled = ModState.ENABLED.get();
        general.myMessagesRequired = ModState.MY_MESSAGES_REQUIRED;
        general.quietAfterSendMs = ModState.QUIET_AFTER_SEND_MS;
        general.appendFace = ModState.APPEND_FACE.get();
        general.replyMode = ModState.replyModeName(ModState.REPLY_MODE);
        general.replyText = ModState.REPLY_TEXT;
        for (String reply : ModState.CUSTOM_REPLIES) {
            general.customReplies.add(ModState.REPLY_PRESETS.indexOf(reply));
        }
        general.playSound = ModState.PLAY_SOUND.get();
        general.heartsEffect = ModState.HEARTS_EFFECT.get();

        AutomeowConfig.Badge badge = config.badge;
        badge.style = ModState.badgeStyleName(shownBadgeStyle());
        badge.gradientPreset = spaced(shownGradientPreset());
        List<String> colours = new ArrayList<>();
        for (String hex : ModState.GRADIENT_COLOURS) {
            colours.add("#" + hex);
        }
        badge.gradientColours = String.join(", ", colours);
        badge.gradientDirection = spaced(ModState.GRADIENT_DIRECTION);
        badge.gradientSpeed = ModState.GRADIENT_SPEED;
        badge.gradientLoop = ModState.GRADIENT_LOOP.get();

        AutomeowConfig.CatFacts catFacts = config.catFacts;
        catFacts.party = ModState.CATFACT_PARTY.get();
        catFacts.guild = ModState.CATFACT_GUILD.get();
        catFacts.pm = ModState.CATFACT_PM.get();
        catFacts.selfOnly = ModState.CATFACT_SELF_ONLY.get();

        AutomeowConfig.Channels channels = config.channels;
        channels.all = ModState.isChannelEnabled(HpChannel.ALL);
        channels.guild = ModState.isChannelEnabled(HpChannel.GUILD);
        channels.party = ModState.isChannelEnabled(HpChannel.PARTY);
        channels.coop = ModState.isChannelEnabled(HpChannel.COOP);
        channels.pm = ModState.isChannelEnabled(HpChannel.PM);
        channels.officer = ModState.isChannelEnabled(HpChannel.OFFICER);

        return config;
    }

    private static void apply(AutomeowConfig config) {
        AutomeowConfig.General general = config.general;
        ModState.ENABLED.set(general.enabled);
        ModState.MY_MESSAGES_REQUIRED = Math.max(0, Math.min(Math.round(general.myMessagesRequired), 10));
        ModState.QUIET_AFTER_SEND_MS = Math.max(0, Math.min(Math.round(general.quietAfterSendMs), 20000));
        ModState.APPEND_FACE.set(general.appendFace);

        List<String> oldCustomReplies = ModState.CUSTOM_REPLIES;
        List<String> picked = new ArrayList<>();
        for (int index : general.customReplies) {
            picked.add(ModState.REPLY_PRESETS.get(index));
        }
        ModState.setCustomReplies(picked);
        if (ModState.CUSTOM_REPLIES.size() < ModState.MIN_CUSTOM_REPLIES) ModState.CUSTOM_REPLIES = oldCustomReplies;

        String mode = ModState.matchOption(general.replyMode, ModState.REPLY_MODES, ModState::replyModeName);
        boolean customTooSmall = ModState.REPLY_MODE_CUSTOM.equals(mode) && ModState.CUSTOM_REPLIES.size() < ModState.MIN_CUSTOM_REPLIES;
        if (mode != null && !customTooSmall) ModState.setReplyMode(mode);
        ModState.setReplyText(general.replyText);
        ModState.PLAY_SOUND.set(general.playSound);
        ModState.HEARTS_EFFECT.set(general.heartsEffect);

        AutomeowConfig.Badge badge = config.badge;
        String style = ModState.matchOption(badge.style, ModState.badgeStyleOptions(), ModState::badgeStyleName);
        if (style != null && !style.equals(shownBadgeStyle())) ModState.setBadgeStyle(style);
        String preset = ModState.matchOption(badge.gradientPreset, ModState.gradientPresetOptions());
        if (preset != null && !preset.equals(shownGradientPreset())) ModState.setGradientPreset(preset);
        List<String> colours = new ArrayList<>();
        for (String part : badge.gradientColours.split("[\\s,]+")) {
            Integer colour = ChatUtil.parseHexColour(part);
            if (colour != null) colours.add(String.format("%06X", colour));
        }
        if (colours.size() >= 2) ModState.setGradientColours(colours);
        ModState.setGradientDirection(badge.gradientDirection);
        ModState.setGradientSpeed(badge.gradientSpeed);
        ModState.GRADIENT_LOOP.set(badge.gradientLoop);

        AutomeowConfig.CatFacts catFacts = config.catFacts;
        ModState.CATFACT_PARTY.set(catFacts.party);
        ModState.CATFACT_GUILD.set(catFacts.guild);
        ModState.CATFACT_PM.set(catFacts.pm);
        ModState.CATFACT_SELF_ONLY.set(catFacts.selfOnly);

        AutomeowConfig.Channels channels = config.channels;
        ModState.setChannelEnabled(HpChannel.ALL, channels.all);
        ModState.setChannelEnabled(HpChannel.GUILD, channels.guild);
        ModState.setChannelEnabled(HpChannel.PARTY, channels.party);
        ModState.setChannelEnabled(HpChannel.COOP, channels.coop);
        ModState.setChannelEnabled(HpChannel.PM, channels.pm);
        ModState.setChannelEnabled(HpChannel.OFFICER, channels.officer);

        ModConfig.save();
    }
}