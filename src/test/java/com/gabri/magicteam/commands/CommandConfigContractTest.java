package com.gabri.magicteam.commands;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free source contract for the public Magic Team command/config surface. */
public final class CommandConfigContractTest {
    private static final Path COMMANDS = Path.of("src/main/java/com/gabri/magicteam/MagicTeamCommands.java");
    private static final Path SPELL_ARGUMENT = Path.of("src/main/java/com/gabri/magicteam/command/SpellIdArgumentType.java");
    private static final Path CONFIG = Path.of("src/main/java/com/gabri/magicteam/util/MagicTeamConfig.java");
    private static final Path TEAM_UTILS = Path.of("src/main/java/com/gabri/magicteam/util/TeamUtils.java");
    private static final Path ENTITY_MIXIN = Path.of("src/main/java/com/gabri/magicteam/mixin/EntityMixin.java");
    private static final Path DAMAGE_SOURCES_MIXIN = Path.of("src/main/java/com/gabri/magicteam/mixin/DamageSourcesMixin.java");
    private static final Path UTILS_MIXIN = Path.of("src/main/java/com/gabri/magicteam/mixin/UtilsMixin.java");

    private CommandConfigContractTest() {
    }

    public static void main(String[] args) throws Exception {
        String commands = Files.readString(COMMANDS);
        String spellArgument = Files.readString(SPELL_ARGUMENT);
        String config = Files.readString(CONFIG);
        String teamUtils = Files.readString(TEAM_UTILS);
        String entityMixin = Files.readString(ENTITY_MIXIN);
        String damageSourcesMixin = Files.readString(DAMAGE_SOURCES_MIXIN);
        String utilsMixin = Files.readString(UTILS_MIXIN);

        check(commands.contains("Commands.literal(\"enabled\")"), "missing /magicteam enabled <true|false>");
        check(commands.contains("BoolArgumentType.bool()"), "enabled/message toggles must use Brigadier boolean arguments");
        check(commands.contains("Commands.literal(\"spell\")"), "missing spell command root");
        check(commands.contains("Commands.literal(\"info\")"), "missing spell info command");
        check(commands.contains("Commands.literal(\"set\")"), "missing spell set command");
        check(commands.contains("Commands.literal(\"support\")"), "missing support behavior");
        check(commands.contains("Commands.literal(\"hostile\")"), "missing hostile behavior");
        check(commands.contains("Commands.literal(\"reset\")"), "missing spell/message reset command");
        check(commands.contains("Commands.literal(\"overrides\")"), "missing spell overrides command");
        check(commands.contains("Commands.literal(\"list\")"), "missing spell list command");
        check(commands.contains("Commands.literal(\"message\")"), "missing message command root");
        check(commands.contains("Commands.literal(\"debug\")"), "missing debug toggle");
        check(!commands.contains("Commands.literal(\"filter\")"), "legacy filter command must be removed");
        check(!commands.contains("Commands.literal(\"save\")"), "manual save command must be removed");

        check(commands.contains("SpellIdArgumentType.spellId()"),
                "spell commands need an argument parser that accepts namespaced IDs like irons_spellbooks:fireball");
        check(commands.contains("SpellIdArgumentType.getSpellId"),
                "spell command handlers must read the namespaced spell token from the custom argument");
        check(!commands.contains("Commands.argument(\"spell\", StringArgumentType.word())"),
                "StringArgumentType.word() rejects ':' and must not be used for spell IDs");
        check(spellArgument.contains("!Character.isWhitespace(reader.peek())"),
                "spell ID parser must consume the complete non-whitespace token, including ':' and '/' characters");

        check(config.contains("BooleanValue enabled"), "server config needs persistent enabled flag");
        check(config.contains("BooleanValue blockedMessageEnabled"), "server config needs message enabled flag");
        check(config.contains("ConfigValue<String> blockedMessage"), "server config needs configurable blocked message");
        check(config.contains("ConfigValue<List<? extends String>> spellOverrides"), "server config needs spell overrides");
        check(!config.contains("harmfulSpells"), "legacy harmful list must be removed");
        check(!config.contains("beneficialSpells"), "legacy beneficial list must be removed");

        check(teamUtils.contains("SpellBehavior"), "TeamUtils must resolve support/hostile behavior");
        check(teamUtils.contains("spellOverrides"), "TeamUtils must consult admin spell overrides");
        check(teamUtils.contains("blockedMessageEnabled"), "blocked feedback must honor message toggle");
        check(teamUtils.contains("Component.Serializer.fromJson"), "blocked feedback must accept tellraw-style JSON components");
        check(teamUtils.contains("if (!isEnabled())"), "central gameplay gates must support disabling Magic Team");
        check(entityMixin.contains("TeamUtils.isEnabled()"), "Entity alliance mixin must become transparent while disabled");
        check(damageSourcesMixin.contains("if (!TeamUtils.isEnabled())"),
                "DamageSources mixin must defer to Iron's original result while Magic Team is disabled");
        check(utilsMixin.contains("if (!TeamUtils.isEnabled())"),
                "target-helper mixin must defer to Iron's original method while Magic Team is disabled");

        String damageGate = isolate(teamUtils,
                "public static boolean shouldBlockMagicDamage(Entity attacker, Entity target, AbstractSpell spell)",
                "public static SpellBehavior getDefaultSpellBehavior");
        check(damageGate.contains("getSpellOverride") || damageGate.contains("getSpellBehavior"),
                "spell overrides must affect allied spell damage, not only target selection");

        String effectGate = isolate(teamUtils,
                "public static boolean shouldAllowEffect(Entity source, Entity target, MobEffectInstance effectInstance,",
                "public static boolean shouldBlockMagicDamage(Entity attacker, Entity target)");
        check(effectGate.contains("getSpellOverride"),
                "explicit admin overrides must take precedence when filtering spell effects");
    }

    private static String isolate(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start + 1);
        check(start >= 0 && end > start, "could not isolate source contract section: " + startMarker);
        return source.substring(start, end);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
