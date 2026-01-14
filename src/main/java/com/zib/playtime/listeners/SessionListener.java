package com.zib.playtime.listeners;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.zib.playtime.Playtime;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionListener {


    private static final ConcurrentHashMap<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Long> historicalCache = new ConcurrentHashMap<>();

    public static void onJoin(PlayerConnectEvent event) {
        UUID uuid = event.getPlayerRef().getUuid();
        long now = System.currentTimeMillis();

        joinTimes.put(uuid, now);

        new Thread(() -> {
            long dbTime = Playtime.get().getService().getTotalPlaytime(uuid.toString());
            historicalCache.put(uuid, dbTime);
        }).start();
    }

    public static void onQuit(PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayerRef().getUuid();

        if (joinTimes.containsKey(uuid)) {
            long start = joinTimes.get(uuid);
            long duration = System.currentTimeMillis() - start;

            new Thread(() -> {
                Playtime.get().getService().saveSession(
                        uuid.toString(),
                        event.getPlayerRef().getUsername(),
                        start,
                        duration
                );
            }).start();

            joinTimes.remove(uuid);
            historicalCache.remove(uuid);
        }
    }

    public static long getCurrentSession(UUID uuid) {
        if (!joinTimes.containsKey(uuid)) return 0;
        return System.currentTimeMillis() - joinTimes.get(uuid);
    }

    public static long getLiveTotalTime(UUID uuid) {
        long history = historicalCache.getOrDefault(uuid, 0L);
        long current = getCurrentSession(uuid);
        return history + current;
    }
}