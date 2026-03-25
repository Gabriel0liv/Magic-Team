package com.gabri.magicteam;

import com.gabri.magicteam.events.TeamDamageEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * Main entry point for Magic Team mod.
 * This is a server-side mod that overrides Iron's Spells targeting.
 */
@Mod("magic_team")
public class MagicTeam {
    public static final String MODID = "magic_team";

    public MagicTeam() {
        // Register Config
        MagicTeamConfig.register();

        // Register the event handler for team damage protection
        MinecraftForge.EVENT_BUS.register(new TeamDamageEventHandler());
        
        // Register Command
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        
        // Load Bypass Persistence
        com.gabri.magicteam.util.TeamUtils.loadBypassData();
    }

    private void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        MagicTeamCommands.register(event.getDispatcher());
    }
}
