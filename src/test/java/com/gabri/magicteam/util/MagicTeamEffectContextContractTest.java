package com.gabri.magicteam.util;

/**
 * Dependency-free regression contract for MagicTeamEffectContext lifecycle semantics.
 */
public final class MagicTeamEffectContextContractTest {
    private MagicTeamEffectContextContractTest() {
    }

    public static void main(String[] args) {
        MagicTeamEffectContext.clear();
        check(MagicTeamEffectContext.getDepth() == 0, "context starts empty");

        MagicTeamEffectContext.push((net.minecraft.world.entity.Entity) null);
        check(MagicTeamEffectContext.getDepth() == 1, "entity scope increments depth");
        check(MagicTeamEffectContext.getOrigin() == MagicTeamEffectContext.Origin.ENTITY_SCOPE,
                "plain entity push is classified as ENTITY_SCOPE");
        check(MagicTeamEffectContext.getInteractionType() == MagicTeamEffectContext.InteractionType.GENERIC,
                "plain entity push defaults to GENERIC interaction intent");
        check(MagicTeamEffectContext.shouldFilterDamage(),
                "generic entity scopes preserve synchronous magic damage filtering");
        MagicTeamEffectContext.pop();

        MagicTeamEffectContext.push((net.minecraft.world.entity.Entity) null,
                MagicTeamEffectContext.InteractionType.HARMFUL);
        check(MagicTeamEffectContext.isHarmfulInteraction(),
                "explicit harmful scope is classified as HARMFUL");
        check(MagicTeamEffectContext.shouldFilterDamage(),
                "harmful scopes filter synchronous magic damage");

        MagicTeamEffectContext.push((net.minecraft.world.entity.Entity) null,
                MagicTeamEffectContext.InteractionType.BENEFICIAL);
        check(MagicTeamEffectContext.getInteractionType() == MagicTeamEffectContext.InteractionType.BENEFICIAL,
                "nested beneficial scope is visible");
        check(!MagicTeamEffectContext.shouldFilterDamage(),
                "beneficial scopes must not classify damage as hostile");
        MagicTeamEffectContext.pop();
        check(MagicTeamEffectContext.getInteractionType() == MagicTeamEffectContext.InteractionType.HARMFUL,
                "pop restores previous interaction intent");
        MagicTeamEffectContext.pop();

        MagicTeamEffectContext.pushVanillaPotion(null);
        check(MagicTeamEffectContext.getOrigin() == MagicTeamEffectContext.Origin.VANILLA_POTION,
                "vanilla potion scope is classified explicitly");
        check(MagicTeamEffectContext.getInteractionType() == MagicTeamEffectContext.InteractionType.GENERIC,
                "vanilla potion defaults to GENERIC interaction intent");
        check(!MagicTeamEffectContext.shouldFilterDamage(),
                "vanilla potion scope must not filter LivingEntity#hurt");

        MagicTeamEffectContext.clear();
        check(!MagicTeamEffectContext.hasContext(), "clear removes all context state");
        check(MagicTeamEffectContext.getSource() == null && MagicTeamEffectContext.getOrigin() == null,
                "empty context reads are null without allocating state");
        MagicTeamEffectContext.pop();
        check(MagicTeamEffectContext.getDepth() == 0, "pop on an empty context is a safe no-op");

        // Passenger boundary: a nested tick must not inherit the outer scope.
        MagicTeamEffectContext.push((net.minecraft.world.entity.Entity) null,
                MagicTeamEffectContext.InteractionType.HARMFUL);
        MagicTeamEffectContext.suspend();
        check(MagicTeamEffectContext.getDepth() == 0, "suspend hides the outer scope");
        check(!MagicTeamEffectContext.shouldFilterDamage(), "suspended scope must not filter nested damage");
        check(MagicTeamEffectContext.getSuspendedDepth() == 1, "suspend is tracked for leak detection");
        MagicTeamEffectContext.push((net.minecraft.world.entity.Entity) null,
                MagicTeamEffectContext.InteractionType.BENEFICIAL);
        MagicTeamEffectContext.pop();
        MagicTeamEffectContext.resume();
        check(MagicTeamEffectContext.getDepth() == 1 && MagicTeamEffectContext.isHarmfulInteraction(),
                "resume restores the outer scope intact");
        check(MagicTeamEffectContext.getSuspendedDepth() == 0, "resume clears the suspended marker");
        MagicTeamEffectContext.pop();

        MagicTeamEffectContext.suspend();
        MagicTeamEffectContext.resume();
        check(MagicTeamEffectContext.getDepth() == 0, "suspending an empty context restores an empty context");

        MagicTeamEffectContext.push((net.minecraft.world.entity.Entity) null);
        MagicTeamEffectContext.suspend();
        MagicTeamEffectContext.clear();
        check(MagicTeamEffectContext.getDepth() == 0 && MagicTeamEffectContext.getSuspendedDepth() == 0,
                "clear also drops suspended scopes");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
