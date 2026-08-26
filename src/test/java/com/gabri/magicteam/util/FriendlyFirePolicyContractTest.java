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
                "Babel owner/self-root alliances between distinct entities remain protected");
        check(FriendlyFirePolicy.shouldBlock(true, true, false),
                "allied scoreboard teams with friendlyFire=false must be blocked");
        check(!FriendlyFirePolicy.shouldBlock(true, true, true),
                "allied scoreboard teams with friendlyFire=true must be allowed");

        check(!FriendlyFirePolicy.shouldBlock(true, true, false, false),
                "an entity interacting with itself is not friendly fire");
        check(FriendlyFirePolicy.shouldBlock(false, true, false, false),
                "distinct entities with the same Babel root must remain protected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
