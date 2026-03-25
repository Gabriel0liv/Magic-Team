package com.gabri.magicteam;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class MagicTeamCommands {
    @SuppressWarnings("null")
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("magicteam")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("status")
                .executes(context -> {
                    sendStatus(context.getSource());
                    return 1;
                })
            )
            .then(Commands.literal("list")
                 .executes(context -> {
                     sendStatus(context.getSource());
                     return 1;
                 })
             )
            .then(Commands.literal("filter")
                .then(Commands.literal("add")
                    .then(Commands.literal("beneficial")
                        .then(Commands.argument("spell", StringArgumentType.string())
                            .executes(context -> addFilter(context.getSource(), "beneficial", StringArgumentType.getString(context, "spell")))
                        )
                    )
                    .then(Commands.literal("harmful")
                        .then(Commands.argument("spell", StringArgumentType.string())
                            .executes(context -> addFilter(context.getSource(), "harmful", StringArgumentType.getString(context, "spell")))
                        )
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.literal("beneficial")
                        .then(Commands.argument("spell", StringArgumentType.string())
                            .executes(context -> removeFilter(context.getSource(), "beneficial", StringArgumentType.getString(context, "spell")))
                        )
                    )
                    .then(Commands.literal("harmful")
                        .then(Commands.argument("spell", StringArgumentType.string())
                            .executes(context -> removeFilter(context.getSource(), "harmful", StringArgumentType.getString(context, "spell")))
                        )
                    )
                )
            )
            .then(Commands.literal("bypass")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                        return toggleBypass(context.getSource(), player);
                    } else {
                        context.getSource().sendFailure(Component.literal("Only players can use this without arguments. Use /magicteam bypass <player>"));
                        return 0;
                    }
                })
                .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                    .executes(context -> toggleBypass(context.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "target")))
                )
            )
            .then(Commands.literal("targeting")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        MagicTeamConfig.SERVER.enableTargetingBlock.set(enabled);
                        context.getSource().sendSuccess(() -> Component.translatable("magic_team.command.status.targeting").append(enabled ? Component.translatable("magic_team.command.status.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("magic_team.command.status.disabled").withStyle(ChatFormatting.RED)), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("damage")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        MagicTeamConfig.SERVER.enableDamageBlock.set(enabled);
                        context.getSource().sendSuccess(() -> Component.translatable("magic_team.command.status.damage").append(enabled ? Component.translatable("magic_team.command.status.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("magic_team.command.status.disabled").withStyle(ChatFormatting.RED)), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("effects")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        MagicTeamConfig.SERVER.enableEffectBlock.set(enabled);
                        context.getSource().sendSuccess(() -> Component.translatable("magic_team.command.status.effects").append(enabled ? Component.translatable("magic_team.command.status.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("magic_team.command.status.disabled").withStyle(ChatFormatting.RED)), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("alliance")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean enabled = BoolArgumentType.getBool(context, "enabled");
                        MagicTeamConfig.SERVER.enableGlobalAlliance.set(enabled);
                        context.getSource().sendSuccess(() -> Component.translatable("magic_team.command.status.alliance").append(enabled ? Component.translatable("magic_team.command.status.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("magic_team.command.status.disabled").withStyle(ChatFormatting.RED)), true);
                        return 1;
                    })
                )
            )
        );
    }

    @SuppressWarnings("null")
    private static void sendStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("magic_team.command.status.header").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.translatable("magic_team.command.status.targeting").append(MagicTeamConfig.SERVER.enableTargetingBlock.get() ? Component.translatable("magic_team.command.status.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("magic_team.command.status.disabled").withStyle(ChatFormatting.RED)), false);
        source.sendSuccess(() -> Component.translatable("magic_team.command.status.damage").append(MagicTeamConfig.SERVER.enableDamageBlock.get() ? Component.translatable("magic_team.command.status.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("magic_team.command.status.disabled").withStyle(ChatFormatting.RED)), false);
        source.sendSuccess(() -> Component.translatable("magic_team.command.status.effects").append(MagicTeamConfig.SERVER.enableEffectBlock.get() ? Component.translatable("magic_team.command.status.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("magic_team.command.status.disabled").withStyle(ChatFormatting.RED)), false);
        source.sendSuccess(() -> Component.translatable("magic_team.command.status.alliance").append(MagicTeamConfig.SERVER.enableGlobalAlliance.get() ? Component.translatable("magic_team.command.status.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("magic_team.command.status.disabled").withStyle(ChatFormatting.RED)), false);
        
        int beneficial = MagicTeamConfig.SERVER.beneficialSpells.get().size();
        int harmful = MagicTeamConfig.SERVER.explicitHarmfulSpells.get().size();
        source.sendSuccess(() -> Component.translatable("magic_team.command.status.filters_loaded").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(beneficial)).append(Component.translatable("magic_team.command.status.filters_beneficial")).append(Component.literal(String.valueOf(harmful))).append(Component.translatable("magic_team.command.status.filters_harmful")).withStyle(ChatFormatting.YELLOW)), false);
    }

    @SuppressWarnings({"null", "unchecked"})
    private static int addFilter(CommandSourceStack source, String listType, String spell) {
        var configList = listType.equals("beneficial") ? MagicTeamConfig.SERVER.beneficialSpells : MagicTeamConfig.SERVER.explicitHarmfulSpells;
        java.util.List<String> current = new java.util.ArrayList<>((java.util.Collection<? extends String>) configList.get());
        
        Component filterName = Component.translatable(listType.equals("beneficial") ? "magic_team.command.filter.beneficial" : "magic_team.command.filter.harmful");
        
        if (!current.contains(spell)) {
            current.add(spell);
            ((net.minecraftforge.common.ForgeConfigSpec.ConfigValue<java.util.List<? extends String>>)(Object)configList).set(current);
            source.sendSuccess(() -> Component.translatable("magic_team.command.filter.add.success", spell, filterName).withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendFailure(Component.translatable("magic_team.command.filter.add.exists", spell, filterName));
        }
        return 1;
    }

    @SuppressWarnings({"null", "unchecked"})
    private static int removeFilter(CommandSourceStack source, String listType, String spell) {
        var configList = listType.equals("beneficial") ? MagicTeamConfig.SERVER.beneficialSpells : MagicTeamConfig.SERVER.explicitHarmfulSpells;
        java.util.List<String> current = new java.util.ArrayList<>((java.util.Collection<? extends String>) configList.get());
        
        Component filterName = Component.translatable(listType.equals("beneficial") ? "magic_team.command.filter.beneficial" : "magic_team.command.filter.harmful");
        
        if (current.remove(spell)) {
            ((net.minecraftforge.common.ForgeConfigSpec.ConfigValue<java.util.List<? extends String>>)(Object)configList).set(current);
            source.sendSuccess(() -> Component.translatable("magic_team.command.filter.remove.success", spell, filterName).withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendFailure(Component.translatable("magic_team.command.filter.remove.missing", spell, filterName));
        }
        return 1;
    }

    @SuppressWarnings("null")
    private static int toggleBypass(CommandSourceStack source, net.minecraft.server.level.ServerPlayer target) {
        java.util.UUID uuid = target.getUUID();
        if (com.gabri.magicteam.util.TeamUtils.BYPASSED_PLAYERS.contains(uuid)) {
            com.gabri.magicteam.util.TeamUtils.BYPASSED_PLAYERS.remove(uuid);
            source.sendSuccess(() -> Component.translatable("magic_team.bypass.disabled").append(" -> ").append(target.getDisplayName()).withStyle(ChatFormatting.RED), true);
        } else {
            com.gabri.magicteam.util.TeamUtils.BYPASSED_PLAYERS.add(uuid);
            source.sendSuccess(() -> Component.translatable("magic_team.bypass.enabled").append(" -> ").append(target.getDisplayName()).withStyle(ChatFormatting.GREEN), true);
        }
        return 1;
    }
}
