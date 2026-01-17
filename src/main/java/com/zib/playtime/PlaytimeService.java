package com.zib.playtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.zib.playtime.database.DatabaseManager;
import com.zib.playtime.listeners.SessionListener;

public class PlaytimeService {

    private final DatabaseManager db;
    private final boolean isMySQL;

    public PlaytimeService(DatabaseManager db) {
        this.db = db;
        this.isMySQL = db.isMySQL();
    }

    public void saveSession(String uuid, String name, long start, long duration) {
        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO playtime_sessions (uuid, username, start_time, duration, session_date) VALUES (?, ?, ?, ?, " + (isMySQL ? "CURDATE()" : "date('now')") + ")"
            );
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.setLong(3, start);
            ps.setLong(4, duration);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public long getTotalPlaytime(String uuid) {
        return getPlaytime(uuid, "all");
    }

    public long getPlaytime(String uuid, String type) {
        String dateFilter = getDateFilter(type);
        String query = "SELECT SUM(duration) FROM playtime_sessions WHERE uuid = ? " + dateFilter;

        long dbTime = 0;
        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) dbTime = rs.getLong(1);
        } catch (SQLException e) { e.printStackTrace(); }

        try {
            dbTime += SessionListener.getCurrentSession(UUID.fromString(uuid));
        } catch (Exception ignored) {}

        return dbTime;
    }

    public int getRank(String uuid, String type) {
        Map<String, Long> all = getTopPlayers(type, 1000);
        int rank = 1;
        long myTime = getPlaytime(uuid, type);

        for (Long time : all.values()) {
            if (time > myTime) {
                rank++;
            }
        }
        return rank;
    }

    public Map<String, Long> getTopPlayers(String type) {
        return getTopPlayers(type, 10);
    }

    public Map<String, Long> getTopPlayers(String type, int limit) {
        Map<String, Long> tempMap = new HashMap<>();

        String dateFilter = getDateFilter(type);
        String where = dateFilter.isEmpty() ? "" : "WHERE " + dateFilter.substring(4) + " ";

        String query = "SELECT uuid, username, SUM(duration) as total FROM playtime_sessions " +
                where +
                "GROUP BY uuid";

        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String name = rs.getString("username");
                long total = rs.getLong("total");

                try {
                    total += SessionListener.getCurrentSession(UUID.fromString(uuid));
                } catch (Exception ignored) {}

                tempMap.put(name, total);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return tempMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private String getDateFilter(String type) {
        if (isMySQL) {
            if (type.equalsIgnoreCase("daily")) {
                return "AND session_date = CURDATE() ";
            } else if (type.equalsIgnoreCase("weekly")) {
                return "AND session_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) ";
            } else if (type.equalsIgnoreCase("monthly")) {
                return "AND session_date >= DATE_SUB(CURDATE(), INTERVAL 1 MONTH) ";
            }
        } else {
            if (type.equalsIgnoreCase("daily")) {
                return "AND session_date = date('now') ";
            } else if (type.equalsIgnoreCase("weekly")) {
                return "AND session_date >= date('now', '-7 days') ";
            } else if (type.equalsIgnoreCase("monthly")) {
                return "AND session_date >= date('now', '-1 month') ";
            }
        }
        return "";
    }
}