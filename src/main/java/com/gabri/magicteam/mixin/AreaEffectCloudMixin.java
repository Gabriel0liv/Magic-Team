package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraft.world.entity.AreaEffectCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AreaEffectCloud.class)
public class AreaEffectCloudMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        MagicTeamEffectContext.pushVanillaPotion((AreaEffectCloud) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickEnd(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
