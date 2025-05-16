package com.github.xomarnd.rps.server.service;

import java.util.Map;
import java.util.Set;

public class GameLogicService {

    private final Set<String> combinations;
    private final Map<String, Set<String>> beatsMap;

    public GameLogicService(Set<String> combinations, Map<String, Set<String>> beatsMap) {
        this.combinations = combinations;
        this.beatsMap = beatsMap;
    }

    public enum GameResultType {
        WIN, LOSE, DRAW, INVALID
    }

    public static class GameResult {
        public final GameResultType type;
        public final String winnerNick;
        public final String loserNick;
        public final String move1;
        public final String move2;
        public final String resultText;

        public GameResult(
                GameResultType type,
                String winnerNick,
                String loserNick,
                String move1,
                String move2,
                String resultText
        ) {
            this.type = type;
            this.winnerNick = winnerNick;
            this.loserNick = loserNick;
            this.move1 = move1;
            this.move2 = move2;
            this.resultText = resultText;
        }
    }

    public GameResult determineResult(
            String move1, String move2,
            String nick1, String nick2
    ) {
        if (move1 == null || move2 == null) {
            return new GameResult(
                    GameResultType.INVALID,
                    null, null,
                    move1, move2,
                    "Invalid move! Move cannot be null.\n"
            );
        }

        String m1 = move1.trim().toLowerCase();
        String m2 = move2.trim().toLowerCase();

        if (!combinations.contains(m1) || !combinations.contains(m2)) {
            return new GameResult(
                    GameResultType.INVALID,
                    null, null,
                    m1, m2,
                    "Invalid move! Allowed moves: " + combinations + "\n"
            );
        }

        if (m1.equals(m2)) {
            return new GameResult(
                    GameResultType.DRAW,
                    null, null,
                    m1, m2,
                    "Draw! Repeat round.\n"
            );
        }

        if (beatsMap.getOrDefault(m1, Set.of()).contains(m2)) {
            return new GameResult(
                    GameResultType.WIN,
                    nick1, nick2,
                    m1, m2,
                    nick1 + " wins! (" + m1 + " vs " + m2 + ")\n"
            );
        }

        if (beatsMap.getOrDefault(m2, Set.of()).contains(m1)) {
            return new GameResult(
                    GameResultType.LOSE,
                    nick2, nick1,
                    m2, m1,
                    nick2 + " wins! (" + m2 + " vs " + m1 + ")\n"
            );
        }

        return new GameResult(
                GameResultType.INVALID,
                null, null,
                m1, m2,
                "Invalid move combination!\n"
        );
    }

    public Set<String> getCombinations() {
        return combinations;
    }

    public Map<String, Set<String>> getBeatsMap() {
        return beatsMap;
    }
}
