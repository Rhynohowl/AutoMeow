package org.rhynohowl.automeow.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.LinkedHashSet;



public class AutomeowModMenu implements ModMenuApi {

    private static String canonicalPresetOrNull(String typed) {
        if (typed == null) return null;
        String trimed = typed.trim();
        if (trimed.isEmpty()) return null;

        for (String option : AutomeowClient.REPLY_PRESETS) {
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
            builder.setSavingRunnable(AutomeowModMenu::saveToDisk);

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
            ConfigEntryBuilder eb = builder.entryBuilder();

            // Enabled toggle
            general.addEntry(
                    eb.startBooleanToggle(Text.literal("Enabled"), AutomeowClient.ENABLED.get())
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> AutomeowClient.ENABLED.set(val))
                            .setTooltip(Text.literal("Master switch for AutoMeow"))
                            .build()
            );

            // Chroma toggle (only useful if Aaron-mod is present)
            boolean canChroma = AutomeowClient.aaronChromaAvailable();
            AbstractConfigListEntry<Boolean> chromaEntry =
                    eb.startBooleanToggle(Text.literal("Chroma badge (Aaron-mod)"),
                                    AutomeowClient.CHROMA_WANTED.get() && canChroma)
                            .setDefaultValue(false)
                            .setSaveConsumer(val -> AutomeowClient.CHROMA_WANTED.set(val && canChroma))
                            .setTooltip(
                                    Text.literal("Uses Aaron-mod's chroma_text pack for an animated [AutoMeow] badge.")
                                            .append(Text.literal(canChroma ? "" : "\n(Not available: Aaron-mod or pack disabled)")
                                                    .formatted(Formatting.GRAY))
                            )
                            .build();
            chromaEntry.setEditable(canChroma);
            general.addEntry(chromaEntry);

            // My messages required (0..10; 0 disables)
            general.addEntry(
                    eb.startIntSlider(Text.literal("My messages required (0 disables)"),
                                    AutomeowClient.MY_MESSAGES_REQUIRED, 0, 10)
                            .setDefaultValue(3)
                            .setSaveConsumer(val -> AutomeowClient.MY_MESSAGES_REQUIRED = val)
                            .setTooltip(Text.literal("How many of YOUR chat messages must be sent\nbefore AutoMeow can reply again."))
                            .build()
            );

            // Quiet window after send (ms)
            general.addEntry(
                    eb.startIntField(Text.literal("Quiet window after send (ms)"),
                                    (int) AutomeowClient.QUIET_AFTER_SEND_MS)
                            .setDefaultValue(3500)
                            .setMin(0)
                            .setMax(20000)
                            .setSaveConsumer(val ->
                                    AutomeowClient.QUIET_AFTER_SEND_MS = (long) Math.max(0, Math.min(val, 20000)))
                            .setTooltip(Text.literal("Ignore immediate echoes for this many milliseconds\n"
                                    + "after the bot (or you) sends a message."))
                            .build()
            );

            // :3 toggle
            general.addEntry(
                    eb.startBooleanToggle(Text.literal("Append \" :3\" to reply"), AutomeowClient.APPEND_FACE.get())
                            .setDefaultValue(false)
                            .setSaveConsumer(val -> { AutomeowClient.APPEND_FACE.set(val); AutomeowClient.saveConfig(); })
                            .setTooltip(Text.literal("When enabled, the mod sends your reply text followed by \" :3\"."))
                            .build()
            );

            general.addEntry(
                    eb.startDropdownMenu(
                                    Text.literal("Reply text"),
                                    DropdownMenuBuilder.TopCellElementBuilder.of(
                                            AutomeowClient.REPLY_TEXT,
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
                            .setSelections(new LinkedHashSet<>(AutomeowClient.REPLY_PRESETS))
                            .setDefaultValue(AutomeowClient.DEFAULT_REPLY_TEXT)
                            .setSaveConsumer(v -> AutomeowClient.setReplyText(v))
                            .build()
            );

            // Play meow sound
            general.addEntry(
                    eb.startBooleanToggle(Text.literal("Play meow sound"), AutomeowClient.PLAY_SOUND.get())
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { AutomeowClient.PLAY_SOUND.set(val); AutomeowClient.saveConfig(); })
                            .setTooltip(Text.literal("Plays a cat meow when someone says a cat-sound (you or others)."))
                            .build()
            );

            // Show heart particles
            general.addEntry(
                    eb.startBooleanToggle(Text.literal("Show heart particles"), AutomeowClient.HEARTS_EFFECT.get())
                            .setDefaultValue(true)
                            .setSaveConsumer(val -> { AutomeowClient.HEARTS_EFFECT.set(val); AutomeowClient.saveConfig(); })
                            .setTooltip(Text.literal("Spawns the hearts effect around the player who meowed."))
                            .build()
            );

            return builder.build();
        };
    }

    private static void saveToDisk() {
        AutomeowClient.saveConfig();
    }
}
