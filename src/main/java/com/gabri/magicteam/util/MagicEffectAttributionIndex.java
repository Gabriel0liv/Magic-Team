package com.gabri.magicteam.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Addon-neutral attribution for effects whose hostile/support behavior continues
 * after the spell call stack has ended.
 */
public final class MagicEffectAttributionIndex {
    private static final long EXPIRY_GRACE_TICKS = 20L;
    private static final Map<Key, MagicAttribution> ATTRIBUTIONS = new ConcurrentHashMap<>();

    private MagicEffectAttributionIndex() {
    }

    public static void record(LivingEntity target,
                              MobEffectInstance effectInstance,
                              MagicAttribution attribution,
                              long currentTick) {
        if (target == null || effectInstance == null || attribution == null) {
            return;
        }

        String effectId = effectId(effectInstance.getEffect());
        if (effectId.isEmpty()) {
            return;
        }

        long effectExpiry;
        int duration = effectInstance.getDuration();
        if (duration < 0 || attribution.expiresAtTick() == Long.MAX_VALUE) {
            effectExpiry = Long.MAX_VALUE;
        } else {
            effectExpiry = currentTick + Math.max(EXPIRY_GRACE_TICKS, (long) duration + EXPIRY_GRACE_TICKS);
        }

        long expiresAt = attribution.expiresAtTick() == Long.MAX_VALUE || effectExpiry == Long.MAX_VALUE
                ? Long.MAX_VALUE
                : Math.max(attribution.expiresAtTick(), effectExpiry);

        ATTRIBUTIONS.put(
                new Key(target.getUUID(), effectId),
                attribution.withExpiry(expiresAt)
        );
    }

    public static MagicAttribution get(LivingEntity target, MobEffect effect, long currentTick) {
        if (target == null || effect == null) {
            return null;
        }
        return get(target.getUUID(), effectId(effect), currentTick);
    }

    public static MagicAttribution get(UUID targetId, String effectId, long currentTick) {
        if (targetId == null || effectId == null || effectId.isBlank()) {
            return null;
        }

        Key key = new Key(targetId, effectId);
        MagicAttribution attribution = ATTRIBUTIONS.get(key);
        if (attribution == null) {
            return null;
        }
        if (attribution.isExpired(currentTick)) {
            ATTRIBUTIONS.remove(key, attribution);
            return null;
        }
        return attribution;
    }

    public static void remove(LivingEntity target, MobEffect effect) {
        if (target == null || effect == null) {
            return;
        }
        ATTRIBUTIONS.remove(new Key(target.getUUID(), effectId(effect)));
    }

    public static void cleanup(long currentTick) {
        ATTRIBUTIONS.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTick));
    }

    private static String effectId(MobEffect effect) {
        ResourceLocation id = effect == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return id == null ? "" : id.toString();
    }

    private record Key(UUID targetId, String effectId) {
    }
}
