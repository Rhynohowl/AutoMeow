package org.rhynohowl.automeow.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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
                                if (!ChromaHelper.hasAaronMod()) {
                                    ctx.getSource().sendFeedback(ChatUtil.badge()
                                            .append(Component.literal("Aaron-mod not found").withStyle(ChatFormatting.RED)));
                                    return 0;
                                }
                                if (!ChromaHelper.aaronChromaAvailable()) {
                                    ctx.getSource().sendFeedback(ChatUtil.badge()
                                            .append(Component.literal("Chroma pack is disabled").withStyle(ChatFormatting.YELLOW)));
                                    return 0;
                                }
                                boolean newValue = !ModState.CHROMA_WANTED.get();
                                ModState.CHROMA_WANTED.set(newValue);
                                ModConfig.save();
                                ctx.getSource().sendFeedback(ChatUtil.badge()
                                        .append(Component.literal("Chroma " + (newValue ? "ON" : "OFF"))
                                                .withStyle(newValue ? ChatFormatting.GREEN : ChatFormatting.RED)));
                                return newValue ? 1 : 0;
                            }))
                            .then(literal("debug").executes(ctx -> {
                                boolean newValue = !ModState.DEBUG.get();
                                ModState.DEBUG.set(newValue);
                                ctx.getSource().sendFeedback(
                                        ChatUtil.badge().append(Component.literal("Debug " + (newValue ? "ON" : "OFF"))
                                                .withStyle(newValue ? ChatFormatting.GREEN : ChatFormatting.RED))
                                );
                                return newValue ? 1 : 0;
                            }))
                            .then(literal("hearts")
                                    // `/automeow hearts` -> TOGGLE
                                    .executes(ctx -> {
                                        boolean newValue = !ModState.HEARTS_EFFECT.get();
                                        ModState.HEARTS_EFFECT.set(newValue);
                                        ModConfig.save();
                                        ctx.getSource().sendFeedback(
                                                ChatUtil.badge().append(Component.literal("Hearts " + (newValue ? "ON" : "OFF"))
                                                        .withStyle(newValue ? ChatFormatting.GREEN : ChatFormatting.RED)));
                                        return newValue ? 1 : 0;
                                    })
                                    .then(literal("on").executes(ctx -> {
                                        ModState.HEARTS_EFFECT.set(true); ModConfig.save();
                                        ctx.getSource().sendFeedback(ChatUtil.badge().append(Component.literal("Hearts ON").withStyle(ChatFormatting.GREEN)));
                                        return 1;
                                    }))
                                    .then(literal("off").executes(ctx -> {
                                        ModState.HEARTS_EFFECT.set(false); ModConfig.save();
                                        ctx.getSource().sendFeedback(ChatUtil.badge().append(Component.literal("Hearts OFF").withStyle(ChatFormatting.RED)));
                                        return 1;
                                    }))
                            )
                            .then(literal("sound")
                                    // `/automeow sound` -> TOGGLE
                                    .executes(ctx -> {
                                        boolean newValue = !ModState.PLAY_SOUND.get();
                                        ModState.PLAY_SOUND.set(newValue);
                                        ModConfig.save();
                                        ctx.getSource().sendFeedback(
                                                ChatUtil.badge().append(Component.literal("Cat Sound " + (newValue ? "ON" : "OFF"))
                                                        .withStyle(newValue ? ChatFormatting.GREEN : ChatFormatting.RED)));
                                        return newValue ? 1 : 0;
                                    })
                                    .then(literal("on").executes(ctx -> {
                                        ModState.PLAY_SOUND.set(true); ModConfig.save();
                                        ctx.getSource().sendFeedback(ChatUtil.badge().append(Component.literal("Cat Sound ON").withStyle(ChatFormatting.GREEN)));
                                        return 1;
                                    }))
                                    .then(literal("off").executes(ctx -> {
                                        ModState.PLAY_SOUND.set(false); ModConfig.save();
                                        ctx.getSource().sendFeedback(ChatUtil.badge().append(Component.literal("Cat Sound OFF").withStyle(ChatFormatting.RED)));
                                        return 1;
                                    }))
                            )
                            .then(literal("face")
                                    // `/automeow face` -> TOGGLE
                                    .executes(ctx -> {
                                        boolean newValue = !ModState.APPEND_FACE.get();
                                        ModState.APPEND_FACE.set(newValue);
                                        ModConfig.save();
                                        ctx.getSource().sendFeedback(
                                                ChatUtil.badge().append(Component.literal("Cat Face " + (newValue ? "ON" : "OFF"))
                                                        .withStyle(newValue ? ChatFormatting.GREEN : ChatFormatting.RED)));
                                        return newValue ? 1 : 0;
                                    })
                                    .then(literal("on").executes(ctx -> {
                                        ModState.APPEND_FACE.set(true); ModConfig.save();
                                        ctx.getSource().sendFeedback(ChatUtil.badge().append(Component.literal("Cat Face ON").withStyle(ChatFormatting.GREEN)));
                                        return 1;
                                    }))
                                    .then(literal("off").executes(ctx -> {
                                        ModState.APPEND_FACE.set(false); ModConfig.save();
                                        ctx.getSource().sendFeedback(ChatUtil.badge().append(Component.literal("Cat Face OFF").withStyle(ChatFormatting.RED)));
                                        return 1;
                                    }))
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
                                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands
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
            );
        });
    }
}