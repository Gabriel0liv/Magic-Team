package com.gabri.magicteam.util;

import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Shared "context bridge" for optional addon adapters.
 *
 * <p>An adapter only marks the start and end of an addon ability method as
 * hostile magic from {@code source}. Everything inside is then handled by the
 * global gates instead of by adapter-specific redirects:</p>
 * <ul>
 *     <li>{@code Entity#isAlliedTo} answers with Babel alliance, wherever the
 *     addon calls it (inside lambdas, on any receiver type);</li>
 *     <li>{@code LivingEntity#hurt} blocks damage to allies;</li>
 *     <li>effects, forced movement, fire and effect removal are gated.</li>
 * </ul>
 *
 * <p>This way an adapter depends only on the addon method name, never on how
 * the method body is compiled. Begin/end must be paired: each call records
 * whether it actually pushed, so toggling Magic Team or a client-side call
 * never unbalances the context stack.</p>
 */
public final class AdapterContextBridge {
    private static final ThreadLocal<Deque<Boolean>> SCOPES = new ThreadLocal<>();

    private AdapterContextBridge() {
    }

    public static void beginHostile(Entity source) {
        boolean pushed = source != null
                && !source.level().isClientSide()
                && TeamUtils.isEnabled();
        if (pushed) {
            MagicTeamEffectContext.push(source, MagicTeamEffectContext.InteractionType.HARMFUL);
        }

        Deque<Boolean> scopes = SCOPES.get();
        if (scopes == null) {
            scopes = new ArrayDeque<>();
            SCOPES.set(scopes);
        }
        scopes.push(pushed);
    }

    public static void end() {
        Deque<Boolean> scopes = SCOPES.get();
        if (scopes == null) {
            return;
        }

        boolean pushed = !scopes.isEmpty() && scopes.pop();
        if (pushed) {
            MagicTeamEffectContext.pop();
        }
        if (scopes.isEmpty()) {
            SCOPES.remove();
        }
    }

    /** Drops any unbalanced scopes (called with the tick-end leak cleanup). */
    public static void clear() {
        SCOPES.remove();
    }
}
