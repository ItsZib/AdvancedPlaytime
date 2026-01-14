package com.zib.playtime.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zib.playtime.Playtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;

public class ConfigManager {

    private final File configFile;
    private PlaytimeConfig config;
    private final Gson gson;
    private final Logger logger = LoggerFactory.getLogger("Playtime-Config");

    public ConfigManager(File dataFolder) {
        this.configFile = new File(dataFolder, "config.json");
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public void init() {
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        load();
    }

    private void saveDefaultConfig() {
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        try (InputStream in = Playtime.class.getResourceAsStream("/config.json")) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
                logger.info("Copied default config.json from JAR.");
            } else {
                config = new PlaytimeConfig();
                save();
                logger.warn("Could not find config.json in JAR, created blank default.");
            }
        } catch (IOException e) {
            logger.error("Failed to create config.json", e);
        }
    }

    public void load() {
        try (Reader reader = new FileReader(configFile)) {
            config = gson.fromJson(reader, PlaytimeConfig.class);
            logger.info("Configuration loaded!");
        } catch (IOException e) {
            logger.error("Failed to load config.json", e);
            config = new PlaytimeConfig();
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            logger.error("Failed to save config.json", e);
        }
    }

    public PlaytimeConfig getConfig() {
        if (config == null) load();
        return config;
    }
}