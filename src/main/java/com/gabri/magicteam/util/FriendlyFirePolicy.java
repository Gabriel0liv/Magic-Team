package com.gabri.magicteam.util;

/**
 * Pure policy for deciding whether an allied interaction should be blocked as friendly fire.
 * Kept independent from Minecraft types so the semantics can be regression-tested directly.
 */
final class FriendlyFirePolicy {
    private FriendlyFirePolicy() {
    }

    static boolean shouldBlock(boolean allied, boolean hasTeamRelation, boolean friendlyFireAllowed) {
        if (!allied) {
            return false;
        }

        // Preserve the existing self/root-owner protection when Babel reports an alliance
        // that is not backed by a scoreboard team (for example an entity and its owner).
        if (!hasTeamRelation) {
            return true;
        }

        return !friendlyFireAllowed;
    }
}
