package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free contract for global hostile TargetEntityCastData filtering. */
public final class GlobalTargetGateContractTest {
    private static final Path ABSTRACT_SPELL = Path.of("src/main/java/com/gabri/magicteam/mixin/AbstractSpellMixin.java");

    private GlobalTargetGateContractTest() {
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(ABSTRACT_SPELL);

        check(source.contains("TargetEntityCastData"),
                "AbstractSpell global hook must understand Iron's standard selected-target data");
        check(source.contains("TeamUtils.shouldBlockFriendlyFire"),
                "targeted hostile spells must use the central Magic Team policy");
        check(source.contains("onServerPreCast") && source.contains("onServerCastTick") && source.contains("onCast"),
                "target gate must cover pre-cast, channel ticks, and release");
        check(source.contains("ci.cancel()"),
                "protected hostile target must be cancellable before addon side effects");
        check(source.contains("TeamUtils.isHarmful"),
                "support spells must not be rejected by the hostile target gate");
        check(!source.contains("traveloptics") && !source.contains("familiars")
                        && !source.contains("geomancy") && !source.contains("cataclysm"),
                "global target gate must stay addon-neutral");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
