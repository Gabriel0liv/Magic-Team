package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free source contract for generic attribution propagation. */
public final class MagicAttributionPropagationContractTest {
    private MagicAttributionPropagationContractTest() {
    }

    public static void main(String[] args) throws Exception {
        String serverBoundary = Files.readString(Path.of(
                "src/main/java/com/gabri/magicteam/mixin/EntityMagicAttributionMixin.java"));
        String projectile = Files.readString(Path.of(
                "src/main/java/com/gabri/magicteam/mixin/AbstractMagicProjectileMixin.java"));
        String aoe = Files.readString(Path.of(
                "src/main/java/com/gabri/magicteam/mixin/AoeEntityMixin.java"));
        String coreConfig = Files.readString(Path.of("src/main/resources/magic_team.mixins.json"));

        check(coreConfig.contains("\"EntityMagicAttributionMixin\""),
                "generic server attribution boundary must be registered in core config");
        check(serverBoundary.contains("addFreshEntity"),
                "spawned magic entities must inherit attribution at a shared server boundary");
        check(serverBoundary.contains("MagicTeamEffectContext.currentAttribution()"),
                "spawn propagation must require proven active magic context");
        check(serverBoundary.contains("MagicAttributionIndex.record(entity, attribution)"),
                "spawned entity attribution must be persisted by UUID");
        check(serverBoundary.contains("method = \"tickNonPassenger\"")
                        && serverBoundary.contains("@At(\"HEAD\")")
                        && serverBoundary.contains("@At(\"RETURN\")"),
                "delayed attribution must wrap the stable tickNonPassenger method boundary");
        check(serverBoundary.contains("MAGIC_TEAM_TICK_SCOPES"),
                "paired tick injections must track whether a magic context was actually pushed");
        check(serverBoundary.contains("MagicAttributionIndex.refresh(entity, level.getGameTime())"),
                "active delayed entities must validate and refresh attribution lifetime");
        check(serverBoundary.contains("TeamUtils.isEnabled()"),
                "disabled Magic Team must not create or re-enter enforcement attribution");
        check(serverBoundary.contains("MagicTeamEffectContext.pop()"),
                "re-entered entity context must be popped at the tick boundary");

        for (String shared : new String[]{projectile, aoe}) {
            check(shared.contains("MagicTeamEffectContext.currentAttribution()"),
                    "shared Iron's entity path must preserve active spell attribution");
            check(shared.contains("MagicAttributionIndex.get"),
                    "shared Iron's entity path must recover delayed attribution");
            check(shared.contains("MagicTeamEffectContext.push"),
                    "shared Iron's entity path must re-enter the recovered context");
        }

        for (String forbidden : new String[]{"traveloptics", "geomancyplus", "familiars", "cataclysm"}) {
            check(!serverBoundary.toLowerCase().contains(forbidden),
                    "generic propagation must not name an addon: " + forbidden);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
