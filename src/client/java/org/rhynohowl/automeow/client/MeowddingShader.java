package org.rhynohowl.automeow.client;

//? if >=26.1 {
/*import me.owdding.lib.rendering.text.TextShaderKt;
import me.owdding.lib.rendering.text.builtin.GradientTextShader;
import net.minecraft.text.Style;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class MeowddingShader {
    private static final String PRIDE_SHADER_CLASS = "me.owdding.lib.rendering.text.PrideShader";

    private static Object[] prideShaders() {
        try {
            Object[] constants = Class.forName(PRIDE_SHADER_CLASS).getEnumConstants();
            return constants != null ? constants : new Object[0];
        } catch (Throwable ignored) {
            return new Object[0];
        }
    }

    public static List<String> presetNames() {
        List<String> names = new ArrayList<>();
        for (Object preset : prideShaders()) {
            names.add(((Enum<?>) preset).name());
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

    @SuppressWarnings("unchecked")
    private static List<Integer> presetColours() {
        if (ModState.CUSTOM_GRADIENT_PRESET.equals(ModState.GRADIENT_PRESET)) return null;
        for (Object preset : prideShaders()) {
            if (!((Enum<?>) preset).name().equals(ModState.GRADIENT_PRESET)) continue;
            try {
                Method getColors = preset.getClass().getMethod("getColors");
                return new ArrayList<>((List<Integer>) getColors.invoke(preset));
            } catch (Throwable ignored) {
                return null;
            }
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
*///?}