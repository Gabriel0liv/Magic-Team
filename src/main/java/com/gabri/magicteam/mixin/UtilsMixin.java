package com.gabri.magicteam.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
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
        // Se a magia é nociva e eles são aliados, bloqueamos a mira.
        if (spell != null && TeamUtils.isHarmful(spell)) {
            return (target) -> {
                if (TeamUtils.areAllies(caster, target)) {
                    TeamUtils.sendBlockedMessage(caster);
                    return false;
                }
                return original == null || original.test(target);
            };
        }
        // Se for uma magia benéfica (ou nula), permitimos mirar em aliados normalmente.
        return original;
    }

    /**
     * Hook para o Raycast visual (esferas de mira e outlines).
     */
    @Inject(method = "raycastForEntity(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;FZF)Lnet/minecraft/world/phys/HitResult;", 
            at = @At("RETURN"), cancellable = true)
    private static void onRaycastForEntity(Level level, Entity originEntity, float distance, boolean checkForBlocks, float bbInflation, CallbackInfoReturnable<HitResult> cir) {
        HitResult hitResult = cir.getReturnValue();
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity target = entityHitResult.getEntity();
            
            // Se são aliados, precisamos saber se a magia atual do player é nociva
            if (TeamUtils.areAllies(originEntity, target)) {
                boolean isHarmful = false; // Por padrão permitimos (bloqueio real é no preCastTargetHelper)
                
                if (originEntity instanceof net.minecraft.world.entity.player.Player player) {
                    io.redspace.ironsspellbooks.api.magic.SpellSelectionManager manager = new io.redspace.ironsspellbooks.api.magic.SpellSelectionManager(player);
                    if (manager.getSelection() != null) {
                        isHarmful = TeamUtils.isHarmful(manager.getSelection().spellData.getSpell());
                    }
                }

                if (isHarmful) {
                    // Se for nociva, cancelamos o hit visual (faz a mira "passar" pelo aliado)
                    net.minecraft.world.phys.Vec3 eyePos = originEntity.getEyePosition();
                    net.minecraft.world.phys.Vec3 viewVec = originEntity.getViewVector(1.0F);
                    if (eyePos != null && viewVec != null) {
                        net.minecraft.world.phys.Vec3 scaledView = viewVec.scale((double)distance);
                        if (scaledView != null) {
                            net.minecraft.world.phys.Vec3 missPos = eyePos.add(scaledView);
                            if (missPos != null) {
                                net.minecraft.core.BlockPos blockPos = net.minecraft.core.BlockPos.containing(missPos);
                                if (blockPos != null) {
                                    cir.setReturnValue(net.minecraft.world.phys.BlockHitResult.miss(missPos, net.minecraft.core.Direction.UP, blockPos));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}