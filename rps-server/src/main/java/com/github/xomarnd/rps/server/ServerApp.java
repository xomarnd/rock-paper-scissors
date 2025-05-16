package com.github.xomarnd.rps.server;

import com.github.xomarnd.rps.server.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Set;

public class ServerApp {
    private static final Logger log = LoggerFactory.getLogger(ServerApp.class);

    public static void main(String[] args) {
        ServerConfig config = null;
        try {
            config = new ServerConfig();
        } catch (RuntimeException e) {
            log.error("Configuration error: {}", e.getMessage());
            System.exit(1);
        }
        int port = config.getPort();
        Set<String> allowedMoves = Set.copyOf(config.getCombinations());

        log.info("RPS Netty Server started! Listening on port {}", port);

        GameLogicService gameLogicService = new GameLogicService(
                allowedMoves,
                config.getBeatsMap()
        );
        NicknameService nicknameService = new NicknameService();
        PlayerSessionService playerSessionService = new PlayerSessionService();

        GameSessionService gameSessionService = new GameSessionService(gameLogicService, allowedMoves);
        MatchmakingService matchmakingService = new MatchmakingService(gameSessionService);

        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new RpsServerInitializer(
                            config,
                            nicknameService,
                            matchmakingService,
                            gameSessionService,
                            playerSessionService
                    ));

            ChannelFuture f = bootstrap.bind(port).sync();
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("Fatal error: {}", e.getMessage(), e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
