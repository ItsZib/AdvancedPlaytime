package com.zib.playtime.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlaytimeConfig {

    public DatabaseSettings database = new DatabaseSettings();
    public CommandSettings command = new CommandSettings();
    public PeriodSettings periods = new PeriodSettings();
    public MessageSettings messages = new MessageSettings();
    public GuiSettings gui = new GuiSettings();
    public List<Reward> rewards = new ArrayList<>();

    public void setDefaults() {
        if (database == null) database = new DatabaseSettings();
        if (command == null) command = new CommandSettings();
        if (periods == null) periods = new PeriodSettings();
        if (messages == null) messages = new MessageSettings();
        if (gui == null) gui = new GuiSettings();
        if (rewards == null) rewards = new ArrayList<>();

        if (command.topStyle == null) command.topStyle = "text";
        if (command.aliases == null) command.aliases = Arrays.asList("pt", "play", "time");
    }

    public static class DatabaseSettings {
        public String type = "sqlite";
        public String host = "localhost";
        public int port = 3306;
        public String databaseName = "playtime_db";
        public String username = "root";
        public String password = "password";
        public boolean useSSL = false;
    }

    public static class CommandSettings {
        public String name = "playtime";
        public String description = "Check your playtime stats";
        public List<String> aliases = Arrays.asList("pt", "play", "time");
        public String topStyle = "text";
    }

    public static class PeriodSettings {
        public String daily = "daily";
        public String weekly = "weekly";
        public String monthly = "monthly";
        public String all = "all";
        public String reload = "reload";
    }

    public static class MessageSettings {
        public String selfCheck = "&dTotal Playtime: &e%time%";
        public String otherCheck = "&d%player%'s Playtime: &e%time%";

        public String leaderboardHeader = "&6--- Playtime Leaderboard (&e%period_name%&6) ---";
        public String leaderboardEntry = "&6#%rank% &e%player% &7: &f%time%";
        public String leaderboardEmpty = "&7No data available yet.";

        public String reloadSuccess = "&aConfiguration reloaded successfully!";
        public String reloadNoPermission = "&cYou do not have permission to reload.";
        public String reloadFailed = "&cFailed to reload config. Check console.";

        public String errorInvalidPeriod = "&cInvalid period. Use: %valid_periods%";
        public String errorConsole = "&cPlayers only.";
        public String noPermission = "&cYou do not have permission to use this command.";

        public String rewardAdded = "&aReward '%id%' added successfully!";
        public String rewardRemoved = "&aReward '%id%' removed successfully!";
        public String rewardNotFound = "&cReward '%id%' not found.";
        public String rewardBroadcast = "&6%player% &ehas played for &6%time% &eand claimed the &6%reward% &ereward!";

        // NEW: List Messages
        public String rewardListHeader = "&6--- Server Rewards ---";
        public String rewardListEntry = "&e%id% &7(%period%): &f%status%";
        public String statusClaimed = "&a[CLAIMED]";
        public String statusAvailable = "&e[AVAILABLE]";
        public String statusLocked = "&c[LOCKED]";
    }

    public static class GuiSettings {
        public String title = "LEADERBOARD";
        public String buttonAll = "ALL TIME";
        public String buttonDaily = "DAILY";
        public String buttonWeekly = "WEEKLY";
        public String buttonMonthly = "MONTHLY";
        public String footerTitle = "YOUR STATS:";
        public String rankPrefix = "Rank: #";
        public String timePrefix = "Time: ";
    }
}