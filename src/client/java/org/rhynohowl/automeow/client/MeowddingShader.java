package org.rhynohowl.automeow.client;

import me.owdding.lib.rendering.text.PrideShader;
import me.owdding.lib.rendering.text.TextShaderKt;
import me.owdding.lib.rendering.text.builtin.GradientTextShader;
import net.minecraft.network.chat.Style;
import java.util.ArrayList;
import java.util.List;

public final class MeowddingShader {
    public static List<String> presetNames() {
        List<String> names = new ArrayList<>();
        for (PrideShader preset : PrideShader.values()) {
            names.add(preset.name());
        }
        return names;
    }

    public static Style withGradient(Style style) {
        List<Integer> colours = presetColours();
        if (colours == null) {
            colours = customColours();
        }
        if (colours.size() < 2) return style;

        GradientTextShader shader = new GradientTextShader(
                colours,
                GradientTextShader.Direction.valueOf(ModState.GRADIENT_DIRECTION),
                ModState.GRADIENT_SPEED
        );
        return TextShaderKt.withTextShader(style, shader);
    }

    private static List<Integer> presetColours() {
        if (ModState.CUSTOM_GRADIENT_PRESET.equals(ModState.GRADIENT_PRESET)) return null;
        for (PrideShader preset : PrideShader.values()) {
            if (preset.name().equals(ModState.GRADIENT_PRESET)) return new ArrayList<>(preset.getColors());
        }
        return null;
    }

    private static List<Integer> customColours() {
        List<Integer> colours = new ArrayList<>();
        for (String hex : ModState.GRADIENT_COLOURS) {
            Integer parsed = ChatUtil.parseHexColour(hex);
            if (parsed != null) colours.add(parsed);
        }
        if (colours.size() >= 2 && ModState.GRADIENT_LOOP.get()) colours.add(colours.get(0));
        return colours;
    }
}