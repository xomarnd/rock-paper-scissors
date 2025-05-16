package com.github.xomarnd.rps.server;

import com.github.xomarnd.rps.server.service.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static com.github.xomarnd.rps.server.PlayerConnectionTest.drainAllOutbound;
import static org.junit.jupiter.api.Assertions.*;

class GameSessionNettyIntegrationTest {

    private NicknameService nicknameService;
    private GameSessionService gameSessionService;
    private MatchmakingService matchmakingService;
    private PlayerSessionService playerSessionService;
    private ServerConfig config;

    @BeforeEach
    void setup() {
        NicknameService.releaseNick("Alice");
        NicknameService.releaseNick("Bob");
        config = makeTestConfig();

        nicknameService = new NicknameService();
        playerSessionService = new PlayerSessionService();
        // Передаём Set<String> — всё ок!
        gameSessionService = new GameSessionService(
                new GameLogicService(
                        config.getCombinations(),
                        config.getBeatsMap()
                ),
                config.getCombinations()
        );
        matchmakingService = new MatchmakingService(gameSessionService);
    }

    @AfterEach
    void cleanup() {
        NicknameService.releaseNick("Alice");
        NicknameService.releaseNick("Bob");
    }

    @Test
    void testFullGameSession_Player1Wins() throws Exception {
        EmbeddedChannel ch1 = new EmbeddedChannel(
                new LineBasedFrameDecoder(256),
                new StringDecoder(StandardCharsets.UTF_8),
                new StringEncoder(StandardCharsets.UTF_8),
                new PlayerNettyHandler(config, nicknameService, matchmakingService, gameSessionService, playerSessionService)
        );
        EmbeddedChannel ch2 = new EmbeddedChannel(
                new LineBasedFrameDecoder(256),
                new StringDecoder(StandardCharsets.UTF_8),
                new StringEncoder(StandardCharsets.UTF_8),
                new PlayerNettyHandler(config, nicknameService, matchmakingService, gameSessionService, playerSessionService)
        );

        ch1.pipeline().fireChannelActive();
        ch2.pipeline().fireChannelActive();

        assertTrue(waitOutboundContains(ch1, "Welcome", 500), "Alice did not receive Welcome after connect");
        assertTrue(waitOutboundContains(ch2, "Welcome", 500), "Bob did not receive Welcome after connect");

        drainAllOutbound(ch1);
        drainAllOutbound(ch2);


        ch1.writeInbound("Alice\n");
        ch2.writeInbound("Bob\n");

        assertTrue(waitOutboundContains(ch1, "Opponent found!", 500), "Alice did not see Opponent found!");
        assertTrue(waitOutboundContains(ch2, "Opponent found!", 500), "Bob did not see Opponent found!");

        ch1.writeInbound("rock\n");
        ch2.writeInbound("scissors\n");

        String result1 = waitOutboundLine(ch1, "wins", 500);
        String result2 = waitOutboundLine(ch2, "wins", 500);

        assertTrue(result1.contains("Alice wins") && result1.contains("rock vs scissors"), "Alice should win: " + result1);
        assertTrue(result2.contains("Alice wins") && result2.contains("rock vs scissors"), "Alice should win (Bob): " + result2);

        ch1.close();
        ch2.close();
    }

    private static ServerConfig makeTestConfig() {
        Properties p = new Properties();
        p.setProperty("server.port", "5050");
        p.setProperty("game.combinations", "rock,paper,scissors");
        p.setProperty("game.beats", "rock:scissors;paper:rock;scissors:paper");
        p.setProperty("log.level", "INFO");
        return new ServerConfig(p);
    }

    private static boolean waitOutboundContains(EmbeddedChannel ch, String expected, int maxWait) throws InterruptedException {
        long start = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        while (System.currentTimeMillis() - start < maxWait) {
            Object msg;
            while ((msg = ch.readOutbound()) != null) {
                String s;
                if (msg instanceof ByteBuf buf) {
                    s = buf.toString(StandardCharsets.UTF_8);
                    buf.release();
                } else {
                    s = msg.toString();
                }
                sb.append(s);
                if (s.contains(expected)) return true;
            }
            ch.runPendingTasks();
            ch.runScheduledPendingTasks();
            Thread.sleep(10);
        }
        return false;
    }

    private static String waitOutboundLine(EmbeddedChannel ch, String expected, int maxWait) throws InterruptedException {
        long start = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        while (System.currentTimeMillis() - start < maxWait) {
            Object msg;
            while ((msg = ch.readOutbound()) != null) {
                String s;
                if (msg instanceof ByteBuf buf) {
                    s = buf.toString(StandardCharsets.UTF_8);
                    buf.release();
                } else {
                    s = msg.toString();
                }
                sb.append(s);
                if (s.contains(expected)) return s;
            }
            ch.runPendingTasks();
            ch.runScheduledPendingTasks();
            Thread.sleep(10);
        }
        throw new AssertionError("Did not receive expected line with '" + expected + "', got: " + sb);
    }
}
