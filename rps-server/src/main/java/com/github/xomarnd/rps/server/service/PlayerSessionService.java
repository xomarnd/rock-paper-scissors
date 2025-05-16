package com.github.xomarnd.rps.server.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSessionService {

    private final Map<UUID, PlayerSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, PlayerSession> sessionsByNick = new ConcurrentHashMap<>();

    public void registerSession(PlayerSession session) {
        UUID id = session.getSessionId();
        String nickname = session.getNickname();
        sessionsById.put(id, session);
        sessionsByNick.put(nickname, session);
    }

    public void unregisterSession(PlayerSession session) {
        UUID id = session.getSessionId();
        String nickname = session.getNickname();
        sessionsById.remove(id);
        sessionsByNick.remove(nickname);
    }

    public PlayerSession getSessionById(UUID id) {
        return sessionsById.get(id);
    }

    public PlayerSession getSessionByNickname(String nickname) {
        return sessionsByNick.get(nickname);
    }

    public boolean existsById(UUID id) {
        return sessionsById.containsKey(id);
    }

    public boolean existsByNickname(String nickname) {
        return sessionsByNick.containsKey(nickname);
    }

    public int getSessionCount() {
        return sessionsById.size();
    }
}
