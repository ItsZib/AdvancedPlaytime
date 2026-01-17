package com.zib.playtime.database;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zib.playtime.Playtime;
import com.zib.playtime.config.PlaytimeConfig;

public class DatabaseManager {

    private HikariDataSource dataSource;
    private final File dataFolder;
    private boolean isMySQL;

    private final Logger logger = LoggerFactory.getLogger("Playtime-DB");

    public boolean isMySQL() {
        return isMySQL;
    }

    public DatabaseManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void init() {
        PlaytimeConfig.DatabaseSettings settings = Playtime.get().getConfigManager().getConfig().database;
        HikariConfig config = new HikariConfig();

        if (settings.type.equalsIgnoreCase("mysql")) {
            config.setJdbcUrl("jdbc:mysql://" + settings.host + ":" + settings.port + "/" + settings.databaseName + "?useSSL=" + settings.useSSL);
            config.setUsername(settings.username);
            config.setPassword(settings.password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            isMySQL = true;
            logger.info("Connecting to MySQL Database...");
        } else {
            if (!dataFolder.exists()) dataFolder.mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "playtime.db").getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            isMySQL = false;
            logger.info("Using local SQLite Database.");
        }

        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);

        createTable();
    }

    private void createTable() {
        String sql;
        if (isMySQL) {
            sql = "CREATE TABLE IF NOT EXISTS playtime_sessions (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "uuid VARCHAR(36)," +
                    "username VARCHAR(16)," +
                    "start_time BIGINT," +
                    "duration BIGINT," +
                    "session_date DATE" +
                    ")";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS playtime_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid VARCHAR(36)," +
                    "username VARCHAR(16)," +
                    "start_time BIGINT," +
                    "duration BIGINT," +
                    "session_date DATE DEFAULT CURRENT_DATE" +
                    ")";
        }

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Successfully created playtime_sessions table");
        } catch (SQLException e) {
            logger.error("Failed to create table: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create database table", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) dataSource.close();
    }
}