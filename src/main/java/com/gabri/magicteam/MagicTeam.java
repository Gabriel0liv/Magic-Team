package com.gabri.magicteam;

import com.gabri.magicteam.util.MagicTeamConfig;
import com.gabri.magicteam.MagicTeamCommands;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
        LOGGER.info("Magic-Team loaded: fixed team rules enabled.");
    }

    private void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        MagicTeamCommands.register(event.getDispatcher());
    }
}
