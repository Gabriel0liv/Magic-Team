package com.gabri.magicteam.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import com.gabri.magicteam.util.TeamUtils;
import com.gabri.magicteam.MagicTeamConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import java.util.function.Predicate;

@Mixin(value = Utils.class, remap = false)
public class UtilsMixin {

    /**
     * Hook Mestre: Modificamos o Filtro (Predicate) do seletor de alvos.
     */
    @ModifyVariable(
        method = "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Lnet/minecraft/world/entity/LivingEntity;",
        at = @At("HEAD"),
        argsOnly = true,
        remap = false,
        require = 0
    )
    private static Predicate<LivingEntity> wrapFilter_3153(Predicate<LivingEntity> original, Level level, LivingEntity caster, MagicData magicData, AbstractSpell spell) {
        return wrapPredicate(original, caster, spell);
    }

    @ModifyVariable(
        method = "preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZLjava/util/function/Predicate;)Z",
        at = @At("HEAD"),
        argsOnly = true,
        remap = false,
        require = 0
    )
    private static Predicate<LivingEntity> wrapFilter_3154(Predicate<LivingEntity> original, Level level, LivingEntity caster, MagicData magicData, AbstractSpell spell) {
        return wrapPredicate(original, caster, spell);
    }

    private static Predicate<LivingEntity> wrapPredicate(Predicate<LivingEntity> original, LivingEntity caster, AbstractSpell spell) {
        if (!MagicTeamConfig.SERVER.enableTargetingBlock.get()) return original;

        if (spell != null && TeamUtils.isHarmful(spell)) {
            return (target) -> {
                boolean isAlly = TeamUtils.areAllies(caster, target);
                if (isAlly) {
                    TeamUtils.sendBlockedMessage(caster);
                    return false;
                }
                return original == null || original.test(target);
            };
        }
        return original;
    }


    /**
     * Hook para o Raycast visual (remove o outline instantaneamente).
     */
    @SuppressWarnings("null")
    @Inject(method = "raycastForEntity(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;FZF)Lnet/minecraft/world/phys/HitResult;", 
            at = @At("RETURN"), cancellable = true)
    private static void onRaycastForEntity(Level level, Entity originEntity, float distance, boolean checkForBlocks, float bbInflation, CallbackInfoReturnable<HitResult> cir) {
        if (!MagicTeamConfig.SERVER.enableTargetingBlock.get()) return;

        HitResult hitResult = cir.getReturnValue();
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity target = entityHitResult.getEntity();
            if (TeamUtils.areAllies(originEntity, target)) {
                Vec3 eyePos = originEntity.getEyePosition();
                Vec3 viewVec = originEntity.getViewVector(1.0F);
                if (eyePos != null && viewVec != null) {
                    Vec3 scaledView = viewVec.scale((double)distance);
                    if (scaledView != null) {
                        Vec3 missPos = eyePos.add(scaledView);
                        if (missPos != null) {
                            cir.setReturnValue(BlockHitResult.miss(missPos, Direction.UP, BlockPos.containing(missPos)));
                        }
                    }
                }
            }
        }
    }
}