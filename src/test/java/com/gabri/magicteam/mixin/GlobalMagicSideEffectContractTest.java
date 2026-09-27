package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free contract for global non-damage hostile magic side effects. */
public final class GlobalMagicSideEffectContractTest {
    private static final Path ENTITY = Path.of("src/main/java/com/gabri/magicteam/mixin/EntityMagicSideEffectMixin.java");
    private static final Path LIVING = Path.of("src/main/java/com/gabri/magicteam/mixin/LivingEntityMagicSideEffectMixin.java");
    private static final Path POLICY = Path.of("src/main/java/com/gabri/magicteam/util/MagicSideEffectPolicy.java");
    private static final Path CONFIG = Path.of("src/main/resources/magic_team.mixins.json");

    private GlobalMagicSideEffectContractTest() {
    }

    public static void main(String[] args) throws Exception {
        check(Files.isRegularFile(ENTITY), "global Entity magic side-effect hook is missing");
        check(Files.isRegularFile(LIVING), "global LivingEntity magic side-effect hook is missing");
        check(Files.isRegularFile(POLICY), "shared magic side-effect policy is missing");

        String entity = Files.readString(ENTITY);
        String living = Files.readString(LIVING);
        String policy = Files.readString(POLICY);
        String config = Files.readString(CONFIG);

        check(policy.contains("shouldBlock"), "side-effect hooks must share one central policy helper");
        check(policy.contains("resolveMagicBehavior") && policy.contains("resolveMagicSource"),
                "side-effect policy must reuse normalized global magic evidence");
        check(entity.contains("setDeltaMovement"),
                "forced magic movement must pass through the global side-effect gate");
        check(entity.contains("setSecondsOnFire") || entity.contains("setRemainingFireTicks"),
                "hostile magic fire side effects must pass through the global gate");
        check(entity.contains("getRemainingFireTicks") && entity.contains("ticks <="),
                "reducing/extinguishing fire must remain a beneficial side effect even inside mixed spells");
        check(living.contains("removeEffect"),
                "hostile magic buff removal must pass through the global gate");
        check(living.contains("effect.isBeneficial()"),
                "removing a harmful effect must remain a beneficial cleanse in mixed spells");
        check(living.contains("removeAllEffects"),
                "bulk hostile magic buff removal must pass through the global gate");
        check(config.contains("\"EntityMagicSideEffectMixin\"")
                        && config.contains("\"LivingEntityMagicSideEffectMixin\""),
                "global side-effect mixins must be registered in the strict core config");

        String combined = (entity + living + policy).toLowerCase();
        for (String forbidden : new String[]{"traveloptics", "familiars", "geomancy", "cataclysm"}) {
            check(!combined.contains(forbidden), "global side-effect hooks must remain addon-neutral: " + forbidden);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
