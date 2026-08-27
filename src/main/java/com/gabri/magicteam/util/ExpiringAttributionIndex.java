package com.gabri.magicteam.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Small dependency-free index for temporary target -> source attribution.
 * Entries are valid through their expiry tick and are removed lazily or by cleanup.
 */
public final class ExpiringAttributionIndex {
    private final Map<UUID, Entry> entries = new HashMap<>();

    public void put(UUID targetId, UUID sourceId, long expiresAt) {
        if (targetId == null || sourceId == null) {
            return;
        }
        entries.put(targetId, new Entry(sourceId, expiresAt));
    }

    public UUID get(UUID targetId, long now) {
        if (targetId == null) {
            return null;
        }

        Entry entry = entries.get(targetId);
        if (entry == null) {
            return null;
        }

        if (now > entry.expiresAt) {
            entries.remove(targetId);
            return null;
        }

        return entry.sourceId;
    }

    public void cleanup(long now) {
        Iterator<Map.Entry<UUID, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now > iterator.next().getValue().expiresAt) {
                iterator.remove();
            }
        }
    }

    public int size() {
        return entries.size();
    }

    private record Entry(UUID sourceId, long expiresAt) {
    }
}
