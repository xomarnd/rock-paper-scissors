package com.github.xomarnd.rps.server.service;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class NicknameService {
    private static final Set<String> usedNicks = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 20;
    private static final String REGEX = "^[a-zA-Z0-9_]{3,20}$";

    public boolean reserveNick(String nick) {
        if (!isValid(nick)) return false;
        return usedNicks.add(nick.trim().toLowerCase());
    }

    public static void releaseNick(String nick) {
        if (nick != null) usedNicks.remove(nick.trim().toLowerCase());
    }

    public boolean isNickInUse(String nick) {
        if (nick == null) return false;
        return usedNicks.contains(nick.trim().toLowerCase());
    }

    public boolean isValid(String nick) {
        if (nick == null) return false;
        String n = nick.trim();
        if (n.length() < MIN_LENGTH || n.length() > MAX_LENGTH) return false;
        return n.matches(REGEX);
    }
}
