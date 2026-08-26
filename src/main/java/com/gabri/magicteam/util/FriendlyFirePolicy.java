package com.gabri.magicteam.util;

/**
 * Pure policy for deciding whether an allied interaction should be blocked as friendly fire.
 * Kept independent from Minecraft types so the semantics can be regression-tested directly.
 */
final class FriendlyFirePolicy {
    private FriendlyFirePolicy() {
    }

    static boolean shouldBlock(boolean allied, boolean hasTeamRelation, boolean friendlyFireAllowed) {
        return shouldBlock(false, allied, hasTeamRelation, friendlyFireAllowed);
    }

    static boolean shouldBlock(boolean sameEntity, boolean allied, boolean hasTeamRelation, boolean friendlyFireAllowed) {
        // Friendly fire is a relation between distinct entities. A spell applying
        // a helper/buff/debuff to its own caster must not be classified as teammate damage.
        if (sameEntity) {
            return false;
        }

        if (!allied) {
            return false;
        }

        // Preserve root-owner protection between distinct entities when Babel reports
        // an alliance without a scoreboard team (for example a projectile and its owner).
        if (!hasTeamRelation) {
            return true;
        }

        return !friendlyFireAllowed;
    }
}
