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
        // The displayTest is now handled exclusively in META-INF/mods.toml 
        // using displayTest="IGNORE_SERVER_VERSION" to satisfy modern Forge 1.20.1 
        // and avoid IDE deprecation warnings for ModLoadingContext.get().

        // Register the event handler for team damage protection
        MinecraftForge.EVENT_BUS.register(new TeamDamageEventHandler());
    }
}
