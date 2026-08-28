package com.gabri.magicteam.util;

/**
 * Dependency-free regression contract for Magic Team's hostile-magic protection matrix.
 * Vanilla scoreboard friendlyFire is deliberately absent from the policy API: it cannot
 * change Magic Team's hostile-magic decision.
 */
public final class FriendlyFirePolicyContractTest {
    private FriendlyFirePolicyContractTest() {
    }

    public static void main(String[] args) {
        check(!FriendlyFirePolicy.shouldBlock(false, false),
                "non-allies must never be blocked by Magic Team");
        check(FriendlyFirePolicy.shouldBlock(false, true),
                "hostile magic between distinct allies must be blocked");
        check(!FriendlyFirePolicy.shouldBlock(true, true),
                "a self/root-self interaction is not teammate hostile damage");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
