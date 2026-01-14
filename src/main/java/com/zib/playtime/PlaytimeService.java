package com.zib.playtime;

import com.zib.playtime.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PlaytimeService {

    private final DatabaseManager db;

    public PlaytimeService(DatabaseManager db) {
        this.db = db;
    }

    public void saveSession(String uuid, String name, long start, long duration) {
        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO playtime_sessions (uuid, username, start_time, duration) VALUES (?, ?, ?, ?)"
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
        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT SUM(duration) FROM playtime_sessions WHERE uuid = ?");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }


    public Map<String, Long> getTopPlayers(String type) {
        Map<String, Long> top = new LinkedHashMap<>();

        String dateFilter = "";
        if (type.equalsIgnoreCase("daily")) {
            dateFilter = "WHERE session_date = date('now') ";
        } else if (type.equalsIgnoreCase("weekly")) {
            dateFilter = "WHERE session_date >= date('now', '-7 days') ";
        } else if (type.equalsIgnoreCase("monthly")) {
            dateFilter = "WHERE session_date >= date('now', '-1 month') ";
        }

        String query = "SELECT username, SUM(duration) as total FROM playtime_sessions " +
                dateFilter +
                "GROUP BY username ORDER BY total DESC LIMIT 10";

        try (Connection conn = db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                top.put(rs.getString("username"), rs.getLong("total"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return top;
    }
}