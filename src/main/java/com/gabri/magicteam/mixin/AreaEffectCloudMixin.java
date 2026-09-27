package com.gabri.magicteam.mixin;

import com.gabri.magicteam.util.MagicAttribution;
import com.gabri.magicteam.util.MagicAttributionIndex;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.TeamUtils;
import net.minecraft.world.entity.AreaEffectCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AreaEffectCloud.class)
public class AreaEffectCloudMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        AreaEffectCloud cloud = (AreaEffectCloud) (Object) this;
        MagicAttribution attribution = MagicTeamEffectContext.currentAttribution();
        if (attribution == null && TeamUtils.isEnabled()) {
            attribution = MagicAttributionIndex.get(cloud, cloud.level().getGameTime());
        }

        if (attribution != null) {
            MagicTeamEffectContext.push(cloud, attribution);
        } else {
            MagicTeamEffectContext.pushVanillaPotion(cloud);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickEnd(CallbackInfo ci) {
        MagicTeamEffectContext.pop();
    }
}
