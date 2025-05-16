package com.github.xomarnd.rps.server.service;

import com.github.xomarnd.rps.server.enums.PlayerState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class GameSessionService {
    public class GameSession {
        public enum SessionState { WAIT_OPPONENT, GAME, FINISHED }

        private final UUID sessionId;
        private final PlayerSession player1;
        private final PlayerSession player2;
        private final GameLogicService logicService;
        private final Set<String> allowedMoves;
        private String move1, move2;
        private boolean p1Moved, p2Moved, finished;
        private SessionState sessionState = SessionState.WAIT_OPPONENT;
        private String pendingMove1 = null;
        private String pendingMove2 = null;

        public GameSession(PlayerSession p1, PlayerSession p2, GameLogicService logicService, Set<String> allowedMoves) {
            this.sessionId = UUID.randomUUID();
            this.player1 = p1;
            this.player2 = p2;
            this.logicService = logicService;
            this.allowedMoves = allowedMoves;
        }

        public UUID getSessionId() { return sessionId; }
        public boolean isPlayer(PlayerSession ps) { return player1.equals(ps) || player2.equals(ps); }
        public PlayerSession getOpponent(PlayerSession ps) {
            if (player1.equals(ps)) return player2;
            if (player2.equals(ps)) return player1;
            return null;
        }

        public synchronized void startGame() {
            if (sessionState == SessionState.GAME) return;
            sessionState = SessionState.GAME;
            player1.setState(PlayerState.GAME);
            player2.setState(PlayerState.GAME);
            player1.sendMessage("Opponent found! You play with: " + player2.getNickname());
            player2.sendMessage("Opponent found! You play with: " + player1.getNickname());
            if (pendingMove1 != null) { String move = pendingMove1; pendingMove1 = null; applyMove(player1, move); }
            if (pendingMove2 != null) { String move = pendingMove2; pendingMove2 = null; applyMove(player2, move); }
            promptMovesIfNeeded();
        }

        public synchronized void promptMovesIfNeeded() {
            if (!p1Moved) player1.sendMessage("Your move (" + player1.getNickname() + "): ");
            if (!p2Moved) player2.sendMessage("Your move (" + player2.getNickname() + "): ");
        }

        public synchronized void promptMoves() {
            move1 = move2 = null;
            p1Moved = p2Moved = false;
            finished = false;
            sessionState = SessionState.GAME;
            player1.sendMessage("Your move (" + player1.getNickname() + "): ");
            player2.sendMessage("Your move (" + player2.getNickname() + "): ");
        }

        public synchronized void applyMove(PlayerSession player, String move) {
            if (finished || sessionState == SessionState.FINISHED) {
                player.sendMessage("Game already finished.");
                return;
            }
            if (!allowedMoves.contains(move)) {
                player.sendMessage("Invalid move! Allowed: " + allowedMoves);
                player.sendMessage("Your move (" + player.getNickname() + "): ");
                return;
            }
            if (sessionState != SessionState.GAME) {
                if (player.equals(player1)) pendingMove1 = move;
                else if (player.equals(player2)) pendingMove2 = move;
                player.sendMessage("Opponent not ready yet, your move is saved.");
                return;
            }
            if (player.equals(player1)) {
                if (p1Moved) {
                    player.sendMessage("You've already made your move. Waiting for opponent...");
                    return;
                }
                move1 = move;
                p1Moved = true;
            } else if (player.equals(player2)) {
                if (p2Moved) {
                    player.sendMessage("You've already made your move. Waiting for opponent...");
                    return;
                }
                move2 = move;
                p2Moved = true;
            } else {
                player.sendMessage("You are not part of this game session.");
                return;
            }
            if (p1Moved && p2Moved) {
                GameLogicService.GameResult result = logicService.determineResult(move1, move2, player1.getNickname(), player2.getNickname());
                player1.sendMessage(result.resultText);
                player2.sendMessage(result.resultText);
                if (result.type == GameLogicService.GameResultType.DRAW) {
                    promptMoves();
                } else {
                    finished = true;
                    sessionState = SessionState.FINISHED;
                    player1.sendMessage("Game over.");
                    player2.sendMessage("Game over.");
                    player1.closeConnection();
                    player2.closeConnection();
                    GameSessionService.this.removeSession(this);
                }
            }
        }

        public synchronized void closeSession() {
            finished = true;
            sessionState = SessionState.FINISHED;
            player1.sendMessage("Game session closed.");
            player2.sendMessage("Game session closed.");
        }
    }

    private final Map<UUID, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<PlayerSession, GameSession> playerSessionMap = new ConcurrentHashMap<>();
    private final GameLogicService logicService;
    private final Set<String> allowedMoves;

    public GameSessionService(GameLogicService logicService, Set<String> allowedMoves) {
        this.logicService = logicService;
        this.allowedMoves = allowedMoves;
    }

    public GameSession startSession(PlayerSession p1, PlayerSession p2) {
        GameSession session = new GameSession(p1, p2, logicService, allowedMoves);
        sessions.put(session.getSessionId(), session);
        playerSessionMap.put(p1, session);
        playerSessionMap.put(p2, session);
        session.startGame();
        return session;
    }

    public void applyMove(PlayerSession player, String move) {
        GameSession session = playerSessionMap.get(player);
        if (session != null) {
            session.applyMove(player, move);
        } else {
            player.sendMessage("No active game session found.");
        }
    }

    public void closeSessionByPlayer(PlayerSession player) {
        GameSession session = playerSessionMap.get(player);
        if (session != null) {
            session.closeSession();
            removeSession(session);
        }
    }

    public void removeSession(GameSession session) {
        sessions.remove(session.getSessionId());
        playerSessionMap.remove(session.player1);
        playerSessionMap.remove(session.player2);
    }

    public GameSession getSessionByPlayer(PlayerSession player) {
        return playerSessionMap.get(player);
    }

    public GameSession getSessionById(UUID id) {
        return sessions.get(id);
    }
}
