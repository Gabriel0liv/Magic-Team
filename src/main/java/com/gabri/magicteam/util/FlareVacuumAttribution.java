package com.gabri.magicteam.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Preserves the original Gyro Slash caster across Flare Vacuum's delayed
 * detonation without retaining strong references to casters between ticks.
 */
public final class FlareVacuumAttribution {
    private static final long EXPIRY_GRACE_TICKS = 5L;
    private static final ExpiringAttributionIndex INDEX = new ExpiringAttributionIndex();
    private static final ThreadLocal<Deque<Frame>> ACTIVE = ThreadLocal.withInitial(ArrayDeque::new);

    private FlareVacuumAttribution() {
    }

    public static void record(LivingEntity target, Entity source, int durationTicks) {
        if (target == null || source == null) {
            return;
        }

        LivingEntity caster = resolveCasterFromSource(source);
        if (caster == null) {
            return;
        }

        long now = target.level().getGameTime();
        long lifetime = Math.max(1, durationTicks) + EXPIRY_GRACE_TICKS;
        INDEX.cleanup(now);
        INDEX.put(target.getUUID(), caster.getUUID(), now + lifetime);
    }

    public static void begin(LivingEntity target) {
        LivingEntity source = null;
        if (target != null) {
            long now = target.level().getGameTime();
            UUID sourceId = INDEX.get(target.getUUID(), now);
            source = resolveLoadedLivingEntity(target, sourceId);
        }
        ACTIVE.get().push(new Frame(source));
    }

    public static void end() {
        Deque<Frame> stack = ACTIVE.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            ACTIVE.remove();
        }
    }

    public static LivingEntity getActiveSource() {
        Deque<Frame> stack = ACTIVE.get();
        Frame frame = stack.peek();
        if (frame == null) {
            ACTIVE.remove();
            return null;
        }
        return frame.source;
    }

    public static int getActiveDepth() {
        Deque<Frame> stack = ACTIVE.get();
        int depth = stack.size();
        if (depth == 0) {
            ACTIVE.remove();
        }
        return depth;
    }

    public static void clearActiveContext() {
        ACTIVE.remove();
    }

    private static LivingEntity resolveCasterFromSource(Entity source) {
        Entity root = TeamUtils.getRootOwner(source);
        if (root instanceof LivingEntity living) {
            return living;
        }

        if (source instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }

        return source instanceof LivingEntity living ? living : null;
    }

    private static LivingEntity resolveLoadedLivingEntity(LivingEntity target, UUID sourceId) {
        if (sourceId == null) {
            return null;
        }

        MinecraftServer server = target.getServer();
        if (server != null) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(sourceId);
                if (entity instanceof LivingEntity living) {
                    return living;
                }
            }
        }

        if (target.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(sourceId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }

        return null;
    }

    private record Frame(LivingEntity source) {
    }
}
