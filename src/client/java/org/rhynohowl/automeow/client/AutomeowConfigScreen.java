package org.rhynohowl.automeow.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.ColorEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public final class AutomeowConfigScreen {

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("[AutoMeow] Settings"));

        // Save callback when user clicks "Done"
        builder.setSavingRunnable(ModConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        // Enabled toggle
        general.addEntry(
                eb.startBooleanToggle(Component.literal("Enabled"), ModState.ENABLED.get())
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> ModState.ENABLED.set(val))
                        .setTooltip(Component.literal("Master switch for AutoMeow"))
                        .build()
        );

        ConfigCategory badgeCategory = builder.getOrCreateCategory(Component.literal("Badge"));

        List<String> badgeStyles = ModState.badgeStyleOptions();

        String currentBadgeStyle = badgeStyles.contains(ModState.badgeStyle()) ? ModState.badgeStyle() : ModState.BADGE_DEFAULT;

        String missingBadgeStyles = "";

        if (!ChromaHelper.hasSkyhanni()) {
            missingBadgeStyles += "\n(Skyhanni Chroma: Skyhanni not installed)";
        }

        if (!MeowddingHelper.hasMeowdding()) {
            missingBadgeStyles += MeowddingHelper.supportedVersion()
                    ? "\n(Meowdding Gradient: MeowddingLib not installed)"
                    : "\n(Meowdding Gradient: not available on this game version)";
        }

        badgeCategory.addEntry(
                eb.startDropdownMenu(
                                Component.literal("Style"),
                                DropdownMenuBuilder.TopCellElementBuilder.of(
                                        currentBadgeStyle,
                                        typed -> ModState.matchOption(typed, badgeStyles, ModState::badgeStyleName),
                                        style -> Component.literal(style == null ? "" : ModState.badgeStyleName(style))
                                ),
                                DropdownMenuBuilder.CellCreatorBuilder.of(
                                        14,
                                        140,
                                        8,
                                        style -> Component.literal(style == null ? "" : ModState.badgeStyleName(style))
                                )
                        )
                        .setSelections(new LinkedHashSet<>(badgeStyles))
                        .setSaveConsumer(val -> ModState.setBadgeStyle(val))
                        .setTooltip(
                                Component.literal("How the [AutoMeow] badge is coloured.")
                                        .append(Component.literal("\nSkyhanni Chroma needs Skyhanni with its own chroma enabled."))
                                        .append(Component.literal("\nMeowdding Gradient uses the settings below."))
                                        .withStyle(ChatFormatting.GRAY)
                                        .append(Component.literal(missingBadgeStyles)
                                                .withStyle(ChatFormatting.RED))
                        )
                        .build()
        );

        List<String> presetOptions = ModState.gradientPresetOptions();
        String currentPreset = presetOptions.contains(ModState.GRADIENT_PRESET)
                ? ModState.GRADIENT_PRESET : ModState.CUSTOM_GRADIENT_PRESET;
        badgeCategory.addEntry(
                eb.startDropdownMenu(
                                Component.literal("Preset"),
                                DropdownMenuBuilder.TopCellElementBuilder.of(
                                        currentPreset,
                                        typed -> ModState.matchOption(typed, presetOptions),
                                        preset -> Component.literal(preset == null ? "" : preset.replace('_', ' '))
                                ),
                                DropdownMenuBuilder.CellCreatorBuilder.of(
                                        14,
                                        140,
                                        8,
                                        preset -> Component.literal(preset == null ? "" : preset.replace('_', ' '))
                                )
                        )
                        .setSelections(new LinkedHashSet<>(presetOptions))
                        .setSaveConsumer(val -> ModState.setGradientPreset(val))
                        .setTooltip(Component.literal("Use one of Meowdding's gradients,\nor CUSTOM to use your own colours below.\nDirection and speed apply to presets too."))
                        .build()
        );
        List<Integer> colourValues = new ArrayList<>();
        for (String hex : ModState.GRADIENT_COLOURS) {
            Integer parsed = ChatUtil.parseHexColour(hex);
            if (parsed != null) colourValues.add(parsed);
        }
        List<Integer> defaultColourValues = new ArrayList<>();
        for (String hex : ModState.DEFAULT_GRADIENT_COLOURS) {
            Integer parsed = ChatUtil.parseHexColour(hex);
            if (parsed != null) defaultColourValues.add(parsed);
        }
        NestedListListEntry<Integer, ColorEntry> coloursEntry = new NestedListListEntry<>(
                Component.literal("Colours"),
                colourValues,
                true,
                () -> Optional.of(new Component[] {
                        Component.literal("Colours the gradient flows through, in order.\nClick a box and type a hex colour like #FFC0CB.\nAdd as many as you like.\nOnly used when Preset is CUSTOM.")
                }),
                val -> {
                    List<String> hexColours = new ArrayList<>();
                    for (Integer colour : val) {
                        if (colour != null) hexColours.add(String.format("%06X", colour & 0xFFFFFF));
                    }
                    ModState.setGradientColours(hexColours);
                },
                () -> defaultColourValues,
                eb.getResetButtonKey(),
                true,
                false,
                (value, entry) -> eb.startColorField(Component.literal("Colour"), value == null ? 0xFFFFFF : value)
                        .setDefaultValue(0xFFFFFF)
                        .build()
        );
        coloursEntry.setErrorSupplier(() -> coloursEntry.getValue().size() < 2
                ? Optional.of(Component.literal("Needs at least 2 colours"))
                : Optional.empty());
        badgeCategory.addEntry(coloursEntry);
        badgeCategory.addEntry(
                eb.startDropdownMenu(
                                Component.literal("Direction"),
                                DropdownMenuBuilder.TopCellElementBuilder.of(
                                        ModState.GRADIENT_DIRECTION,
                                        typed -> ModState.matchOption(typed, ModState.GRADIENT_DIRECTIONS),
                                        direction -> Component.literal(direction == null ? "" : direction.replace('_', ' '))
                                ),
                                DropdownMenuBuilder.CellCreatorBuilder.of(
                                        14,
                                        140,
                                        8,
                                        direction -> Component.literal(direction == null ? "" : direction.replace('_', ' '))
                                )
                        )
                        .setSelections(new LinkedHashSet<>(ModState.GRADIENT_DIRECTIONS))
                        .setSaveConsumer(val -> ModState.setGradientDirection(val))
                        .setTooltip(Component.literal("Which way the gradient scrolls."))
                        .build()
        );
        badgeCategory.addEntry(
                eb.startFloatField(Component.literal("Speed"), ModState.GRADIENT_SPEED)
                        .setDefaultValue(ModState.DEFAULT_GRADIENT_SPEED)
                        .setMin(ModState.GRADIENT_SPEED_MIN)
                        .setMax(ModState.GRADIENT_SPEED_MAX)
                        .setSaveConsumer(val -> ModState.setGradientSpeed(val))
                        .setTooltip(Component.literal("How fast the gradient scrolls ("
                                + ModState.GRADIENT_SPEED_MIN + " - " + ModState.GRADIENT_SPEED_MAX + ")."))
                        .build()
        );
        badgeCategory.addEntry(
                eb.startBooleanToggle(Component.literal("Seamless loop"), ModState.GRADIENT_LOOP.get())
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> ModState.GRADIENT_LOOP.set(val))
                        .setTooltip(Component.literal("Blends the last colour back into the first\nso the gradient doesn't jump when it repeats.\nPresets always loop."))
                        .build()
        );
        // My messages required (0..10; 0 disables)
        general.addEntry(
                eb.startIntSlider(Component.literal("My messages required (0 disables)"),
                                ModState.MY_MESSAGES_REQUIRED, 0, 10)
                        .setDefaultValue(3)
                        .setSaveConsumer(val -> ModState.MY_MESSAGES_REQUIRED = val)
                        .setTooltip(Component.literal("How many of YOUR chat messages must be sent\nbefore AutoMeow can reply again."))
                        .build()
        );

        // Quiet window after send (ms)
        general.addEntry(
                eb.startIntField(Component.literal("Quiet window after send (ms)"),
                                (int) ModState.QUIET_AFTER_SEND_MS)
                        .setDefaultValue(3500)
                        .setMin(0)
                        .setMax(20000)
                        .setSaveConsumer(val ->
                                ModState.QUIET_AFTER_SEND_MS = (long) Math.max(0, Math.min(val, 20000)))
                        .setTooltip(Component.literal("Ignore immediate echoes for this many milliseconds\n"
                                + "after the bot (or you) sends a message."))
                        .build()
        );

        // :3 toggle
        general.addEntry(
                eb.startBooleanToggle(Component.literal("Append \" :3\" to reply"), ModState.APPEND_FACE.get())
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.APPEND_FACE.set(val); ModConfig.save(); })
                        .setTooltip(Component.literal("When enabled, the mod sends your reply text followed by \" :3\"."))
                        .build()
        );

        List<BooleanListEntry> customReplyToggles = new ArrayList<>();
        List<String> customSelection = new ArrayList<>(ModState.CUSTOM_REPLIES);

        DropdownBoxEntry<String> replyModeEntry = eb.startDropdownMenu(
                        Component.literal("Reply mode"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(
                                ModState.REPLY_MODE,
                                typed -> ModState.matchOption(typed, ModState.REPLY_MODES, ModState::replyModeName),
                                mode -> Component.literal(mode == null ? "" : ModState.replyModeName(mode))
                        ),
                        DropdownMenuBuilder.CellCreatorBuilder.of(
                                14,
                                140,
                                8,
                                mode -> Component.literal(mode == null ? "" : ModState.replyModeName(mode))
                        )
                )
                .setSelections(new LinkedHashSet<>(ModState.REPLY_MODES))
                .setSaveConsumer(val -> ModState.setReplyMode(val))
                .setTooltip(Component.literal("Single sends your Reply text and uses \"My messages required\".\n"
                        + "Cat, Dog and Custom pick a random sound every reply that isn't\n"
                        + "one of your last " + ModState.RECENT_MESSAGES_REMEMBERED + " messages, so \"My messages required\" is skipped."))
                .build();
        replyModeEntry.setErrorSupplier(() -> {
            if (!ModState.REPLY_MODE_CUSTOM.equals(replyModeEntry.getValue())) return Optional.empty();
            long ticked = customReplyToggles.stream().filter(BooleanListEntry::getValue).count();
            return ticked < ModState.MIN_CUSTOM_REPLIES
                    ? Optional.of(Component.literal("Custom needs at least " + ModState.MIN_CUSTOM_REPLIES + " sounds ticked"))
                    : Optional.empty();
        });
        general.addEntry(replyModeEntry);

        general.addEntry(
                eb.startDropdownMenu(
                                Component.literal("Reply text"),
                                DropdownMenuBuilder.TopCellElementBuilder.of(
                                        ModState.REPLY_TEXT,
                                        typed -> ModState.matchOption(typed, ModState.REPLY_PRESETS),
                                        preset -> Component.literal(preset == null ? "" : preset)
                                ),
                                DropdownMenuBuilder.CellCreatorBuilder.of(
                                        14,
                                        140,
                                        8,
                                        preset -> Component.literal(preset == null ? "" : preset)
                                )
                        )
                        .setSelections(new LinkedHashSet<>(ModState.REPLY_PRESETS))
                        .setDefaultValue(ModState.DEFAULT_REPLY_TEXT)
                        .setSaveConsumer(selectedPreset -> ModState.setReplyText(selectedPreset))
                        .build()
        );

        SubCategoryBuilder customRepliesCategory = eb.startSubCategory(Component.literal("Custom replies")).setExpanded(false);
        for (String preset : ModState.REPLY_PRESETS) {
            BooleanListEntry toggle = eb.startBooleanToggle(Component.literal(preset), ModState.CUSTOM_REPLIES.contains(preset))
                    .setDefaultValue(ModState.DEFAULT_CUSTOM_REPLIES.contains(preset))
                    .setSaveConsumer(val -> {
                        customSelection.remove(preset);
                        if (val) customSelection.add(preset);
                        ModState.setCustomReplies(customSelection);
                    })
                    .build();
            customReplyToggles.add(toggle);
            customRepliesCategory.add(toggle);
        }
        customRepliesCategory.setTooltip(Component.literal("Sounds the Custom reply mode picks from.\nTick at least " + ModState.MIN_CUSTOM_REPLIES + "."));
        general.addEntry(customRepliesCategory.build());

        // Play meow sound
        general.addEntry(
                eb.startBooleanToggle(Component.literal("Play meow sound"), ModState.PLAY_SOUND.get())
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.PLAY_SOUND.set(val); ModConfig.save(); })
                        .setTooltip(Component.literal("Plays a cat meow when someone says a cat-sound (you or others)."))
                        .build()
        );

        // Show heart particles
        general.addEntry(
                eb.startBooleanToggle(Component.literal("Show heart particles"), ModState.HEARTS_EFFECT.get())
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.HEARTS_EFFECT.set(val); ModConfig.save(); })
                        .setTooltip(Component.literal("Spawns the hearts effect around the player who meowed."))
                        .build()
        );

        // Cat facts (I mean like cmon it says it right there like 8 times)
        SubCategoryBuilder catFactsCategory = eb.startSubCategory(Component.literal("Cat Facts")).setExpanded(false);
        catFactsCategory.add(
                eb.startBooleanToggle(Component.literal("Party Chat !catfact"), ModState.CATFACT_PARTY.get())
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.CATFACT_PARTY.set(val); ModConfig.save(); })
                        .setTooltip(Component.literal("Automatically reply with a cat fact when !catfact is sent in party chat."))
                        .build()
        );
        catFactsCategory.add(
                eb.startBooleanToggle(Component.literal("Guild Chat !catfact"), ModState.CATFACT_GUILD.get())
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.CATFACT_GUILD.set(val); ModConfig.save(); })
                        .setTooltip(Component.literal("Automatically reply with a cat fact when !catfact is sent in guild chat."))
                        .build()
        );
        catFactsCategory.add(
                eb.startBooleanToggle(Component.literal("Private Message !catfact "), ModState.CATFACT_PM.get())
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.CATFACT_PM.set(val); ModConfig.save(); })
                        .setTooltip(Component.literal("Automatically reply with a cat fact when !catfact is sent in private messages."))
                        .build()
        );
        catFactsCategory.add(
                eb.startBooleanToggle(Component.literal("Only own messages "), ModState.CATFACT_SELF_ONLY.get())
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.CATFACT_SELF_ONLY.set(val); ModConfig.save(); })
                        .setTooltip(Component.literal("Only send catfacts to your own !catfact message."))
                        .build()
        );
        general.addEntry(catFactsCategory.build());

        // Channels enable/disable per chat channel
        SubCategoryBuilder channelsCategory = eb.startSubCategory(Component.literal("Channels")).setExpanded(false);
        channelsCategory.add(
                eb.startBooleanToggle(Component.literal("Public"), ModState.isChannelEnabled(HpChannel.ALL))
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.ALL, val); ModConfig.save(); })
                        .setTooltip(Component.literal("Reply to cat-sounds in public/all chat."))
                        .build()
        );
        channelsCategory.add(
                eb.startBooleanToggle(Component.literal("Guild"), ModState.isChannelEnabled(HpChannel.GUILD))
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.GUILD, val); ModConfig.save(); })
                        .setTooltip(Component.literal("Reply to cat-sounds in guild chat."))
                        .build()
        );
        channelsCategory.add(
                eb.startBooleanToggle(Component.literal("Party"), ModState.isChannelEnabled(HpChannel.PARTY))
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.PARTY, val); ModConfig.save(); })
                        .setTooltip(Component.literal("Reply to cat-sounds in party chat."))
                        .build()
        );
        channelsCategory.add(
                eb.startBooleanToggle(Component.literal("Co-op"), ModState.isChannelEnabled(HpChannel.COOP))
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.COOP, val); ModConfig.save(); })
                        .setTooltip(Component.literal("Reply to cat-sounds in co-op chat."))
                        .build()
        );
        channelsCategory.add(
                eb.startBooleanToggle(Component.literal("PM (whispers)"), ModState.isChannelEnabled(HpChannel.PM))
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.PM, val); ModConfig.save(); })
                        .setTooltip(Component.literal("Reply to cat-sounds in private messages."))
                        .build()
        );
        channelsCategory.add(
                eb.startBooleanToggle(Component.literal("Officer"), ModState.isChannelEnabled(HpChannel.OFFICER))
                        .setDefaultValue(true)
                        .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.OFFICER, val); ModConfig.save(); })
                        .setTooltip(Component.literal("Reply to cat-sounds in officer chat."))
                        .build()
        );
        general.addEntry(channelsCategory.build());

        return builder.build();
    }
}