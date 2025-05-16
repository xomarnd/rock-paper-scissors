package com.github.xomarnd.rps.server;

import com.github.xomarnd.rps.server.enums.PlayerState;
import com.github.xomarnd.rps.server.service.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

public class PlayerNettyHandler extends SimpleChannelInboundHandler<String> implements PlayerSession {
    private static final Logger log = LoggerFactory.getLogger(PlayerNettyHandler.class);

    private final ServerConfig config;
    private final Set<String> allowedMoves;
    private final NicknameService nicknameService;
    private final MatchmakingService matchmakingService;
    private final GameSessionService gameSessionService;
    private final PlayerSessionService playerSessionService;

    private volatile PlayerState playerState = PlayerState.NICK;
    private String nickname;
    private ChannelHandlerContext ctx;
    private int attempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private final UUID sessionId = UUID.randomUUID();

    public PlayerNettyHandler(
            ServerConfig config,
            NicknameService nicknameService,
            MatchmakingService matchmakingService,
            GameSessionService gameSessionService,
            PlayerSessionService playerSessionService
    ) {
        this.config = config;
        this.nicknameService = nicknameService;
        this.matchmakingService = matchmakingService;
        this.gameSessionService = gameSessionService;
        this.playerSessionService = playerSessionService;
        this.allowedMoves = Set.copyOf(config.getCombinations());
        log.debug("PlayerNettyHandler создан (hashCode={}), sessionId={}", System.identityHashCode(this), sessionId);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        log.info("New client connected: {} (hashCode={}, sessionId={})", ctx.channel().remoteAddress(), System.identityHashCode(this), sessionId);
        sendMessage("Welcome to Rock-Paper-Scissors!");
        sendMessage("Allowed moves: " + allowedMoves);
        sendMessage("Enter your nickname: ");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        log.debug("[{}] (hashCode={}) State: {} | Received: '{}'", nickname, System.identityHashCode(this), playerState, msg.replace("\n", "\\n"));
        msg = msg.trim();

        switch (playerState) {
            case NICK:
                handleNick(msg);
                break;
            case GAME:
                if (!msg.isEmpty()) {
                    if (!allowedMoves.contains(msg)) {
                        sendMessage("Invalid move! Allowed: " + allowedMoves);
                        sendMessage("Your move (" + nickname + "): ");
                        break;
                    }
                    gameSessionService.applyMove(this, msg);
                }
                break;
            default:
                log.warn("[{}] (hashCode={}) Неожиданное состояние: {}", nickname, System.identityHashCode(this), playerState);
                break;
        }
    }

    private void handleNick(String msg) {
        log.debug("[{}] (hashCode={}) handleNick, input='{}'", nickname, System.identityHashCode(this), msg);
        if (!nicknameService.isValid(msg)) {
            attempts++;
            sendMessage("Invalid nickname. Try again.\nEnter your nickname: ");
            log.info("Invalid nickname from client: '{}'", msg);
            if (attempts >= MAX_ATTEMPTS) {
                sendMessage("Too many invalid attempts. Connection closed.");
                closeConnection();
            }
            return;
        }
        if (!nicknameService.reserveNick(msg)) {
            attempts++;
            sendMessage("Nickname already in use. Try another.\nEnter your nickname: ");
            log.info("Busy nickname from client: '{}'", msg);
            if (attempts >= MAX_ATTEMPTS) {
                sendMessage("Too many invalid attempts. Connection closed.");
                closeConnection();
            }
            return;
        }
        this.nickname = msg;
        playerSessionService.registerSession(this);
        sendMessage("Hi, " + nickname + "! Waiting for opponent...");
        log.info("Nickname accepted: {} (hashCode={}, sessionId={})", nickname, System.identityHashCode(this), sessionId);
        matchmakingService.enqueue(this);
    }


    @Override
    public void setState(PlayerState playerState) {
        log.debug("[{}] (hashCode={}) setState: {}", nickname, System.identityHashCode(this), playerState);
        this.playerState = playerState;
    }


    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public void sendMessage(String msg) {
        if (ctx != null && ctx.channel().isActive()) {
            ctx.writeAndFlush(msg.endsWith("\n") ? msg : msg + "\n");
        } else {
            log.warn("Cannot send message, channel is inactive");
        }
    }

    public void closeConnection() {
        try {
            if (nickname != null) {
                nicknameService.releaseNick(nickname);
                playerSessionService.unregisterSession(this);
                log.info("Nick released: {}", nickname);
            }
            if (ctx != null && ctx.channel().isActive()) {
                ctx.writeAndFlush("Connection closed.\n").addListener(future -> ctx.close());
            } else if (ctx != null) {
                ctx.close();
            }
        } catch (Exception e) {
            log.warn("Error during closeConnection: ", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        matchmakingService.removeFromQueue(this);
        gameSessionService.closeSessionByPlayer(this);
        closeConnection();
        log.info("Channel inactive: {} (hashCode={}, sessionId={})", ctx.channel().remoteAddress(), System.identityHashCode(this), sessionId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception: ", cause);
        closeConnection();
    }
}
