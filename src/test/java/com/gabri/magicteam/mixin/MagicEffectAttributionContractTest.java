package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free contract for delayed MobEffect attribution. */
public final class MagicEffectAttributionContractTest {
    private static final Path INDEX = Path.of("src/main/java/com/gabri/magicteam/util/MagicEffectAttributionIndex.java");
    private static final Path EFFECT_MIXIN = Path.of("src/main/java/com/gabri/magicteam/mixin/MobEffectInstanceMagicAttributionMixin.java");
    private static final Path LIVING = Path.of("src/main/java/com/gabri/magicteam/mixin/LivingEntityMixin.java");
    private static final Path CONFIG = Path.of("src/main/resources/magic_team.mixins.json");

    private MagicEffectAttributionContractTest() {
    }

    public static void main(String[] args) throws Exception {
        check(Files.isRegularFile(INDEX), "generic MobEffect attribution index is missing");
        check(Files.isRegularFile(EFFECT_MIXIN), "MobEffect tick attribution mixin is missing");

        String index = Files.readString(INDEX);
        String effectMixin = Files.readString(EFFECT_MIXIN);
        String living = Files.readString(LIVING);
        String config = Files.readString(CONFIG);

        check(index.contains("UUID targetId"), "effect attribution must key by target UUID");
        check(index.contains("String effectId"), "effect attribution must distinguish effect type");
        check(index.contains("MagicAttribution attribution"), "effect attribution must reuse the generic magic value");
        check(index.contains("cleanup"), "effect attribution must support expiry cleanup");

        check(living.contains("MagicEffectAttributionIndex.record"),
                "successful magic effect application must persist attribution");
        check(effectMixin.contains("MagicEffectAttributionIndex.get"),
                "effect tick must recover persisted attribution");
        check(effectMixin.contains("MagicTeamEffectContext.push"),
                "effect tick must re-enter magic context");
        check(effectMixin.contains("finally") && effectMixin.contains("MagicTeamEffectContext.pop"),
                "effect tick context must be released reliably");
        check(config.contains("\"MobEffectInstanceMagicAttributionMixin\""),
                "effect attribution mixin must be registered globally");

        for (String forbidden : new String[]{"traveloptics", "familiars", "geomancy", "cataclysm"}) {
            check(!index.toLowerCase().contains(forbidden), "effect attribution index must be addon-neutral: " + forbidden);
            check(!effectMixin.toLowerCase().contains(forbidden), "effect attribution mixin must be addon-neutral: " + forbidden);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
