package org.rhynohowl.automeow.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AutomeowModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (Screen parent) -> {
            // Build the screen
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("[AutoMeow] Settings"));

            // Save callback (called when the user clicks "Done")
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

            // Visually disable when chroma is not available
            chromaEntry.setEditable(canChroma);
            general.addEntry(chromaEntry);

            // My messages required (slider 1..10)
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
                                    + "after the bot (or you) sends a meow."))
                            .build()
            );

            return builder.build();
        };
    }

    private static void saveToDisk() {
        // Persist to your existing JSON config
        AutomeowClient.saveConfig();
    }
}
