package com.gabri.magicteam.commands;

import com.gabri.magicteam.MagicTeamCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Builds the real /magicteam Brigadier tree and parses every documented
 * command, without executing it (execution needs a running server).
 */
public final class CommandTreeContractTest {
    private static final String[] VALID = {
            "magicteam enabled true",
            "magicteam enabled false",
            "magicteam status",
            "magicteam reload",
            "magicteam debug true",
            "magicteam targetnotification false",
            "magicteam message enabled true",
            "magicteam message set Você não pode ferir um aliado.",
            "magicteam message set {\"text\":\"Aliado!\",\"color\":\"red\"}",
            "magicteam message reset",
            "magicteam spell info irons_spellbooks:fireball",
            "magicteam spell info fireball",
            "magicteam spell set irons_spellbooks:heal support",
            "magicteam spell set traveloptics:aqua_missiles hostile",
            "magicteam spell reset irons_spellbooks:heal",
            "magicteam spell overrides",
            "magicteam spell list",
            "magicteam spell list irons_spellbooks",
    };

    private static final String[] INVALID = {
            "magicteam enabled maybe",
            "magicteam spell set irons_spellbooks:heal neutral",
            "magicteam spell info Irons:Fireball",
            "magicteam filter",
            "magicteam save",
            "magicteam targetnotification enabled true",
    };

    private CommandTreeContractTest() {
    }

    public static void main(String[] args) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        MagicTeamCommands.register(dispatcher);

        CommandSourceStack operator = source(4);
        for (String command : VALID) {
            ParseResults<CommandSourceStack> parse = dispatcher.parse(command, operator);
            check(isExecutable(parse), "command must parse to an executable node: /" + command
                    + " " + parse.getExceptions());
        }

        for (String command : INVALID) {
            check(!isExecutable(dispatcher.parse(command, operator)), "command must be rejected: /" + command);
        }

        CommandSourceStack player = source(0);
        check(!isExecutable(dispatcher.parse("magicteam status", player)),
                "/magicteam must require operator permission level 2");
    }

    private static boolean isExecutable(ParseResults<CommandSourceStack> parse) {
        return !parse.getReader().canRead()
                && parse.getExceptions().isEmpty()
                && parse.getContext().getCommand() != null;
    }

    private static CommandSourceStack source(int permissionLevel) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, permissionLevel,
                "contract-test", Component.literal("contract-test"), null, null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
