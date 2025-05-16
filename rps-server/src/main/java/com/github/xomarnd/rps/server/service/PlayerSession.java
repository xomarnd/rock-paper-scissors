package com.github.xomarnd.rps.server.service;

import com.github.xomarnd.rps.server.enums.PlayerState;

import java.util.UUID;

public interface PlayerSession {
    UUID getSessionId();
    String getNickname();
    void sendMessage(String msg);
    void setState(PlayerState playerState);
    void closeConnection();
}