package com.gabri.magicteam;

import java.nio.file.Files;
import java.nio.file.Path;

/** Regression for login failure caused by an unregistered custom Brigadier argument type. */
public final class CommandArgumentSerializationContractTest {
    private static final Path COMMANDS = Path.of("src/main/java/com/gabri/magicteam/MagicTeamCommands.java");
    private static final Path CUSTOM_ARGUMENT = Path.of("src/main/java/com/gabri/magicteam/command/SpellIdArgumentType.java");

    private CommandArgumentSerializationContractTest() {
    }

    public static void main(String[] args) throws Exception {
        String commands = Files.readString(COMMANDS);

        check(commands.contains("ResourceLocationArgument.id()"),
                "spell IDs must use Minecraft's registered ResourceLocation argument type");
        check(commands.contains("ResourceLocationArgument.getId"),
                "spell IDs must be read from the vanilla ResourceLocation argument");
        check(!commands.contains("SpellIdArgumentType"),
                "an unregistered custom Brigadier argument breaks ClientboundCommandsPacket login sync");
        check(!Files.exists(CUSTOM_ARGUMENT),
                "obsolete custom SpellIdArgumentType must stay removed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
