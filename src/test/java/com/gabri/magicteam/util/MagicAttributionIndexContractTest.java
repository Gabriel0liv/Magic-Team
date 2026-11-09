package com.gabri.magicteam.util;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free source contract for the persistent attribution layer. */
public final class MagicAttributionIndexContractTest {
    private static final Path ATTRIBUTION = Path.of("src/main/java/com/gabri/magicteam/util/MagicAttribution.java");
    private static final Path INDEX = Path.of("src/main/java/com/gabri/magicteam/util/MagicAttributionIndex.java");
    private static final Path CONTEXT = Path.of("src/main/java/com/gabri/magicteam/util/MagicTeamEffectContext.java");

    private MagicAttributionIndexContractTest() {
    }

    public static void main(String[] args) throws Exception {
        String attribution = Files.readString(ATTRIBUTION);
        String index = Files.readString(INDEX);
        String context = Files.readString(CONTEXT);

        check(attribution.contains("UUID sourceEntityId"), "attribution must store source entity UUID");
        check(attribution.contains("UUID rootCasterId"), "attribution must store root caster UUID");
        check(attribution.contains("String spellId"), "attribution must store spell id as value data");
        check(attribution.contains("SpellBehavior behavior"), "attribution must preserve support/hostile behavior");
        check(attribution.contains("InteractionType interactionType"), "attribution must preserve interaction type");
        check(attribution.contains("long expiresAtTick"), "attribution must expire explicitly");
        check(attribution.contains("currentTick >= expiresAtTick"), "expiry boundary must be deterministic");

        check(index.contains("ConcurrentHashMap"), "attribution index must support server-side concurrent access safely");
        check(index.contains("record(Entity entity, MagicAttribution attribution)"), "entity record API missing");
        check(index.contains("get(Entity entity, long currentTick)"), "entity lookup API missing");
        check(index.contains("remove(Entity entity)"), "entity removal API missing");
        check(index.contains("cleanup(long currentTick)"), "expiry cleanup API missing");
        check(index.contains("ATTRIBUTIONS.remove(entityId, attribution)"), "expired lookup must remove stale attribution");

        check(context.contains("MagicAttribution currentAttribution()"), "live context must expose attribution snapshot");
        check(context.contains("context.persistentAttribution"), "nested/re-entered context must preserve persistent attribution");
        check(context.contains("MagicAttribution.interactionFor(behavior)"), "generic live context must preserve support/hostile classification");

        for (String forbidden : new String[]{
                "com.gametechbc.traveloptics",
                "com.gametechbc.gtbcs_geomancy_plus",
                "net.alshanex.alshanex_familiars",
                "com.github.L_Ender.cataclysm"
        }) {
            check(!attribution.contains(forbidden), "attribution value must be addon-neutral: " + forbidden);
            check(!index.contains(forbidden), "attribution index must be addon-neutral: " + forbidden);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
