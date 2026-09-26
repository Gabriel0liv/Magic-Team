package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free contract for global non-damage hostile magic side effects. */
public final class GlobalMagicSideEffectContractTest {
    private static final Path ENTITY = Path.of("src/main/java/com/gabri/magicteam/mixin/EntityMagicSideEffectMixin.java");
    private static final Path LIVING = Path.of("src/main/java/com/gabri/magicteam/mixin/LivingEntityMagicSideEffectMixin.java");
    private static final Path TEAM_UTILS = Path.of("src/main/java/com/gabri/magicteam/util/TeamUtils.java");
    private static final Path CONFIG = Path.of("src/main/resources/magic_team.mixins.json");

    private GlobalMagicSideEffectContractTest() {
    }

    public static void main(String[] args) throws Exception {
        check(Files.isRegularFile(ENTITY), "global Entity magic side-effect hook is missing");
        check(Files.isRegularFile(LIVING), "global LivingEntity magic side-effect hook is missing");

        String entity = Files.readString(ENTITY);
        String living = Files.readString(LIVING);
        String teamUtils = Files.readString(TEAM_UTILS);
        String config = Files.readString(CONFIG);

        check(teamUtils.contains("shouldBlockCurrentMagicSideEffect"),
                "side-effect hooks must share one central policy helper");
        check(entity.contains("setDeltaMovement"),
                "forced magic movement must pass through the global side-effect gate");
        check(entity.contains("setSecondsOnFire") || entity.contains("setRemainingFireTicks"),
                "hostile magic fire side effects must pass through the global gate");
        check(living.contains("removeEffect"),
                "hostile magic buff removal must pass through the global gate");
        check(living.contains("removeAllEffects"),
                "bulk hostile magic buff removal must pass through the global gate");
        check(config.contains("\"EntityMagicSideEffectMixin\"")
                        && config.contains("\"LivingEntityMagicSideEffectMixin\""),
                "global side-effect mixins must be registered in the strict core config");

        String combined = (entity + living).toLowerCase();
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
