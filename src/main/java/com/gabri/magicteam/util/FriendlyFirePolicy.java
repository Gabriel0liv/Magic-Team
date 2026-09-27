package com.gabri.magicteam.util;

/**
 * Pure policy for deciding whether Magic Team must block a hostile magical
 * interaction between two resolved entities.
 *
 * <p>Vanilla scoreboard friendlyFire is intentionally not part of this policy.
 * Vanilla combat remains Minecraft's responsibility; this helper is used only
 * from Magic Team's magic/addon interception points.</p>
 */
final class FriendlyFirePolicy {
    private FriendlyFirePolicy() {
    }

    static boolean shouldBlock(boolean sameResolvedEntity, boolean allied) {
        // Self/root-self spell helpers are not teammate hostile damage.
        if (sameResolvedEntity) {
            return false;
        }

        // Hostile magic is blocked between distinct allies whenever Magic Team
        // is enabled. The caller handles the global enabled/disabled switch.
        return allied;
    }
}
