package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free contract for generic attribution capture at entity spawn. */
public final class ServerLevelMagicAttributionContractTest {
    private static final Path MIXIN = Path.of("src/main/java/com/gabri/magicteam/mixin/ServerLevelMagicAttributionMixin.java");
    private static final Path CONFIG = Path.of("src/main/resources/magic_team.mixins.json");

    private ServerLevelMagicAttributionContractTest() {
    }

    public static void main(String[] args) throws Exception {
        check(Files.isRegularFile(MIXIN), "generic ServerLevel attribution hook is missing");
        String source = Files.readString(MIXIN);
        String config = Files.readString(CONFIG);

        check(source.contains("MagicTeamEffectContext.currentAttribution()"),
                "spawn hook must derive attribution only from proven active magic context");
        check(source.contains("MagicAttributionIndex.record"),
                "spawn hook must persist attribution for the spawned entity");
        check(source.contains("TeamUtils.isEnabled()"),
                "spawn attribution must be transparent while Magic Team is disabled");
        check(source.contains("addFreshEntity"),
                "spawn attribution must hook the shared ServerLevel entity-add boundary");
        check(!source.contains("traveloptics") && !source.contains("familiars")
                        && !source.contains("geomancy") && !source.contains("cataclysm"),
                "generic spawn attribution must remain addon-neutral");
        check(config.contains("\"ServerLevelMagicAttributionMixin\""),
                "generic ServerLevel attribution hook is not registered in core config");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
