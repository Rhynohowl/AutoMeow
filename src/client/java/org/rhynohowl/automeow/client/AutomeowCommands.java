package org.rhynohowl.automeow.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class AutomeowCommands {
    public static void register() {
        // /automeow status & toggle, im not commenting all of this. work it out yourself
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
        {
            dispatcher.register(
                    literal("automeow")
                            .executes(ctx -> {
                                boolean on = ModState.ENABLED.get();
                                ctx.getSource().sendFeedback(ChatUtil.statusLine(on));
                                return on ? 1 : 0;
                            })
                            .then(literal("toggle").executes(ctx -> {
                                boolean newValue = !ModState.ENABLED.get();
                                ModState.ENABLED.set(newValue);
                                ModConfig.save();
                                ctx.getSource().sendFeedback(ChatUtil.statusLine(newValue));
                                return newValue ? 1 : 0;
                            }))
                            .then(literal("on").executes(ctx -> {
                                ModState.ENABLED.set(true);
                                ModConfig.save();
                                ctx.getSource().sendFeedback(ChatUtil.statusLine(true));
                                return 1;
                            }))
                            .then(literal("off").executes(ctx -> {
                                ModState.ENABLED.set(false);
                                ModConfig.save();
                                ctx.getSource().sendFeedback(ChatUtil.statusLine(false));
                                return 1;
                            }))
                            .then(literal("chroma").executes(ctx -> {
                                if (!ChromaHelper.hasSkyhanni()) {
                                    ctx.getSource().sendFeedback(ChatUtil.badge()
                                            .append(Component.literal("SkyHanni not found").withStyle(ChatFormatting.RED)));
                                    return 0;
                                }
                                boolean newValue = !ModState.CHROMA_WANTED.get();
                                ModState.setBadgeStyle(newValue ? ModState.BADGE_CHROMA : ModState.BADGE_DEFAULT);
                                ModConfig.save();
                                ctx.getSource().sendFeedback(ChatUtil.badge()
                                        .append(Component.literal("Chroma " + (newValue ? "ON" : "OFF"))
                                                .withStyle(newValue ? ChatFormatting.GREEN : ChatFormatting.RED)));
                                return newValue ? 1 : 0;
                            }))
                            .then(literal("gradient")
                                    .executes(ctx -> {
                                        if (!MeowddingHelper.hasMeowdding()) {
                                            ctx.getSource().sendFeedback(ChatUtil.badge()
                                                    .append(Component.literal(MeowddingHelper.supportedVersion()
                                                            ? "MeowddingLib not found"
                                                            : "Gradient not available on this game version").withStyle(ChatFormatting.RED)));
                                            return 0;
                                        }
                                        boolean newValue = !ModState.GRADIENT_WANTED.get();
                                        ModState.setBadgeStyle(newValue ? ModState.BADGE_GRADIENT : ModState.BADGE_DEFAULT);
                                        ModConfig.save();
                                        ctx.getSource().sendFeedback(ChatUtil.badge()
                                                .append(Component.literal("Gradient " + (newValue ? "ON" : "OFF"))
                                                        .withStyle(newValue ? ChatFormatting.GREEN : ChatFormatting.RED)));
                                        return newValue ? 1 : 0;
                                    })
                                    .then(literal("on").executes(ctx -> {
                                        if (!MeowddingHelper.hasMeowdding()) {
                                            ctx.getSource().sendFeedback(ChatUtil.badge()
                                                    .append(Component.literal(MeowddingHelper.supportedVersion()
                                                            ? "MeowddingLib not found"
                                                            : "Gradient not available on this game version").withStyle(ChatFormatting.RED)));
                                            return 0;
                                        }
                                        ModState.setBadgeStyle(ModState.BADGE_GRADIENT); ModConfig.save();
                                        ctx.getSource().sendFeedback(ChatUtil.badge().append(Component.literal("Gradient ON").withStyle(ChatFormatting.GREEN)));
                                        return 1;
                                    }))
                                    .then(literal("off").executes(ctx -> {
                                        ModState.GRADIENT_WANTED.set(false); ModConfig.save();
                                        ctx.getSource().sendFeedback(ChatUtil.badge().append(Component.literal("Gradient OFF").withStyle(ChatFormatting.RED)));
                                        return 1;
                                    }))
                                    .then(literal("preset")
                                            .then(ClientCommands
                                                    .argument("preset", StringArgumentType.word())
                                                    .suggests((ctx, suggestion) -> {
                                                        for (String opt : ModState.gradientPresetOptions()) suggestion.suggest(opt.toLowerCase(Locale.ROOT));
                                                        return suggestion.buildFuture();
                                                    })
                                                    .executes(ctx -> {
                                                        java.util.List<String> options = ModState.gradientPresetOptions();
                                                        String matched = ModState.matchOption(StringArgumentType.getString(ctx, "preset"), options);

                                                        if (matched != null) {
                                                            ModState.setGradientPreset(matched);
                                                            ModConfig.save();
                                                            ctx.getSource().sendFeedback(
                                                                    ChatUtil.badge().append(Component.literal("Gradient preset set to " + matched.replace('_', ' '))
                                                                            .withStyle(ChatFormatting.GREEN))
                                                            );
                                                            return 1;
                                                        }

                                                        ctx.getSource().sendFeedback(
                                                                ChatUtil.badge().append(Component.literal("Invalid preset. Choose one of: " + String.join(", ", options).toLowerCase(Locale.ROOT))
                                                                        .withStyle(ChatFormatting.RED))
                                                        );
                                                        return 0;
                                                    })
                                            )
                                    )
                                    .then(literal("direction")
                                            .then(ClientCommands
                                                    .argument("direction", StringArgumentType.word())
                                                    .suggests((ctx, suggestion) -> {
                                                        for (String opt : ModState.GRADIENT_DIRECTIONS) suggestion.suggest(opt.toLowerCase(Locale.ROOT));
                                                        return suggestion.buildFuture();
                                                    })
                                                    .executes(ctx -> {
                                                        String matched = ModState.matchOption(StringArgumentType.getString(ctx, "direction"), ModState.GRADIENT_DIRECTIONS);

                                                        if (matched != null) {
                                                            ModState.setGradientDirection(matched);
                                                            ModConfig.save();
                                                            ctx.getSource().sendFeedback(
                                                                    ChatUtil.badge().append(Component.literal("Gradient direction set to " + matched.replace('_', ' '))
                                                                            .withStyle(ChatFormatting.GREEN))
                                                            );
                                                            return 1;
                                                        }

                                                        ctx.getSource().sendFeedback(
                                                                ChatUtil.badge().append(Component.literal("Invalid direction. Choose one of: " + String.join(", ", ModState.GRADIENT_DIRECTIONS).toLowerCase(Locale.ROOT))
                                                                        .withStyle(ChatFormatting.RED))
                                                        );
                                                        return 0;
                                                    })
                                            )
                                    )
                                    .then(literal("speed")
                                            .then(ClientCommands
                                                    .argument("speed", FloatArgumentType.floatArg(ModState.GRADIENT_SPEED_MIN, ModState.GRADIENT_SPEED_MAX))
                                                    .executes(ctx -> {
                                                        ModState.setGradientSpeed(FloatArgumentType.getFloat(ctx, "speed"));
                                                        ModConfig.save();
                                                        ctx.getSource().sendFeedback(
                                                                ChatUtil.badge().append(Component.literal("Gradient speed set to " + ModState.GRADIENT_SPEED)
                                                                        .withStyle(ChatFormatting.GREEN))
                                                        );
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(literal("colours")
                                            .then(ClientCommands
                                                    .argument("colours", StringArgumentType.greedyString())
                                                    .executes(ctx -> {
                                                        java.util.List<String> colours = new ArrayList<>();
                                                        for (String hex : StringArgumentType.getString(ctx, "colours").split("[\\s,]+")) {
                                                            if (hex.isEmpty()) continue;
                                                            if (ChatUtil.parseHexColour(hex) == null) {
                                                                ctx.getSource().sendFeedback(
                                                                        ChatUtil.badge().append(Component.literal("\"" + hex + "\" is not a hex colour (e.g. FF66B2 or #FF3399)")
                                                                                .withStyle(ChatFormatting.RED))
                                                                );
                                                                return 0;
                                                            }
                                                            colours.add(hex);
                                                        }

                                                        if (colours.size() < 2) {
                                                            ctx.getSource().sendFeedback(
                                                                    ChatUtil.badge().append(Component.literal("Needs at least 2 colours")
                                                                            .withStyle(ChatFormatting.RED))
                                                            );
                                                            return 0;
                                                        }

                                                        ModState.setGradientColours(colours);
                                                        ModState.setGradientPreset(ModState.CUSTOM_GRADIENT_PRESET);
                                                        ModConfig.save();
                                                        ctx.getSource().sendFeedback(
                                                                ChatUtil.badge().append(Component.literal("Gradient colours set to " + String.join(", ", colours) + " (preset: custom)")
                                                                        .withStyle(ChatFormatting.GREEN))
                                                        );
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(toggle("loop", ModState.GRADIENT_LOOP, "Gradient Loop"))
                            )
                            .then(literal("debug").executes(ctx -> {
                                boolean newValue = !ModState.DEBUG.get();
                                ModState.DEBUG.set(newValue);
                                ctx.getSource().sendFeedback(
                                        ChatUtil.badge().append(Component.literal("Debug " + (newValue ? "ON" : "OFF"))
                                                .withStyle(newValue ? ChatFormatting.GREEN : ChatFormatting.RED))
                                );
                                return newValue ? 1 : 0;
                            }))
                            // `/automeow hearts` -> TOGGLE
                            .then(toggle("hearts", ModState.HEARTS_EFFECT, "Hearts"))
                            .then(literal("catfact")
                                    .then(toggle("party", ModState.CATFACT_PARTY, "Cat Fact (Party)"))
                                    .then(toggle("guild", ModState.CATFACT_GUILD, "Cat Fact (Guild)"))
                                    .then(toggle("pm", ModState.CATFACT_PM, "Cat Fact (PM)"))
                                    .then(toggle("OnlyOwn", ModState.CATFACT_SELF_ONLY, "Cat Fact (OnlyOwn)"))
                            )
                            // `/automeow sound` -> TOGGLE
                            .then(toggle("sound", ModState.PLAY_SOUND, "Cat Sound"))
                            // `/automeow face` -> TOGGLE
                            .then(toggle("face", ModState.APPEND_FACE, "Cat Face"))
                            .then(literal("channels")
                                    // `/automeow channels` -> status overview
                                    .executes(ctx -> {
                                        ctx.getSource().sendFeedback(ChatUtil.channelsStatusLine());
                                        return 1;
                                    })
                                    .then(channelToggle("public", HpChannel.ALL))
                                    .then(channelToggle("guild", HpChannel.GUILD))
                                    .then(channelToggle("party", HpChannel.PARTY))
                                    .then(channelToggle("coop", HpChannel.COOP))
                                    .then(channelToggle("pm", HpChannel.PM))
                                    .then(channelToggle("officer", HpChannel.OFFICER))
                            )
                            .then(literal("stats").executes(ctx -> {
                                ctx.getSource().sendFeedback(
                                        ChatUtil.badge()
                                                .append(Component.literal("Total meows sent: ").withStyle(ChatFormatting.WHITE))
                                                .append(Component.literal(String.valueOf(ModConfig.CONFIG.totalReplies)).withStyle(ChatFormatting.AQUA))

                                );
                                return 1;
                            }))
                            .then(literal("say")
                                    .then(ClientCommands
                                            .argument("preset", StringArgumentType.greedyString())
                                            .suggests((ctx, suggestion) -> {
                                                for (String opt : ModState.REPLY_PRESETS) suggestion.suggest(opt);
                                                return suggestion.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                String wanted = StringArgumentType.getString(ctx, "preset");
                                                boolean ok = ModState.setReplyText(wanted);

                                                if (ok) {
                                                    ModConfig.save();
                                                    ctx.getSource().sendFeedback(
                                                            ChatUtil.badge().append(Component.literal("reply preset set to \"" + ModState.REPLY_TEXT + "\"")
                                                                    .withStyle(ChatFormatting.GREEN))
                                                    );
                                                    return 1;
                                                }

                                                ctx.getSource().sendFeedback(
                                                        ChatUtil.badge().append(Component.literal("Invalid preset. Choose one of: " + String.join(", ", ModState.REPLY_PRESETS))
                                                                .withStyle(ChatFormatting.RED))
                                                );
                                                return 0;
                                            })
                                    )
                            )
                            .then(literal("mode")
                                    .executes(ctx -> {
                                        ctx.getSource().sendFeedback(
                                                ChatUtil.badge().append(Component.literal("Reply mode: " + ModState.replyModeName(ModState.REPLY_MODE))
                                                        .withStyle(ChatFormatting.GRAY))
                                        );
                                        return 1;
                                    })
                                    .then(ClientCommands
                                            .argument("mode", StringArgumentType.word())
                                            .suggests((ctx, suggestion) -> {
                                                for (String opt : ModState.REPLY_MODES) suggestion.suggest(opt.toLowerCase(Locale.ROOT));
                                                return suggestion.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                String matched = ModState.matchOption(StringArgumentType.getString(ctx, "mode"), ModState.REPLY_MODES, ModState::replyModeName);

                                                if (matched == null) {
                                                    ctx.getSource().sendFeedback(
                                                            ChatUtil.badge().append(Component.literal("Invalid mode. Choose one of: " + String.join(", ", ModState.REPLY_MODES).toLowerCase(Locale.ROOT))
                                                                    .withStyle(ChatFormatting.RED))
                                                    );
                                                    return 0;
                                                }

                                                if (matched.equals(ModState.REPLY_MODE_CUSTOM) && ModState.CUSTOM_REPLIES.size() < ModState.MIN_CUSTOM_REPLIES) {
                                                    ctx.getSource().sendFeedback(
                                                            ChatUtil.badge().append(Component.literal("Custom needs at least " + ModState.MIN_CUSTOM_REPLIES + " sounds. Set them with /automeow custom <sounds>")
                                                                    .withStyle(ChatFormatting.RED))
                                                    );
                                                    return 0;
                                                }

                                                ModState.setReplyMode(matched);
                                                ModConfig.save();
                                                ctx.getSource().sendFeedback(
                                                        ChatUtil.badge().append(Component.literal("Reply mode set to " + ModState.replyModeName(matched))
                                                                .withStyle(ChatFormatting.GREEN))
                                                );
                                                return 1;
                                            })
                                    )
                            )
                            .then(literal("custom")
                                    .executes(ctx -> {
                                        ctx.getSource().sendFeedback(
                                                ChatUtil.badge().append(Component.literal("Custom sounds: " + String.join(", ", ModState.CUSTOM_REPLIES))
                                                        .withStyle(ChatFormatting.GRAY))
                                        );
                                        return 1;
                                    })
                                    .then(ClientCommands
                                            .argument("sounds", StringArgumentType.greedyString())
                                            .suggests((ctx, suggestion) -> {
                                                String typed = suggestion.getRemaining();
                                                int lastSpace = Math.max(typed.lastIndexOf(' '), typed.lastIndexOf(','));
                                                String before = typed.substring(0, lastSpace + 1);
                                                for (String opt : ModState.REPLY_PRESETS) suggestion.suggest(before + opt);
                                                return suggestion.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                java.util.LinkedHashSet<String> sounds = new java.util.LinkedHashSet<>();
                                                for (String sound : StringArgumentType.getString(ctx, "sounds").split("[\\s,]+")) {
                                                    if (sound.isEmpty()) continue;
                                                    if (ModState.matchOption(sound, ModState.REPLY_PRESETS) == null) {
                                                        ctx.getSource().sendFeedback(
                                                                ChatUtil.badge().append(Component.literal("\"" + sound + "\" is not a reply preset. Choose from: " + String.join(", ", ModState.REPLY_PRESETS))
                                                                        .withStyle(ChatFormatting.RED))
                                                        );
                                                        return 0;
                                                    }
                                                    sounds.add(ModState.matchOption(sound, ModState.REPLY_PRESETS));
                                                }

                                                if (sounds.size() < ModState.MIN_CUSTOM_REPLIES) {
                                                    ctx.getSource().sendFeedback(
                                                            ChatUtil.badge().append(Component.literal("Needs at least " + ModState.MIN_CUSTOM_REPLIES + " different sounds")
                                                                    .withStyle(ChatFormatting.RED))
                                                    );
                                                    return 0;
                                                }

                                                ModState.setCustomReplies(new ArrayList<>(sounds));
                                                ModState.setReplyMode(ModState.REPLY_MODE_CUSTOM);
                                                ModConfig.save();
                                                ctx.getSource().sendFeedback(
                                                        ChatUtil.badge().append(Component.literal("Custom sounds set to " + String.join(", ", ModState.CUSTOM_REPLIES) + " (mode: custom)")
                                                                .withStyle(ChatFormatting.GREEN))
                                                );
                                                return 1;
                                            })
                                    )
                            )
            );
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> toggle(String name, AtomicBoolean setting, String label) {
        return literal(name)
                .executes(ctx -> setToggle(ctx, setting, !setting.get(), label))
                .then(literal("on").executes(ctx -> setToggle(ctx, setting, true, label)))
                .then(literal("off").executes(ctx -> setToggle(ctx, setting, false, label)));
    }

    private static int setToggle(CommandContext<FabricClientCommandSource> ctx, AtomicBoolean setting, boolean value, String label) {
        setting.set(value);
        ModConfig.save();
        ctx.getSource().sendFeedback(ChatUtil.badge()
                .append(Component.literal(label + " " + (value ? "ON" : "OFF"))
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED)));
        return value ? 1 : 0;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> channelToggle(String name, HpChannel channel) {
        return literal(name).executes(ctx -> {
            boolean newValue = !ModState.isChannelEnabled(channel);
            ModState.setChannelEnabled(channel, newValue);
            ModConfig.save();
            ctx.getSource().sendFeedback(ChatUtil.badge()
                    .append(Component.literal(channel.displayName() + " " + (newValue ? "ENABLED" : "DISABLED"))
                            .withStyle(newValue ? ChatFormatting.GREEN : ChatFormatting.RED)));
            return newValue ? 1 : 0;
        });
    }
}