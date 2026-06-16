/*
package org.rhynohowl.automeow.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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

            // Chroma toggle (only useful if Aaron-mod is present)
            boolean canChroma = ChromaHelper.aaronChromaAvailable();
            AbstractConfigListEntry<Boolean> chromaEntry =
                    eb.startBooleanToggle(Component.literal("Chroma badge (Aaron-mod)"),
                                    ModState.CHROMA_WANTED.get() && canChroma)
                            .setDefaultValue(false)
                            .setSaveConsumer(val -> ModState.CHROMA_WANTED.set(val && canChroma))
                            .setTooltip(
                                    Component.literal("Uses Aaron-mod's chroma_text pack for an animated [AutoMeow] badge.")
                                            .append(Component.literal(canChroma ? "" : "\n(Not available: Aaron-mod or pack disabled)")
                                                    .withStyle(ChatFormatting.GRAY))
                            )
                            .build();
            chromaEntry.setEditable(canChroma);
            general.addEntry(chromaEntry);

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
                            .setDefaultValue(false)
                            .setSaveConsumer(val -> { ModState.APPEND_FACE.set(val); ModConfig.save(); })
                            .setTooltip(Component.literal("When enabled, the mod sends your reply text followed by \" :3\"."))
                            .build()
            );

            general.addEntry(
                    eb.startDropdownMenu(
                                    Component.literal("Reply text"),
                                    DropdownMenuBuilder.TopCellElementBuilder.of(
                                            ModState.REPLY_TEXT,
                                            AutomeowModMenu::canonicalPresetOrNull,
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

            return builder.build();
        };
    }
}
*/
