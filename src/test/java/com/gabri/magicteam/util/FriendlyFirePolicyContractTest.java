package com.gabri.magicteam.util;

/**
 * Dependency-free regression contract for the friendly-fire decision matrix.
 * Run with Java assertions enabled or invoke main directly after compiling test sources.
 */
public final class FriendlyFirePolicyContractTest {
    private FriendlyFirePolicyContractTest() {
    }

    public static void main(String[] args) {
        check(!FriendlyFirePolicy.shouldBlock(false, false, false),
                "non-allies must never be blocked as friendly fire");
        check(FriendlyFirePolicy.shouldBlock(true, false, false),
                "Babel owner/self alliances without a scoreboard team remain protected");
        check(FriendlyFirePolicy.shouldBlock(true, true, false),
                "allied scoreboard teams with friendlyFire=false must be blocked");
        check(!FriendlyFirePolicy.shouldBlock(true, true, true),
                "allied scoreboard teams with friendlyFire=true must be allowed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
