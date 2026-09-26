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
        check(serverBoundary.contains("tickNonPassenger"),
                "attributed delayed entities must re-enter magic context during server tick");
        check(serverBoundary.contains("MagicAttributionIndex.get(entity, level.getGameTime())"),
                "delayed entity scope must validate attribution expiry");
        check(serverBoundary.contains("if (!TeamUtils.isEnabled()"),
                "disabled Magic Team must not create enforcement attribution");
        check(serverBoundary.contains("finally"),
                "re-entered entity context must always pop on normal/exceptional tick exit");

        for (String shared : new String[]{projectile, aoe}) {
            check(shared.contains("MagicTeamEffectContext.currentAttribution()"),
                    "shared Iron's entity path must preserve active spell attribution");
            check(shared.contains("MagicAttributionIndex.get"),
                    "shared Iron's entity path must recover delayed attribution");
            check(shared.contains("MagicTeamEffectContext.push"),
                    "shared Iron's entity path must re-enter the recovered context");
        }

        for (String forbidden : new String[]{
                "traveloptics", "geomancyplus", "familiars", "cataclysm"
        }) {
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
