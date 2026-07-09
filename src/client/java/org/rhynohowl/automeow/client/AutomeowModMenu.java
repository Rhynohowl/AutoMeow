package org.rhynohowl.automeow.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.LinkedHashSet;



public class AutomeowModMenu implements ModMenuApi {

    private static String canonicalPresetOrNull(String typed) {
        if (typed == null) return null;
        String trimed = typed.trim();
        if (trimed.isEmpty()) return null;

        for (String option : ModState.REPLY_PRESETS) {
            if (option.equalsIgnoreCase(trimed)) return option;
        }
        return null;
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (Screen parent) -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("[AutoMeow] Settings"));

            // Save callback when user clicks "Done"
            builder.setSavingRunnable(ModConfig::save);

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
            ConfigEntryBuilder eb = builder.entryBuilder();

            // Enabled toggle
            general.addEntry(
                    eb.startBooleanToggle(Text.literal("Enabled"), ModState.ENABLED.get())
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> ModState.ENABLED.set(val))
                            .setTooltip(Text.literal("Master switch for AutoMeow"))
                            .build()
            );

            // Chroma toggle (only useful if Skyhanni is present)
            boolean canChroma = ChromaHelper.hasSkyhanni();
            AbstractConfigListEntry<Boolean> chromaEntry =
                    eb.startBooleanToggle(Text.literal("Chroma badge (Skyhanni)"),
                                    ModState.CHROMA_WANTED.get() && canChroma)
                            .setDefaultValue(false)
                            .setSaveConsumer(val -> ModState.CHROMA_WANTED.set(val && canChroma))
                            .setTooltip(
                                    Text.literal("Uses Skyhanni's chroma_text pack for an animated [AutoMeow] badge.")
                                            .append(Text.literal("\nRequires Skyhanni installed with its own chroma enabled."))
                                                    .formatted(Formatting.GRAY)
                                            .append(Text.literal(canChroma ? "" : "\n(Not available: Skyhanni not installed)")
                                                    .formatted(Formatting.RED))
                            )
                            .build();
            chromaEntry.setEditable(canChroma);
            general.addEntry(chromaEntry);

            // My messages required (0..10; 0 disables)
            general.addEntry(
                    eb.startIntSlider(Text.literal("My messages required (0 disables)"),
                                    ModState.MY_MESSAGES_REQUIRED, 0, 10)
                            .setDefaultValue(3)
                            .setSaveConsumer(val -> ModState.MY_MESSAGES_REQUIRED = val)
                            .setTooltip(Text.literal("How many of YOUR chat messages must be sent\nbefore AutoMeow can reply again."))
                            .build()
            );

            // Quiet window after send (ms)
            general.addEntry(
                    eb.startIntField(Text.literal("Quiet window after send (ms)"),
                                    (int) ModState.QUIET_AFTER_SEND_MS)
                            .setDefaultValue(3500)
                            .setMin(0)
                            .setMax(20000)
                            .setSaveConsumer(val ->
                                    ModState.QUIET_AFTER_SEND_MS = (long) Math.max(0, Math.min(val, 20000)))
                            .setTooltip(Text.literal("Ignore immediate echoes for this many milliseconds\n"
                                    + "after the bot (or you) sends a message."))
                            .build()
            );

            // :3 toggle
            general.addEntry(
                    eb.startBooleanToggle(Text.literal("Append \" :3\" to reply"), ModState.APPEND_FACE.get())
                            .setDefaultValue(false)
                            .setSaveConsumer(val -> { ModState.APPEND_FACE.set(val); ModConfig.save(); })
                            .setTooltip(Text.literal("When enabled, the mod sends your reply text followed by \" :3\"."))
                            .build()
            );

            general.addEntry(
                    eb.startDropdownMenu(
                                    Text.literal("Reply text"),
                                    DropdownMenuBuilder.TopCellElementBuilder.of(
                                            ModState.REPLY_TEXT,
                                            AutomeowModMenu::canonicalPresetOrNull,
                                            preset -> Text.literal(preset == null ? "" : preset)
                                    ),
                                    DropdownMenuBuilder.CellCreatorBuilder.of(
                                            14,
                                            140,
                                            8,
                                            preset -> Text.literal(preset == null ? "" : preset)
                                    )
                            )
                            .setSelections(new LinkedHashSet<>(ModState.REPLY_PRESETS))
                            .setDefaultValue(ModState.DEFAULT_REPLY_TEXT)
                            .setSaveConsumer(selectedPreset -> ModState.setReplyText(selectedPreset))
                            .build()
            );

            // Play meow sound
            general.addEntry(
                    eb.startBooleanToggle(Text.literal("Play meow sound"), ModState.PLAY_SOUND.get())
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { ModState.PLAY_SOUND.set(val); ModConfig.save(); })
                            .setTooltip(Text.literal("Plays a cat meow when someone says a cat-sound (you or others)."))
                            .build()
            );

            // Show heart particles
            general.addEntry(
                    eb.startBooleanToggle(Text.literal("Show heart particles"), ModState.HEARTS_EFFECT.get())
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { ModState.HEARTS_EFFECT.set(val); ModConfig.save(); })
                            .setTooltip(Text.literal("Spawns the hearts effect around the player who meowed."))
                            .build()
            );

            // Channels enable/disable per chat channel
            SubCategoryBuilder channelsCategory = eb.startSubCategory(Text.literal("Channels")).setExpanded(false);
            channelsCategory.add(
                    eb.startBooleanToggle(Text.literal("Public"), ModState.isChannelEnabled(HpChannel.ALL))
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.ALL, val); ModConfig.save(); })
                            .setTooltip(Text.literal("Reply to cat-sounds in public/all chat."))
                            .build()
            );
            channelsCategory.add(
                    eb.startBooleanToggle(Text.literal("Guild"), ModState.isChannelEnabled(HpChannel.GUILD))
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.GUILD, val); ModConfig.save(); })
                            .setTooltip(Text.literal("Reply to cat-sounds in guild chat."))
                            .build()
            );
            channelsCategory.add(
                    eb.startBooleanToggle(Text.literal("Party"), ModState.isChannelEnabled(HpChannel.PARTY))
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.PARTY, val); ModConfig.save(); })
                            .setTooltip(Text.literal("Reply to cat-sounds in party chat."))
                            .build()
            );
            channelsCategory.add(
                    eb.startBooleanToggle(Text.literal("Co-op"), ModState.isChannelEnabled(HpChannel.COOP))
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.COOP, val); ModConfig.save(); })
                            .setTooltip(Text.literal("Reply to cat-sounds in co-op chat."))
                            .build()
            );
            channelsCategory.add(
                    eb.startBooleanToggle(Text.literal("PM (whispers)"), ModState.isChannelEnabled(HpChannel.PM))
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.PM, val); ModConfig.save(); })
                            .setTooltip(Text.literal("Reply to cat-sounds in private messages."))
                            .build()
            );
            channelsCategory.add(
                    eb.startBooleanToggle(Text.literal("Officer"), ModState.isChannelEnabled(HpChannel.OFFICER))
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { ModState.setChannelEnabled(HpChannel.OFFICER, val); ModConfig.save(); })
                            .setTooltip(Text.literal("Reply to cat-sounds in officer chat."))
                            .build()
            );
            general.addEntry(channelsCategory.build());

            return builder.build();
        };
    }
}