package com.gabri.magicteam;

import com.gabri.magicteam.util.AdapterContextBridge;
import com.gabri.magicteam.util.MagicTeamConfig;
import com.gabri.magicteam.util.MagicTeamEffectContext;
import com.gabri.magicteam.util.OptionalAdapterDiagnostics;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for the server-side Magic Team mod. */
@Mod("magic_team")
public class MagicTeam {
    public static final String MODID = "magic_team";
    private static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public MagicTeam() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MagicTeamConfig.SERVER_SPEC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(MagicTeamConfig::onConfigEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        LOGGER.info("Magic-Team loaded: fixed team rules enabled.");
    }

    private void onRegisterCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        MagicTeamCommands.register(event.getDispatcher());
    }

    /** By server start every addon class with an adapter has been loaded and transformed. */
    private void onServerStarted(ServerStartedEvent event) {
        OptionalAdapterDiagnostics.logSummary(modId -> ModList.get().isLoaded(modId));
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        int depth = MagicTeamEffectContext.getDepth();
        if (depth > 0 || MagicTeamEffectContext.getSuspendedDepth() > 0) {
            String context = MagicTeamEffectContext.describeCurrentContext();
            LOGGER.warn("Magic-Team detected a leaked effect context at server tick end; clearing it to prevent unrelated damage/effects from inheriting the stale scope. {}", context);
            MagicTeamEffectContext.clear();
            AdapterContextBridge.clear();
        }
    }
}
