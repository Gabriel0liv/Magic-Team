package com.gabri.magicteam.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Dependency-free behavioral contract for the expiring target -> source index
 * used by Flare Vacuum attribution.
 */
public final class FlareVacuumAttributionIndexContractTest {
    private static final String CLASS_NAME = "com.gabri.magicteam.util.ExpiringAttributionIndex";

    private FlareVacuumAttributionIndexContractTest() {
    }

    public static void main(String[] args) throws Exception {
        Class<?> type;
        try {
            type = Class.forName(CLASS_NAME);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("missing expiring attribution index", exception);
        }

        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object index = constructor.newInstance();

        Method put = type.getDeclaredMethod("put", UUID.class, UUID.class, long.class);
        Method get = type.getDeclaredMethod("get", UUID.class, long.class);
        Method cleanup = type.getDeclaredMethod("cleanup", long.class);
        Method size = type.getDeclaredMethod("size");
        put.setAccessible(true);
        get.setAccessible(true);
        cleanup.setAccessible(true);
        size.setAccessible(true);

        UUID target = UUID.randomUUID();
        UUID sourceA = UUID.randomUUID();
        UUID sourceB = UUID.randomUUID();

        put.invoke(index, target, sourceA, 120L);
        check(sourceA.equals(get.invoke(index, target, 119L)), "live attribution must resolve its source");
        check(sourceA.equals(get.invoke(index, target, 120L)), "attribution must remain valid through its expiry tick");

        put.invoke(index, target, sourceB, 180L);
        check(sourceB.equals(get.invoke(index, target, 121L)), "successful reapplication must replace the source");

        check(get.invoke(index, target, 181L) == null, "expired attribution must stop resolving");
        check(((Integer) size.invoke(index)) == 0, "expired lookup must remove the stale entry");

        UUID staleTarget = UUID.randomUUID();
        UUID liveTarget = UUID.randomUUID();
        put.invoke(index, staleTarget, sourceA, 200L);
        put.invoke(index, liveTarget, sourceB, 260L);
        cleanup.invoke(index, 201L);
        check(get.invoke(index, staleTarget, 201L) == null, "cleanup must remove expired entries");
        check(sourceB.equals(get.invoke(index, liveTarget, 201L)), "cleanup must preserve live entries");
        check(((Integer) size.invoke(index)) == 1, "cleanup must leave only live entries");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
