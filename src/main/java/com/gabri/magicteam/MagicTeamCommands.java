package com.gabri.magicteam;

import com.gabri.magicteam.command.SpellIdArgumentType;
import com.gabri.magicteam.util.MagicTeamConfig;
import com.gabri.magicteam.util.SpellBehavior;
import com.gabri.magicteam.util.TeamUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MagicTeamCommands {
    private static final SuggestionProvider<CommandSourceStack> SPELL_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    SpellRegistry.REGISTRY.get().getValues().stream()
                            .map(AbstractSpell::getSpellId)
                            .sorted(),
                    builder
            );

    private static final SuggestionProvider<CommandSourceStack> NAMESPACE_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(
                    SpellRegistry.REGISTRY.get().getValues().stream()
                            .map(AbstractSpell::getSpellId)
                            .map(MagicTeamCommands::namespaceOf)
                            .distinct()
                            .sorted(),
                    builder
            );

    private MagicTeamCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("magicteam")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("enabled")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setEnabled(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                        .then(Commands.literal("status")
                                .executes(context -> status(context.getSource())))
                        .then(Commands.literal("reload")
                                .executes(context -> reloadConfig(context.getSource())))
                        .then(Commands.literal("debug")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(context -> setDebug(
                                                context.getSource(),
                                                BoolArgumentType.getBool(context, "value")
                                        ))))
                        .then(Commands.literal("message")
                                .then(Commands.literal("enabled")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(context -> setMessageEnabled(
                                                        context.getSource(),
                                                        BoolArgumentType.getBool(context, "value")
                                                ))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(context -> setMessage(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "message")
                                                ))))
                                .then(Commands.literal("reset")
                                        .executes(context -> resetMessage(context.getSource()))))
                        .then(Commands.literal("spell")
                                .then(Commands.literal("info")
                                        .then(Commands.argument("spell", SpellIdArgumentType.spellId())
                                                .suggests(SPELL_SUGGESTIONS)
                                                .executes(context -> spellInfo(
                                                        context.getSource(),
                                                        SpellIdArgumentType.getSpellId(context, "spell")
                                                ))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("spell", SpellIdArgumentType.spellId())
                                                .suggests(SPELL_SUGGESTIONS)
                                                .then(Commands.literal("support")
                                                        .executes(context -> setSpellBehavior(
                                                                context.getSource(),
                                                                SpellIdArgumentType.getSpellId(context, "spell"),
                                                                SpellBehavior.SUPPORT
                                                        )))
                                                .then(Commands.literal("hostile")
                                                        .executes(context -> setSpellBehavior(
                                                                context.getSource(),
                                                                SpellIdArgumentType.getSpellId(context, "spell"),
                                                                SpellBehavior.HOSTILE
                                                        )))))
                                .then(Commands.literal("reset")
                                        .then(Commands.argument("spell", SpellIdArgumentType.spellId())
                                                .suggests(SPELL_SUGGESTIONS)
                                                .executes(context -> resetSpellBehavior(
                                                        context.getSource(),
                                                        SpellIdArgumentType.getSpellId(context, "spell")
                                                ))))
                                .then(Commands.literal("overrides")
                                        .executes(context -> listOverrides(context.getSource())))
                                .then(Commands.literal("list")
                                        .executes(context -> listSpells(context.getSource(), null))
                                        .then(Commands.argument("namespace", StringArgumentType.word())
                                                .suggests(NAMESPACE_SUGGESTIONS)
                                                .executes(context -> listSpells(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "namespace")
                                                )))))
        );
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        MagicTeamConfig.SERVER.setEnabled(enabled);
        MagicTeamConfig.saveServerConfig();
        source.sendSuccess(
                () -> Component.literal("Magic Team " + (enabled ? "ATIVADO" : "DESATIVADO") + ".")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                true
        );
        return 1;
    }

    private static int status(CommandSourceStack source) {
        boolean enabled = TeamUtils.isEnabled();
        int registeredSpells = SpellRegistry.REGISTRY.get().getValues().size();
        int overrides = MagicTeamConfig.SERVER.spellOverrides().size();

        source.sendSuccess(() -> Component.literal("--- Magic Team ---").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Estado: " + (enabled ? "ATIVADO" : "DESATIVADO"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.literal("Spells registradas: " + registeredSpells), false);
        source.sendSuccess(() -> Component.literal("Overrides: " + overrides), false);
        source.sendSuccess(() -> Component.literal("Mensagem de bloqueio: "
                + (MagicTeamConfig.SERVER.blockedMessageEnabled() ? "ATIVADA" : "DESATIVADA")), false);
        source.sendSuccess(() -> Component.literal("Debug: " + (TeamUtils.isDebugEnabled() ? "ATIVADO" : "DESATIVADO")
                + " (não persistente)"), false);
        source.sendSuccess(() -> Component.literal("Babel Core: "
                + (ModList.get().isLoaded("babel_core") ? "OK" : "AUSENTE")), false);
        source.sendSuccess(() -> Component.literal("Iron's Spellbooks: "
                + (ModList.get().isLoaded("irons_spellbooks") ? "OK" : "AUSENTE")), false);
        return 1;
    }

    private static int reloadConfig(CommandSourceStack source) {
        try {
            if (!MagicTeamConfig.reloadServerConfig()) {
                source.sendFailure(Component.literal("A configuração server do Magic Team ainda não está disponível."));
                return 0;
            }
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("Falha ao recarregar a configuração do Magic Team: " + exception.getMessage()));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Configuração do Magic Team recarregada do disco.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setDebug(CommandSourceStack source, boolean enabled) {
        TeamUtils.setDebugEnabled(enabled);
        source.sendSuccess(
                () -> Component.literal("Debug do Magic Team " + (enabled ? "ATIVADO" : "DESATIVADO")
                        + ". Esta opção não persiste após reiniciar.")
                        .withStyle(enabled ? ChatFormatting.YELLOW : ChatFormatting.GRAY),
                true
        );
        return 1;
    }

    private static int setMessageEnabled(CommandSourceStack source, boolean enabled) {
        MagicTeamConfig.SERVER.setBlockedMessageEnabled(enabled);
        MagicTeamConfig.saveServerConfig();
        source.sendSuccess(
                () -> Component.literal("Mensagem de bloqueio " + (enabled ? "ATIVADA" : "DESATIVADA") + ".")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                true
        );
        return 1;
    }

    private static int setMessage(CommandSourceStack source, String rawMessage) {
        try {
            TeamUtils.parseBlockedMessage(rawMessage);
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("JSON de mensagem inválido: " + exception.getMessage()));
            return 0;
        }

        MagicTeamConfig.SERVER.setBlockedMessage(rawMessage);
        MagicTeamConfig.saveServerConfig();
        source.sendSuccess(() -> Component.literal("Mensagem de bloqueio atualizada.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int resetMessage(CommandSourceStack source) {
        MagicTeamConfig.SERVER.setBlockedMessage(MagicTeamConfig.DEFAULT_BLOCKED_MESSAGE);
        MagicTeamConfig.saveServerConfig();
        source.sendSuccess(() -> Component.literal("Mensagem de bloqueio restaurada para o padrão.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int spellInfo(CommandSourceStack source, String input) {
        ResolvedSpell resolved = resolveSpell(source, input);
        if (resolved == null) {
            return 0;
        }

        SpellBehavior defaultBehavior = TeamUtils.getDefaultSpellBehavior(resolved.spell());
        SpellBehavior override = TeamUtils.getSpellOverride(resolved.id());
        SpellBehavior effective = override != null ? override : defaultBehavior;

        source.sendSuccess(() -> Component.literal("--- " + resolved.id() + " ---").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Padrão: " + defaultBehavior.configName().toUpperCase(Locale.ROOT)), false);
        source.sendSuccess(() -> Component.literal("Override: "
                + (override == null ? "nenhum" : override.configName().toUpperCase(Locale.ROOT))), false);
        source.sendSuccess(() -> Component.literal("Comportamento atual: " + effective.configName().toUpperCase(Locale.ROOT))
                .withStyle(effective == SpellBehavior.SUPPORT ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        return 1;
    }

    private static int setSpellBehavior(CommandSourceStack source, String input, SpellBehavior behavior) {
        ResolvedSpell resolved = resolveSpell(source, input);
        if (resolved == null) {
            return 0;
        }

        List<String> overrides = new ArrayList<>(MagicTeamConfig.SERVER.spellOverrides());
        overrides.removeIf(entry -> overrideMatchesSpell(entry, resolved.id()));
        overrides.add(resolved.id() + "=" + behavior.configName());
        overrides.sort(Comparator.naturalOrder());
        MagicTeamConfig.SERVER.setSpellOverrides(overrides);
        MagicTeamConfig.saveServerConfig();

        source.sendSuccess(
                () -> Component.literal(resolved.id() + " agora está como "
                        + behavior.configName().toUpperCase(Locale.ROOT) + ".")
                        .withStyle(ChatFormatting.GREEN),
                true
        );
        return 1;
    }

    private static int resetSpellBehavior(CommandSourceStack source, String input) {
        ResolvedSpell resolved = resolveSpell(source, input);
        if (resolved == null) {
            return 0;
        }

        List<String> overrides = new ArrayList<>(MagicTeamConfig.SERVER.spellOverrides());
        boolean removed = overrides.removeIf(entry -> overrideMatchesSpell(entry, resolved.id()));
        if (!removed) {
            source.sendFailure(Component.literal(resolved.id() + " não possui override."));
            return 0;
        }

        MagicTeamConfig.SERVER.setSpellOverrides(overrides);
        MagicTeamConfig.saveServerConfig();
        SpellBehavior defaultBehavior = TeamUtils.getDefaultSpellBehavior(resolved.spell());
        source.sendSuccess(
                () -> Component.literal(resolved.id() + " voltou ao padrão "
                        + defaultBehavior.configName().toUpperCase(Locale.ROOT) + ".")
                        .withStyle(ChatFormatting.GREEN),
                true
        );
        return 1;
    }

    private static int listOverrides(CommandSourceStack source) {
        List<String> overrides = new ArrayList<>(MagicTeamConfig.SERVER.spellOverrides());
        overrides.sort(Comparator.naturalOrder());

        source.sendSuccess(() -> Component.literal("--- Overrides do Magic Team (" + overrides.size() + ") ---")
                .withStyle(ChatFormatting.GOLD), false);
        if (overrides.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Nenhum override configurado.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }

        for (String override : overrides) {
            source.sendSuccess(() -> Component.literal("- " + override).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int listSpells(CommandSourceStack source, String requestedNamespace) {
        String namespace = requestedNamespace == null ? null : requestedNamespace.trim().toLowerCase(Locale.ROOT);
        List<String> spells = SpellRegistry.REGISTRY.get().getValues().stream()
                .map(AbstractSpell::getSpellId)
                .filter(id -> namespace == null || namespaceOf(id).equals(namespace))
                .sorted()
                .toList();

        if (spells.isEmpty()) {
            source.sendFailure(Component.literal(namespace == null
                    ? "Nenhuma spell está registrada."
                    : "Nenhuma spell registrada no namespace " + namespace + "."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("--- Spells registradas (" + spells.size() + ") ---")
                .withStyle(ChatFormatting.GOLD), false);
        for (String spell : spells) {
            source.sendSuccess(() -> Component.literal("- " + spell).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static ResolvedSpell resolveSpell(CommandSourceStack source, String input) {
        String normalized = normalizeSpellId(input);
        if (normalized.isEmpty()) {
            source.sendFailure(Component.literal("Informe uma spell."));
            return null;
        }

        List<AbstractSpell> exact = SpellRegistry.REGISTRY.get().getValues().stream()
                .filter(spell -> normalizeSpellId(spell.getSpellId()).equals(normalized))
                .toList();
        if (!exact.isEmpty()) {
            AbstractSpell spell = exact.get(0);
            return new ResolvedSpell(normalizeSpellId(spell.getSpellId()), spell);
        }

        if (normalized.contains(":")) {
            source.sendFailure(Component.literal("Spell não registrada: " + normalized));
            return null;
        }

        List<AbstractSpell> byPath = SpellRegistry.REGISTRY.get().getValues().stream()
                .filter(spell -> pathOf(spell.getSpellId()).equals(normalized))
                .toList();
        if (byPath.isEmpty()) {
            source.sendFailure(Component.literal("Spell não registrada: " + normalized));
            return null;
        }
        if (byPath.size() > 1) {
            source.sendFailure(Component.literal("ID ambíguo '" + normalized + "'. Use o ID completo com namespace."));
            return null;
        }

        AbstractSpell spell = byPath.get(0);
        return new ResolvedSpell(normalizeSpellId(spell.getSpellId()), spell);
    }

    private static boolean overrideMatchesSpell(String entry, String spellId) {
        if (entry == null) {
            return false;
        }

        int separator = entry.lastIndexOf('=');
        if (separator <= 0) {
            return false;
        }
        return normalizeSpellId(entry.substring(0, separator)).equals(normalizeSpellId(spellId));
    }

    private static String normalizeSpellId(String spellId) {
        return spellId == null ? "" : spellId.trim().toLowerCase(Locale.ROOT);
    }

    private static String namespaceOf(String spellId) {
        String normalized = normalizeSpellId(spellId);
        int colon = normalized.indexOf(':');
        return colon >= 0 ? normalized.substring(0, colon) : "";
    }

    private static String pathOf(String spellId) {
        String normalized = normalizeSpellId(spellId);
        int colon = normalized.indexOf(':');
        return colon >= 0 ? normalized.substring(colon + 1) : normalized;
    }

    private record ResolvedSpell(String id, AbstractSpell spell) {
    }
}
