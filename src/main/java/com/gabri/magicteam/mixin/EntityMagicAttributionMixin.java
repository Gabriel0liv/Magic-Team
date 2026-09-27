package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicEffectAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

/**
 * Generic server boundary for magic entities that outlive their cast stack.
 *
 * <p>Entities spawned while a proven spell context is active inherit only the
 * stable MagicAttribution value. Later, when the server processes an attributed
 * entity, the whole non-passenger tick executes inside a temporary magic context.
 * The wrapper targets the stable method boundary instead of an invocation inside
 * the method body, which is less sensitive to Forge/Arclight tick rewrites.</p>
 */
@Mixin(ServerLevel.class)
public abstract class EntityMagicAttributionMixin {
    @Unique
    private static final long MAGIC_TEAM_CLEANUP_INTERVAL_TICKS = 200L;

    @Unique
    private static final ThreadLocal<Deque<Boolean>> MAGIC_TEAM_TICK_SCOPES =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(
            method = "addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD")
    )
    private void magicTeam$inheritAttributionOnSpawn(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!TeamUtils.isEnabled() || entity == null) {
            return;
        }

        MagicAttribution attribution = MagicTeamEffectContext.currentAttribution();
        if (attribution != null) {
            MagicAttributionIndex.record(entity, attribution);
        }
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void magicTeam$beginPersistentMagicContext(Entity entity, CallbackInfo ci) {
        boolean pushed = false;
        if (TeamUtils.isEnabled() && entity != null) {
            ServerLevel level = (ServerLevel) (Object) this;
            MagicAttribution attribution = MagicAttributionIndex.refresh(entity, level.getGameTime());
            if (attribution != null) {
                MagicTeamEffectContext.push(entity, attribution);
                pushed = true;
            }
        }

        MAGIC_TEAM_TICK_SCOPES.get().push(pushed);
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void magicTeam$endPersistentMagicContext(Entity entity, CallbackInfo ci) {
        Deque<Boolean> scopes = MAGIC_TEAM_TICK_SCOPES.get();
        boolean pushed = !scopes.isEmpty() && scopes.pop();
        if (pushed) {
            MagicTeamEffectContext.pop();
        }
        if (scopes.isEmpty()) {
            MAGIC_TEAM_TICK_SCOPES.remove();
        }
    }

    @Inject(
            method = "tick(Ljava/util/function/BooleanSupplier;)V",
            at = @At("RETURN")
    )
    private void magicTeam$cleanupExpiredAttribution(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        long gameTime = level.getGameTime();
        if (gameTime % MAGIC_TEAM_CLEANUP_INTERVAL_TICKS != 0L) {
            return;
        }

        MagicAttributionIndex.cleanup(gameTime);
        MagicEffectAttributionIndex.cleanup(gameTime);
    }
}
