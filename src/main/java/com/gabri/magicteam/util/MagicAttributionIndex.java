package com.gabri.magicteam.util;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local attribution index keyed by entity UUID.
 *
 * <p>The index intentionally stores only {@link MagicAttribution} values, so
 * addon classes and live entity instances are never retained by the tracker.</p>
 */
public final class MagicAttributionIndex {
    public static final long ACTIVE_ENTITY_TTL_TICKS = 20L * 60L * 5L;

    private static final Map<UUID, MagicAttribution> ATTRIBUTIONS = new ConcurrentHashMap<>();

    private MagicAttributionIndex() {
    }

    public static void record(Entity entity, MagicAttribution attribution) {
        if (entity == null) {
            return;
        }
        record(entity.getUUID(), attribution);
    }

    public static void record(UUID entityId, MagicAttribution attribution) {
        if (entityId == null || attribution == null) {
            return;
        }
        ATTRIBUTIONS.put(entityId, attribution);
    }

    public static MagicAttribution get(Entity entity, long currentTick) {
        return entity == null ? null : get(entity.getUUID(), currentTick);
    }

    public static MagicAttribution get(UUID entityId, long currentTick) {
        if (entityId == null) {
            return null;
        }

        MagicAttribution attribution = ATTRIBUTIONS.get(entityId);
        if (attribution == null) {
            return null;
        }

        if (attribution.isExpired(currentTick)) {
            ATTRIBUTIONS.remove(entityId, attribution);
            return null;
        }

        return attribution;
    }

    /**
     * Keeps attribution alive while the attributed entity is actively ticking.
     * This avoids a fixed lifetime for long-lived summons while still allowing
     * abandoned/unloaded entries to expire naturally.
     */
    public static MagicAttribution refresh(Entity entity, long currentTick) {
        MagicAttribution attribution = get(entity, currentTick);
        if (attribution == null) {
            return null;
        }

        MagicAttribution refreshed = attribution.withExpiry(currentTick + ACTIVE_ENTITY_TTL_TICKS);
        record(entity, refreshed);
        return refreshed;
    }

    public static void remove(Entity entity) {
        if (entity != null) {
            remove(entity.getUUID());
        }
    }

    public static void remove(UUID entityId) {
        if (entityId != null) {
            ATTRIBUTIONS.remove(entityId);
        }
    }

    public static void cleanup(long currentTick) {
        ATTRIBUTIONS.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTick));
    }

    static int size() {
        return ATTRIBUTIONS.size();
    }

    static void clearForTests() {
        ATTRIBUTIONS.clear();
    }
}
