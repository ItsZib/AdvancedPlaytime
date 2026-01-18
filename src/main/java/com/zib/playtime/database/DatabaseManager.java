package com.zib.playtime.database;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zib.playtime.Playtime;
import com.zib.playtime.config.PlaytimeConfig;
import com.zib.playtime.config.Reward;

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
        String sessionsSql;
        String rewardsSql; // NEW

        if (isMySQL) {
            sessionsSql = "CREATE TABLE IF NOT EXISTS playtime_sessions (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "uuid VARCHAR(36)," +
                    "username VARCHAR(16)," +
                    "start_time BIGINT," +
                    "duration BIGINT," +
                    "session_date DATE" +
                    ")";

            // NEW: Table to log claimed rewards
            rewardsSql = "CREATE TABLE IF NOT EXISTS playtime_rewards_log (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "uuid VARCHAR(36)," +
                    "reward_id VARCHAR(64)," +
                    "claim_date DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        } else {
            sessionsSql = "CREATE TABLE IF NOT EXISTS playtime_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid VARCHAR(36)," +
                    "username VARCHAR(16)," +
                    "start_time BIGINT," +
                    "duration BIGINT," +
                    "session_date DATE DEFAULT CURRENT_DATE" +
                    ")";

            rewardsSql = "CREATE TABLE IF NOT EXISTS playtime_rewards_log (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid VARCHAR(36)," +
                    "reward_id VARCHAR(64)," +
                    "claim_date DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        }

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sessionsSql);
            stmt.execute(rewardsSql);
            logger.info("Successfully created/verified database tables.");
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

    // NEW: Check if a reward is already claimed for the current period
    public boolean hasClaimedReward(String uuid, Reward reward) {
        String timeClause = "";

        // Determine SQL logic for periods
        if (isMySQL) {
            if (reward.period.equalsIgnoreCase("daily")) {
                timeClause = " AND DATE(claim_date) = CURDATE()";
            } else if (reward.period.equalsIgnoreCase("weekly")) {
                timeClause = " AND claim_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
            } else if (reward.period.equalsIgnoreCase("monthly")) {
                timeClause = " AND claim_date >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
            }
        } else {
            // SQLite
            if (reward.period.equalsIgnoreCase("daily")) {
                timeClause = " AND date(claim_date) = date('now')";
            } else if (reward.period.equalsIgnoreCase("weekly")) {
                timeClause = " AND date(claim_date) >= date('now', '-7 days')";
            } else if (reward.period.equalsIgnoreCase("monthly")) {
                timeClause = " AND date(claim_date) >= date('now', '-1 month')";
            }
        }

        String query = "SELECT id FROM playtime_rewards_log WHERE uuid = ? AND reward_id = ?" + timeClause;

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, reward.id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error checking reward claim", e);
            return true; // Fail safe: assume claimed to prevent exploit on error
        }
    }

    // NEW: Log a claim
    public void logRewardClaim(String uuid, String rewardId) {
        String sql = "INSERT INTO playtime_rewards_log (uuid, reward_id) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, rewardId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error logging reward claim", e);
        }
    }
}