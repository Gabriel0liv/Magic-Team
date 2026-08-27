package com.gabri.magicteam;

import com.gabri.magicteam.util.FlareVacuumAttribution;
import com.gabri.magicteam.util.MagicTeamConfig;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Magic Team mod.
 * This is a server-side mod that overrides Iron's Spells targeting.
 */
@Mod("magic_team")
public class MagicTeam {
    public static final String MODID = "magic_team";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public MagicTeam() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MagicTeamConfig.SERVER_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MagicTeamConfig::onConfigEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        LOGGER.info("Magic-Team loaded: fixed team rules enabled.");
    }

    private void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        MagicTeamCommands.register(event.getDispatcher());
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        int depth = MagicTeamEffectContext.getDepth();
        if (depth > 0) {
            String context = MagicTeamEffectContext.describeCurrentContext();
            LOGGER.warn("Magic-Team detected a leaked effect context at server tick end; clearing it to prevent unrelated damage/effects from inheriting the stale scope. {}", context);
            MagicTeamEffectContext.clear();
        }

        int flareVacuumDepth = FlareVacuumAttribution.getActiveDepth();
        if (flareVacuumDepth > 0) {
            LOGGER.warn("Magic-Team detected a leaked Flare Vacuum attribution context at server tick end; clearing depth={}", flareVacuumDepth);
            FlareVacuumAttribution.clearActiveContext();
        }
    }
}
