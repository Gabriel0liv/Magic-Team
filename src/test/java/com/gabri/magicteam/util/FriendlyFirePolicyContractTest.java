package com.gabri.magicteam.util;

/**
 * Dependency-free regression contract for Magic Team's hostile-magic protection matrix.
 * Vanilla scoreboard friendlyFire is deliberately an input here only to prove it cannot
 * change the Magic Team decision.
 */
public final class FriendlyFirePolicyContractTest {
    private FriendlyFirePolicyContractTest() {
    }

    public static void main(String[] args) {
        check(!FriendlyFirePolicy.shouldBlock(false, false, false),
                "non-allies must never be blocked by Magic Team");
        check(FriendlyFirePolicy.shouldBlock(true, false, false),
                "Babel owner/root alliances between distinct entities remain protected");
        check(FriendlyFirePolicy.shouldBlock(true, true, false),
                "allied hostile magic must be blocked when vanilla friendlyFire=false");
        check(FriendlyFirePolicy.shouldBlock(true, true, true),
                "allied hostile magic must stay blocked when vanilla friendlyFire=true");

        check(!FriendlyFirePolicy.shouldBlock(true, true, false, false),
                "an entity interacting with itself is not teammate hostile damage");
        check(FriendlyFirePolicy.shouldBlock(false, true, false, true),
                "distinct allied entities remain protected even when vanilla friendlyFire=true");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
