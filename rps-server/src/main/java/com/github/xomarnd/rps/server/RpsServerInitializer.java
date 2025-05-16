package com.github.xomarnd.rps.server;

import com.github.xomarnd.rps.server.service.*;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import java.nio.charset.StandardCharsets;

public class RpsServerInitializer extends ChannelInitializer<SocketChannel> {
    private final ServerConfig config;
    private final NicknameService nicknameService;
    private final MatchmakingService matchmakingService;
    private final GameSessionService gameSessionService;
    private final PlayerSessionService playerSessionService;

    public RpsServerInitializer(
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
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new LineBasedFrameDecoder(256))
                .addLast(new StringDecoder(StandardCharsets.UTF_8))
                .addLast(new StringEncoder(StandardCharsets.UTF_8))
                .addLast(new PlayerNettyHandler(
                        config,
                        nicknameService,
                        matchmakingService,
                        gameSessionService,
                        playerSessionService
                ));
    }
}
