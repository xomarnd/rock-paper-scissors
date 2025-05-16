package com.github.xomarnd.rps.server.service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchmakingService {

    private final Queue<PlayerSession> queue = new ConcurrentLinkedQueue<>();
    private final GameSessionService gameSessionService;

    public MatchmakingService(GameSessionService gameSessionService) {
        this.gameSessionService = gameSessionService;
    }

    public void enqueue(PlayerSession player) {
        PlayerSession opponent = queue.poll();
        if (opponent != null) {
            gameSessionService.startSession(player, opponent);
        } else {
            queue.offer(player);
            player.sendMessage("Waiting for opponent...");
        }
    }

    public void removeFromQueue(PlayerSession player) {
        queue.remove(player);
    }

    public int getQueueSize() {
        return queue.size();
    }
}
