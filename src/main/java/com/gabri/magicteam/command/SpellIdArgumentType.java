package com.gabri.magicteam.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.List;

/**
 * Brigadier token argument for registry-like spell IDs.
 *
 * <p>Brigadier's StringArgumentType.word() rejects ':' and '/', so it cannot parse
 * normal Minecraft namespaced IDs such as {@code irons_spellbooks:fireball}.
 * This argument consumes one complete non-whitespace token and leaves following
 * literals (for example {@code support} / {@code hostile}) available to Brigadier.</p>
 */
public final class SpellIdArgumentType implements ArgumentType<String> {
    private static final SpellIdArgumentType INSTANCE = new SpellIdArgumentType();
    private static final Collection<String> EXAMPLES = List.of(
            "irons_spellbooks:fireball",
            "fireball"
    );

    private SpellIdArgumentType() {
    }

    public static SpellIdArgumentType spellId() {
        return INSTANCE;
    }

    public static String getSpellId(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
