package com.zib.playtime.rewards;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.zib.playtime.Playtime;
import com.zib.playtime.api.PlaytimeAPI;
import com.zib.playtime.config.PlaytimeConfig;
import com.zib.playtime.config.Reward;
import com.zib.playtime.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RewardManager {

    private final DatabaseManager db;
    private final Logger logger = LoggerFactory.getLogger("Playtime-Rewards");

    public RewardManager(DatabaseManager db) {
        this.db = db;
    }

    @SuppressWarnings("deprecation")
    public void checkRewards() {
        List<PlayerRef> players = new ArrayList<>(Universe.get().getPlayers());
        for (PlayerRef player : players) {
            processPlayer(player);
        }
    }

    @SuppressWarnings("deprecation")
    private void processPlayer(PlayerRef player) {
        PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
        String uuid = player.getUuid().toString();

        for (Reward reward : config.rewards) {
            long playTime = PlaytimeAPI.get().getPlaytime(player.getUuid(), reward.period);

            if (playTime >= reward.timeRequirement) {
                if (!db.hasClaimedReward(uuid, reward)) {
                    giveReward(player, reward);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void giveReward(PlayerRef player, Reward reward) {
        // 1. Log claim to database
        db.logRewardClaim(player.getUuid().toString(), reward.id);

        final String username = player.getUsername();
        logger.info("Granting reward [" + reward.id + "] to " + username);

        World world = Universe.get().getWorld(player.getWorldUuid());
        if (world == null) {
            world = Universe.get().getDefaultWorld();
        }

        if (world != null) {
            world.execute(() -> {
                CommandManager cm = CommandManager.get();

                CommandSender consoleSender = new CommandSender() {
                    @Override public String getDisplayName() { return "Console"; }
                    @Override public UUID getUuid() { return new UUID(0, 0); }
                    @Override public void sendMessage(Message message) { logger.info("[ConsoleOutput]: " + message.toString()); }
                    @Override public boolean hasPermission(String p) { return true; }
                    @Override public boolean hasPermission(String p, boolean d) { return true; }
                };

                for (String cmd : reward.commands) {
                    String parsedCmd = cmd.replace("%player%", username).trim();

                    if (parsedCmd.startsWith("/")) {
                        parsedCmd = parsedCmd.substring(1);
                    }

                    if (parsedCmd.startsWith("\"") && parsedCmd.endsWith("\"")) {
                        parsedCmd = parsedCmd.substring(1, parsedCmd.length() - 1);
                    }

                    try {
                        logger.info("[RewardDebug] Executing: " + parsedCmd);
                        cm.handleCommand(consoleSender, parsedCmd);
                    } catch (Exception e) {
                        logger.error("Failed to execute command: " + parsedCmd, e);
                    }
                }
            });
        } else {
            logger.error("Could not find a valid world to execute reward commands for " + username);
        }

        // 3. Broadcast Message
        if (reward.broadcastMessage != null && !reward.broadcastMessage.isEmpty()) {
            String timeFormatted = PlaytimeAPI.get().formatTime(reward.timeRequirement);
            String msg = reward.broadcastMessage
                    .replace("%player%", username)
                    .replace("%time%", timeFormatted)
                    .replace("%reward%", reward.id);

            Universe.get().sendMessage(color(msg));
        }
    }

    public boolean isClaimed(String uuid, Reward reward) {
        return db.hasClaimedReward(uuid, reward);
    }

    private Message color(String text) {
        if (!text.contains("&")) return Message.raw(text);
        List<Message> messageParts = new ArrayList<>();
        String[] parts = text.split("(?=&[0-9a-fk-or])");
        for (String part : parts) {
            if (part.length() < 2 || part.charAt(0) != '&') {
                messageParts.add(Message.raw(part));
                continue;
            }
            char code = part.charAt(1);
            String content = part.substring(2);
            String hex = getHexFromCode(code);
            if (hex != null) messageParts.add(Message.raw(content).color(hex));
            else messageParts.add(Message.raw(content));
        }
        return Message.join(messageParts.toArray(new Message[0]));
    }

    private String getHexFromCode(char code) {
        switch (code) {
            case '0': return "#000000"; case '1': return "#0000AA"; case '2': return "#00AA00";
            case '3': return "#00AAAA"; case '4': return "#AA0000"; case '5': return "#AA00AA";
            case '6': return "#FFAA00"; case '7': return "#AAAAAA"; case '8': return "#555555";
            case '9': return "#5555FF"; case 'a': return "#55FF55"; case 'b': return "#55FFFF";
            case 'c': return "#FF5555"; case 'd': return "#FF55FF"; case 'e': return "#FFFF55";
            case 'f': return "#FFFFFF"; default: return null;
        }
    }
}