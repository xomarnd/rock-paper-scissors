package com.github.xomarnd.rps.server;

import com.github.xomarnd.rps.server.service.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PlayerConnectionTest {

    private NicknameService nicknameService;
    private MatchmakingService matchmakingService;
    private GameSessionService gameSessionService;
    private PlayerSessionService playerSessionService;
    private ServerConfig config;

    @BeforeEach
    void setup() {
        config = makeTestConfig();
        nicknameService = new NicknameService();
        playerSessionService = new PlayerSessionService();
        gameSessionService = new GameSessionService(
                new GameLogicService(
                        config.getCombinations(),
                        config.getBeatsMap()
                ),
                config.getCombinations()
        );
        matchmakingService = new MatchmakingService(gameSessionService);
        // Освобождаем ник через сервис
        NicknameService.releaseNick("busyNick");
    }

    @AfterEach
    void after() {
        NicknameService.releaseNick("busyNick");
    }

    @Test
    void testBusyNickRejected() {
        // Регистрируем ник через сервис
        nicknameService.reserveNick("busyNick");

        EmbeddedChannel ch = new EmbeddedChannel(
                new LineBasedFrameDecoder(256),
                new StringDecoder(StandardCharsets.UTF_8),
                new StringEncoder(StandardCharsets.UTF_8),
                new PlayerNettyHandler(config, nicknameService, matchmakingService, gameSessionService, playerSessionService)
        );

        drainAllOutbound(ch);

        for (int i = 0; i < 3; i++) {
            ch.writeInbound("busyNick\n");
            String msg = drainAllOutbound(ch);
            if (i < 2) {
                assertTrue(msg.contains("Nickname already in use"), msg);
            } else {
                assertTrue(msg.contains("Too many invalid attempts"), msg);
            }
        }
        assertFalse(ch.isActive(), "Channel should be closed after 3 failed attempts");
    }

    private static ServerConfig makeTestConfig() {
        Properties props = new Properties();
        props.setProperty("server.port", "5050");
        props.setProperty("game.combinations", "rock,paper,scissors");
        props.setProperty("game.beats", "rock:scissors;paper:rock;scissors:paper");
        props.setProperty("log.level", "INFO");
        return new ServerConfig(props);
    }

    static String drainAllOutbound(EmbeddedChannel ch) {
        StringBuilder sb = new StringBuilder();
        Object msg;
        while ((msg = ch.readOutbound()) != null) {
            if (msg instanceof String) {
                sb.append(msg);
            } else if (msg instanceof io.netty.buffer.ByteBuf) {
                io.netty.buffer.ByteBuf buf = (io.netty.buffer.ByteBuf) msg;
                sb.append(buf.toString(java.nio.charset.StandardCharsets.UTF_8));
                buf.release();
            } else {
                sb.append(msg.toString());
            }
        }
        return sb.toString();
    }

}
