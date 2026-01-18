package com.zib.playtime;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.zib.playtime.config.ConfigManager;
import com.zib.playtime.config.PlaytimeConfig;
import com.zib.playtime.database.DatabaseManager;
import com.zib.playtime.commands.PlaytimeCommand;
import com.zib.playtime.listeners.SessionListener;
import com.zib.playtime.rewards.RewardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class Playtime extends JavaPlugin {

    private static Playtime INSTANCE;
    private static final Logger logger = LoggerFactory.getLogger("Playtime");

    private ConfigManager configManager;
    private DatabaseManager db;
    private PlaytimeService service;
    private RewardManager rewardManager; // NEW

    public Playtime(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    public static Playtime get() { return INSTANCE; }

    @Override
    protected void setup() {
        File dataFolder = new File("mods/Playtime");
        if (!dataFolder.exists()) dataFolder.mkdirs();

        configManager = new ConfigManager(dataFolder);
        configManager.init();
        PlaytimeConfig cfg = configManager.getConfig();

        db = new DatabaseManager(dataFolder);
        db.init();
        service = new PlaytimeService(db);

        // NEW: Init Reward Manager
        rewardManager = new RewardManager(db);

        String cmdName = cfg.command.name;
        String[] aliases = cfg.command.aliases.toArray(new String[0]);
        this.getCommandRegistry().registerCommand(new PlaytimeCommand(cmdName, aliases));

        this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, SessionListener::onJoin);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, SessionListener::onQuit);

        // NEW: Schedule Reward Checker (Runs every 1 minute)
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                rewardManager.checkRewards();
            } catch (Exception e) {
                logger.error("Error in reward checker task", e);
            }
        }, 1, 1, TimeUnit.MINUTES);

        logger.info("Playtime loaded successfully. Main command: /" + cmdName);
    }

    @Override
    protected void shutdown() {
        SessionListener.saveAllSessions();
        if (db != null) db.close();
    }

    public PlaytimeService getService() { return service; }
    public ConfigManager getConfigManager() { return configManager; }
    public RewardManager getRewardManager() { return rewardManager; }
}