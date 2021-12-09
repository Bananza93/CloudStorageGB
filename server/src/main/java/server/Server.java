package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import handlers.AuthenticateHandler;
import network.JSONDecoder;
import network.JSONEncoder;
import handlers.SessionHandler;

import java.nio.file.Path;
import java.util.*;

public class Server {
    private static final String WORK_DIRECTORY = System.getProperty("user.home") + "\\CloudStorageGBProject\\ServerDir";
    private static final Map<String, SessionHandler> sessions = new HashMap<>();


    public void start() throws InterruptedException {
        startUtilizeTask();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(
                                            1024 * 1024 * 150,
                                            0,
                                            8,
                                            0,
                                            8
                                    ),
                                    new LengthFieldPrepender(8),
                                    new ByteArrayDecoder(),
                                    new ByteArrayEncoder(),
                                    new JSONDecoder(),
                                    new JSONEncoder(),
                                    new AuthenticateHandler()
                            );
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = serverBootstrap.bind(11111).sync();
            System.out.println("Server started");

            channelFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static String getWorkDirectory() {
        return WORK_DIRECTORY;
    }

    public static SessionHandler getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public static Path getUserDirectory(String sessionId) {
        return getSession(sessionId).getUserDirectory();
    }

    public static Path getUserRecycleBinDirectory(String sessionId) {
        return getSession(sessionId).getUserRecycleBinDirectory();
    }

    public static void addSession(String sessionId, SessionHandler session) {
        sessions.put(sessionId, session);
    }

    private void startUtilizeTask() {
        Timer utilizeTimer = new Timer(true);
        utilizeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                utilizeDeadSessions();
            }
        }, 60000);
    }

    private void utilizeDeadSessions() {
        sessions.entrySet().removeIf(entry -> !entry.getValue().getChannel().isActive());
    }
}
