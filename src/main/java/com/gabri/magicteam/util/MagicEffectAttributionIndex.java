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

        int duration = effectInstance.getDuration();
        long expiresAt = duration < 0
                ? Long.MAX_VALUE
                : currentTick + Math.max(1L, (long) duration) + EXPIRY_GRACE_TICKS;

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

    public static void removeAll(LivingEntity target) {
        if (target == null) {
            return;
        }
        UUID targetId = target.getUUID();
        ATTRIBUTIONS.keySet().removeIf(key -> key.targetId().equals(targetId));
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
