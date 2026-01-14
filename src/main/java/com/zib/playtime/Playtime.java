package com.zib.playtime;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.zib.playtime.config.ConfigManager;
import com.zib.playtime.config.PlaytimeConfig;
import com.zib.playtime.database.DatabaseManager;
import com.zib.playtime.commands.PlaytimeCommand;
import com.zib.playtime.listeners.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;

public class Playtime extends JavaPlugin {

    private static Playtime INSTANCE;
    private static final Logger logger = LoggerFactory.getLogger("Playtime");

    private ConfigManager configManager;
    private DatabaseManager db;
    private PlaytimeService service;

    public Playtime(@Nonnull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    public static Playtime get() { return INSTANCE; }

    @Override
    protected void setup() {
        File dataFolder = new File("mods/Playtime");
        if (!dataFolder.exists()) dataFolder.mkdirs();

        // 1. Config
        configManager = new ConfigManager(dataFolder);
        configManager.init();
        PlaytimeConfig cfg = configManager.getConfig();

        // 2. Database
        db = new DatabaseManager(dataFolder);
        db.init();
        service = new PlaytimeService(db);

        // 3. Command Registration
        String cmdName = cfg.command.name;
        String[] aliases = cfg.command.aliases.toArray(new String[0]);
        this.getCommandRegistry().registerCommand(new PlaytimeCommand(cmdName, aliases));

        // 4. Events
        this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, SessionListener::onJoin);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, SessionListener::onQuit);

        logger.info("Playtime loaded successfully. Main command: /" + cmdName);
    }

    @Override
    protected void shutdown() {
        if (db != null) db.close();
    }

    public PlaytimeService getService() { return service; }
    public ConfigManager getConfigManager() { return configManager; }
}