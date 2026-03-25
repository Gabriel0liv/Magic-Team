package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsAlliedTo(Entity other, CallbackInfoReturnable<Boolean> cir) {
        // Se a aliança global estiver desligada, deixamos a lógica original ou tratamos como não aliados
        if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableGlobalAlliance.get()) {
            return;
        }

        boolean areAllies = TeamUtils.areAllies((Entity) (Object) this, other);
        
        if (areAllies) {
            // Se são aliados, respeitamos o toggle de bloqueio de dano
            // Se o bloqueio de dano estiver DESLIGADO, retornamos FALSE para permitir projéteis/ataques
            if (!com.gabri.magicteam.MagicTeamConfig.SERVER.enableDamageBlock.get()) {
                cir.setReturnValue(false);
            } else {
                cir.setReturnValue(true);
            }
        } else {
            // Se NÃO são aliados segundo o Magic-Team (ex: Bypass ativo ou times diferentes),
            // forçamos o retorno FALSE ignorando qualquer time do Minecraft Vanilla.
            cir.setReturnValue(false);
        }
    }
}
