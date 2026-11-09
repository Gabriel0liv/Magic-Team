package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards global coverage that existed in the published Magic Team 2.3.1 JAR.
 * The goal is not to preserve its bugs, only the entry points that made its
 * broad Iron's/addon coverage work in practice.
 */
public final class Published231ParityContractTest {
    private static final Path CORE_CONFIG = Path.of("src/main/resources/magic_team.mixins.json");
    private static final Path CATACLYSM_CONFIG = Path.of("src/main/resources/magic_team.cataclysm.mixins.json");

    private Published231ParityContractTest() {
    }

    public static void main(String[] args) throws Exception {
        commandCastPathKeepsGlobalSpellContext();
        immolateKeepsAfflicterContextBridge();
        publishedCataclysmProjectileCoverageIsNotSilentlyDropped();
    }

    private static void commandCastPathKeepsGlobalSpellContext() throws Exception {
        String core = Files.readString(CORE_CONFIG);
        Path source = Path.of("src/main/java/com/gabri/magicteam/mixin/CastCommandMixin.java");

        check(Files.isRegularFile(source),
                "2.3.1 parity regression: CastCommand global context bridge is missing");
        check(core.contains("\"CastCommandMixin\""),
                "2.3.1 parity regression: CastCommandMixin is not registered in the strict core config");

        String java = Files.readString(source);
        check(java.contains("CastCommand.class"), "CastCommandMixin must target Iron's CastCommand");
        check(java.contains("method = \"castSpell"), "CastCommandMixin must wrap the command cast dispatch");
        check(java.contains("MagicTeamEffectContext.push"), "command casts must enter Magic Team context");
        check(java.contains("finally"), "command cast context must be exception-safe");
    }

    private static void immolateKeepsAfflicterContextBridge() throws Exception {
        String core = Files.readString(CORE_CONFIG);
        Path source = Path.of("src/main/java/com/gabri/magicteam/mixin/ImmolateEffectMixin.java");

        check(Files.isRegularFile(source),
                "2.3.1 parity regression: Immolate afflicter context bridge is missing");
        check(core.contains("\"ImmolateEffectMixin\""),
                "2.3.1 parity regression: ImmolateEffectMixin is not registered in the strict core config");

        String java = Files.readString(source);
        check(java.contains("ImmolateEffect.class"), "Immolate bridge must target Iron's core effect");
        check(java.contains("addImmolateStack"), "Immolate bridge must wrap addImmolateStack");
        check(java.contains("InteractionType.HARMFUL"), "Immolate chaining must be marked hostile");
    }

    private static void publishedCataclysmProjectileCoverageIsNotSilentlyDropped() throws Exception {
        String config = Files.readString(CATACLYSM_CONFIG);
        check(config.contains("\"required\": false"), "Cataclysm compatibility must remain optional");
        check(config.contains("\"defaultRequire\": 0"), "Cataclysm compatibility must remain fail-soft");
        check(config.contains("compat.cataclysm.FlareBombFriendlyFireMixin"),
                "2.3.1 parity regression: Flare Bomb compatibility is missing");
        check(config.contains("compat.cataclysm.WitherHowitzerFriendlyFireMixin"),
                "2.3.1 parity regression: Wither Howitzer compatibility is missing");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
