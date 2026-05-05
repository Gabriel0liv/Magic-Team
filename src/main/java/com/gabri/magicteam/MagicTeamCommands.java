package com.gabri.magicteam;

import com.gabri.magicteam.util.MagicTeamConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class MagicTeamCommands {
    private static final SuggestionProvider<CommandSourceStack> SPELL_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    SpellRegistry.REGISTRY.get().getValues().stream()
                            .map(AbstractSpell::getSpellId),
                    builder
            );

    private MagicTeamCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("magicteam")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("save")
                                .executes(context -> saveConfig(context.getSource())))
                        .then(Commands.literal("reload")
                                .executes(context -> reloadConfig(context.getSource())))
                        .then(Commands.literal("filter")
                                .then(Commands.literal("add")
                                        .then(Commands.literal("beneficial")
                                                .then(Commands.argument("spell", StringArgumentType.greedyString())
                                                        .suggests(SPELL_SUGGESTIONS)
                                                        .executes(context -> addSpell(context.getSource(), true, StringArgumentType.getString(context, "spell")))))
                                        .then(Commands.literal("harmful")
                                                .then(Commands.argument("spell", StringArgumentType.greedyString())
                                                        .suggests(SPELL_SUGGESTIONS)
                                                        .executes(context -> addSpell(context.getSource(), false, StringArgumentType.getString(context, "spell"))))))
                                .then(Commands.literal("remove")
                                        .then(Commands.literal("beneficial")
                                                .then(Commands.argument("spell", StringArgumentType.greedyString())
                                                        .suggests(SPELL_SUGGESTIONS)
                                                        .executes(context -> removeSpell(context.getSource(), true, StringArgumentType.getString(context, "spell")))))
                                        .then(Commands.literal("harmful")
                                                .then(Commands.argument("spell", StringArgumentType.greedyString())
                                                        .suggests(SPELL_SUGGESTIONS)
                                                        .executes(context -> removeSpell(context.getSource(), false, StringArgumentType.getString(context, "spell"))))))
                                .then(Commands.literal("view")
                                        .then(Commands.literal("beneficial")
                                                .executes(context -> viewSpells(context.getSource(), true)))
                                        .then(Commands.literal("harmful")
                                                .executes(context -> viewSpells(context.getSource(), false)))))
        );
    }

    private static int saveConfig(CommandSourceStack source) {
        MagicTeamConfig.saveServerConfig();
        source.sendSuccess(() -> Component.translatable("magic_team.command.save.success").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int reloadConfig(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("magic_team.command.reload.success").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int addSpell(CommandSourceStack source, boolean beneficial, String spell) {
        String normalized = canonicalizeSpellId(spell);
        List<String> current = new ArrayList<>(beneficial ? MagicTeamConfig.SERVER.beneficialSpells() : MagicTeamConfig.SERVER.harmfulSpells());
        if (containsSpell(current, normalized)) {
            source.sendFailure(Component.translatable("magic_team.command.filter.add.exists", normalized, filterName(beneficial)));
            return 0;
        }

        current.add(normalized);
        if (beneficial) {
            MagicTeamConfig.SERVER.setBeneficialSpells(current);
        } else {
            MagicTeamConfig.SERVER.setHarmfulSpells(current);
        }
        MagicTeamConfig.saveServerConfig();
        source.sendSuccess(() -> Component.translatable("magic_team.command.filter.add.success", normalized, filterName(beneficial)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removeSpell(CommandSourceStack source, boolean beneficial, String spell) {
        String normalized = canonicalizeSpellId(spell);
        List<String> current = new ArrayList<>(beneficial ? MagicTeamConfig.SERVER.beneficialSpells() : MagicTeamConfig.SERVER.harmfulSpells());
        boolean removed = current.removeIf(entry -> spellMatches(entry, normalized));
        if (!removed) {
            source.sendFailure(Component.translatable("magic_team.command.filter.remove.missing", normalized, filterName(beneficial)));
            return 0;
        }

        if (beneficial) {
            MagicTeamConfig.SERVER.setBeneficialSpells(current);
        } else {
            MagicTeamConfig.SERVER.setHarmfulSpells(current);
        }
        MagicTeamConfig.saveServerConfig();
        source.sendSuccess(() -> Component.translatable("magic_team.command.filter.remove.success", normalized, filterName(beneficial)).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int viewSpells(CommandSourceStack source, boolean beneficial) {
        List<String> current = beneficial ? MagicTeamConfig.SERVER.beneficialSpells() : MagicTeamConfig.SERVER.harmfulSpells();
        Component filterName = filterName(beneficial);
        source.sendSuccess(() -> Component.translatable("magic_team.command.filter.view.header", filterName).withStyle(ChatFormatting.GOLD), false);
        if (current.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("magic_team.command.filter.view.empty", filterName).withStyle(ChatFormatting.GRAY), false);
        } else {
            for (String spell : current) {
                source.sendSuccess(() -> Component.translatable("magic_team.command.filter.view.item", spell).withStyle(ChatFormatting.YELLOW), false);
            }
        }
        return 1;
    }

    private static Component filterName(boolean beneficial) {
        return Component.translatable(beneficial ? "magic_team.command.filter.beneficial" : "magic_team.command.filter.harmful");
    }

    private static String normalizeSpellId(String spell) {
        return spell == null ? "" : spell.trim().toLowerCase();
    }

    private static boolean containsSpell(List<String> spells, String spellId) {
        for (String entry : spells) {
            if (spellMatches(entry, spellId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean spellMatches(String storedValue, String queriedSpellId) {
        String stored = normalizeSpellId(storedValue);
        String queried = normalizeSpellId(queriedSpellId);
        if (stored.isEmpty() || queried.isEmpty()) {
            return false;
        }

        if (stored.equals(queried)) {
            return true;
        }

        if (stored.endsWith(":" + queried) || queried.endsWith(":" + stored)) {
            return true;
        }

        int storedColon = stored.indexOf(':');
        int queriedColon = queried.indexOf(':');
        String storedPath = storedColon >= 0 ? stored.substring(storedColon + 1) : stored;
        String queriedPath = queriedColon >= 0 ? queried.substring(queriedColon + 1) : queried;
        return storedPath.equals(queriedPath);
    }

    private static String canonicalizeSpellId(String spell) {
        String normalized = normalizeSpellId(spell);
        if (normalized.isEmpty()) {
            return normalized;
        }

        for (AbstractSpell knownSpell : SpellRegistry.REGISTRY.get().getValues()) {
            String knownId = normalizeSpellId(knownSpell.getSpellId());
            if (spellMatches(knownId, normalized)) {
                return knownId;
            }
        }

        return normalized;
    }
}
