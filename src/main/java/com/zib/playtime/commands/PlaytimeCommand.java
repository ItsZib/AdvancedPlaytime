package com.zib.playtime.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.zib.playtime.Playtime;
import com.zib.playtime.config.PlaytimeConfig;
import com.zib.playtime.listeners.SessionListener;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PlaytimeCommand extends AbstractPlayerCommand {

    public PlaytimeCommand(String name, String... aliases) {
        super(name, Playtime.get().getConfigManager().getConfig().command.description);

        if (aliases != null && aliases.length > 0) {
            this.addAliases(aliases);
        }

        addUsageVariant(new ActionCommand(name));

        addUsageVariant(new TopPeriodCommand(name));
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                           PlayerRef player, World world) {
        long total = SessionListener.getLiveTotalTime(player.getUuid());
        String msg = Playtime.get().getConfigManager().getConfig().messages.selfCheck
                .replace("%time%", format(total))
                .replace("%player%", player.getUsername());
        ctx.sendMessage(color(msg));
    }

    private static Message color(String text) {
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

    private static String getHexFromCode(char code) {
        switch (code) {
            case '0': return "#000000"; case '1': return "#0000AA"; case '2': return "#00AA00";
            case '3': return "#00AAAA"; case '4': return "#AA0000"; case '5': return "#AA00AA";
            case '6': return "#FFAA00"; case '7': return "#AAAAAA"; case '8': return "#555555";
            case '9': return "#5555FF"; case 'a': return "#55FF55"; case 'b': return "#55FFFF";
            case 'c': return "#FF5555"; case 'd': return "#FF55FF"; case 'e': return "#FFFF55";
            case 'f': return "#FFFFFF"; default: return null;
        }
    }

    private static String format(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return hours + "h " + minutes + "m";
    }


    private static void showTop(CommandContext ctx, PlayerRef player, String inputArg) {
        PlaytimeConfig cfg = Playtime.get().getConfigManager().getConfig();
        PlaytimeConfig.PeriodSettings p = cfg.periods;

        String mode = null;
        String displayName = inputArg;

        if (inputArg.equalsIgnoreCase(p.daily)) mode = "daily";
        else if (inputArg.equalsIgnoreCase(p.weekly)) mode = "weekly";
        else if (inputArg.equalsIgnoreCase(p.monthly)) mode = "monthly";
        else if (inputArg.equalsIgnoreCase(p.all)) mode = "all";

        if (mode == null) {
            String valid = String.join(", ", p.daily, p.weekly, p.monthly, p.all);
            ctx.sendMessage(color(cfg.messages.errorInvalidPeriod.replace("%valid_periods%", valid)));
            return;
        }

        Map<String, Long> leaderboard = new HashMap<>(Playtime.get().getService().getTopPlayers(mode));

        if (mode.equals("all") || mode.equals("daily")) {
            leaderboard.put(player.getUsername(), SessionListener.getLiveTotalTime(player.getUuid()));
        }

        List<Map.Entry<String, Long>> sorted = leaderboard.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .collect(Collectors.toList());

        ctx.sendMessage(color(cfg.messages.leaderboardHeader.replace("%period_name%", displayName)));

        if (sorted.isEmpty()) {
            ctx.sendMessage(color(cfg.messages.leaderboardEmpty));
            return;
        }

        int rank = 1;
        for (Map.Entry<String, Long> entry : sorted) {
            String line = cfg.messages.leaderboardEntry
                    .replace("%rank%", String.valueOf(rank))
                    .replace("%player%", entry.getKey())
                    .replace("%time%", format(entry.getValue()));
            ctx.sendMessage(color(line));
            rank++;
        }
    }

    private static void doReload(CommandContext ctx) {
        PlaytimeConfig.MessageSettings msgs = Playtime.get().getConfigManager().getConfig().messages;

        if (!ctx.sender().hasPermission("playtime.reload")) {
            ctx.sendMessage(color(msgs.reloadNoPermission));
            return;
        }

        try {
            Playtime.get().getConfigManager().load();
            ctx.sendMessage(color(msgs.reloadSuccess));
        } catch (Exception e) {
            ctx.sendMessage(color(msgs.reloadFailed));
            e.printStackTrace();
        }
    }


    private static class ActionCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> actionArg;

        public ActionCommand(String name) {
            super(name);

            this.actionArg = withRequiredArg("action", "top|reload", ArgTypes.STRING);
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            String arg = ctx.get(actionArg);
            PlaytimeConfig.PeriodSettings periods = Playtime.get().getConfigManager().getConfig().periods;

            if (arg.equalsIgnoreCase(periods.reload)) {
                doReload(ctx);
                return;
            }


            if (arg.equalsIgnoreCase("top")) {
                showTop(ctx, player, periods.all);
                return;
            }


            if (arg.equalsIgnoreCase(periods.daily) || arg.equalsIgnoreCase(periods.weekly) ||
                    arg.equalsIgnoreCase(periods.monthly) || arg.equalsIgnoreCase(periods.all)) {
                showTop(ctx, player, arg);
                return;
            }

            ctx.sendMessage(color("&cUnknown command. Usage: /playtime top [period] OR /playtime reload"));
        }
    }

    private static class TopPeriodCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> actionArg;
        private final RequiredArg<String> periodArg;
        public TopPeriodCommand(String name) {
            super(name);
            this.actionArg = withRequiredArg("action", "top", ArgTypes.STRING);
            this.periodArg = withRequiredArg("period", "period", ArgTypes.STRING);
        }
        @Override protected boolean canGeneratePermission() { return false; }
        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            if (ctx.get(actionArg).equalsIgnoreCase("top")) {
                showTop(ctx, player, ctx.get(periodArg).toLowerCase());
            }
        }
    }
}