package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Structural regressions for runtime failures that motivated the global-first refactor. */
public final class RuntimeJarCrashRegressionContractTest {
    private static final Path MIXIN_ROOT = Path.of("src/main/java/com/gabri/magicteam/mixin");
    private static final Path TRAVEL_CONFIG = Path.of("src/main/resources/magic_team.traveloptics.mixins.json");

    private RuntimeJarCrashRegressionContractTest() {
    }

    public static void main(String[] args) throws Exception {
        playerCastTickTargetsTheCompiledLambdaBody();
        fragileSpellRedirectsStayRemoved();
        standardTargetedSpellsUseAbstractSpellGate();
        delayedEntitiesUseGenericAttribution();
        optionalTravelOpticsLayerRemainsFailSoft();
    }

    private static void playerCastTickTargetsTheCompiledLambdaBody() throws Exception {
        String source = Files.readString(MIXIN_ROOT.resolve("MagicManagerCastDispatchMixin.java"));
        check(source.contains("method = \"lambda$tick$0\""),
                "MagicManager player cast redirect must target the compiled lambda body used by Iron's 3.15.3");
        check(!source.contains("method = \"tick\""),
                "redirecting MagicManager.tick misses the cast tick invocation inside the compiled lambda");
    }

    private static void fragileSpellRedirectsStayRemoved() throws Exception {
        String config = Files.readString(TRAVEL_CONFIG);
        for (String removed : new String[]{
                "AquaMissilesFriendlyFireMixin",
                "OrbitalVoidFriendlyFireMixin",
                "TidalGraspFriendlyFireMixin",
                "ExtendedDeathLaserFriendlyFireMixin"
        }) {
            check(!config.contains(removed), "fragile spell adapter must stay removed from config: " + removed);
        }

        check(!Files.exists(MIXIN_ROOT.resolve("compat/traveloptics/OrbitalVoidFriendlyFireMixin.java")),
                "Orbital Void 0/1 redirect regression must not be reintroduced");
    }

    private static void standardTargetedSpellsUseAbstractSpellGate() throws Exception {
        String source = Files.readString(MIXIN_ROOT.resolve("AbstractSpellMixin.java"));
        check(source.contains("TargetEntityCastData"),
                "standard selected-target spells must be protected at the Iron's API boundary");
        check(source.contains("magicTeam$cancelProtectedTarget"),
                "target protection must be centralized in AbstractSpell");
        check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                "global target gate must use the central policy");
    }

    private static void delayedEntitiesUseGenericAttribution() throws Exception {
        String entity = Files.readString(MIXIN_ROOT.resolve("EntityMagicAttributionMixin.java"));
        String effect = Files.readString(MIXIN_ROOT.resolve("MobEffectInstanceMagicAttributionMixin.java"));
        check(entity.contains("MagicAttributionIndex.record") && entity.contains("MagicAttributionIndex.refresh"),
                "delayed entities must retain generic magic attribution");
        check(effect.contains("MagicEffectAttributionIndex.get"),
                "delayed MobEffect ticks must recover generic magic attribution");
    }

    private static void optionalTravelOpticsLayerRemainsFailSoft() throws Exception {
        String config = Files.readString(TRAVEL_CONFIG);
        check(config.contains("\"required\": false"), "Travel Optics compatibility must remain optional");
        check(config.contains("\"defaultRequire\": 0"),
                "remaining Travel Optics mechanism exceptions must tolerate addon bytecode drift");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
