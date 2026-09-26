package com.gabri.magicteam.mixin;

import java.nio.file.Files;
import java.nio.file.Path;

/** Dependency-free contract for global hostile TargetEntityCastData filtering. */
public final class GlobalTargetGateContractTest {
    private static final Path POLICY = Path.of("src/main/java/com/gabri/magicteam/util/MagicTargetingPolicy.java");
    private static final Path ABSTRACT_SPELL = Path.of("src/main/java/com/gabri/magicteam/mixin/AbstractSpellMixin.java");
    private static final Path PLAYER_TICK = Path.of("src/main/java/com/gabri/magicteam/mixin/MagicManagerCastDispatchMixin.java");
    private static final Path MOB_DISPATCH = Path.of("src/main/java/com/gabri/magicteam/mixin/AbstractSpellCastingMobDispatchMixin.java");

    private GlobalTargetGateContractTest() {
    }

    public static void main(String[] args) throws Exception {
        check(Files.isRegularFile(POLICY), "shared magic target policy is missing");

        String policy = Files.readString(POLICY);
        String abstractSpell = Files.readString(ABSTRACT_SPELL);
        String playerTick = Files.readString(PLAYER_TICK);
        String mobDispatch = Files.readString(MOB_DISPATCH);

        check(policy.contains("TargetEntityCastData"),
                "global policy must understand Iron's standard selected-target data");
        check(policy.contains("TeamUtils.shouldBlockFriendlyFire"),
                "targeted hostile spells must use the central Magic Team policy");
        check(policy.contains("TeamUtils.isHarmful"),
                "support spells must not be rejected by the hostile target gate");

        check(abstractSpell.contains("MagicTargetingPolicy.shouldBlockSelectedTarget"),
                "player pre-cast/release dispatcher must use the shared target policy");
        check(playerTick.contains("MagicTargetingPolicy.shouldBlockSelectedTarget"),
                "player channel-tick override dispatcher must use the shared target policy");
        check(mobDispatch.contains("MagicTargetingPolicy.shouldBlockSelectedTarget"),
                "mob virtual dispatch must use the shared target policy");

        String combined = (policy + abstractSpell + playerTick + mobDispatch).toLowerCase();
        check(!combined.contains("traveloptics") && !combined.contains("familiars")
                        && !combined.contains("geomancy") && !combined.contains("cataclysm"),
                "global target gate must stay addon-neutral");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
