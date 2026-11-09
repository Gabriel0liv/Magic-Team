package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free contract for addon-neutral magic alliance resolution. */
public final class GenericMagicAllianceContractTest {
    private static final Path ENTITY_MIXIN = Path.of("src/main/java/com/gabri/magicteam/mixin/EntityMixin.java");

    private GenericMagicAllianceContractTest() {
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(ENTITY_MIXIN);

        check(source.contains("MagicTeamEffectContext.hasContext()"),
                "active spell context must make isAlliedTo use Babel relationship semantics");
        check(source.contains("MagicAttributionIndex.get"),
                "persistently attributed entities must use Babel relationship semantics");
        check(source.contains("TeamUtils.areAllies"),
                "magic alliance compatibility must delegate to Babel through TeamUtils");
        check(!source.contains("traveloptics") && !source.contains("familiars")
                        && !source.contains("geomancy") && !source.contains("cataclysm"),
                "magic alliance compatibility must not depend on addon names");
        check(!source.contains("isAllowFriendlyFire"),
                "relationship resolution must remain independent of vanilla friendlyFire");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
