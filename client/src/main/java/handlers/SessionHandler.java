package handlers;

import files.SimpleFile;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import network.JSONDecoder;
import network.JSONEncoder;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SessionHandler {

    private final String serverHost;
    private final int serverPort;
    private final String username;
    private final Bootstrap client;
    private ChannelFuture channel;
    private boolean isProgramRunning;
    private Timer isSessionAliveTimer;
    private boolean isAuthorized;
    private List<SimpleFile> serverSideUserFilesList;

    public SessionHandler(String host, int port, String username) {
        this.serverHost = host;
        this.serverPort = port;
        this.username = username;
        this.client = settingClient();
        this.channel = null;
        this.isProgramRunning = true;
        this.isSessionAliveTimer = new Timer();
        this.isAuthorized = false;
        this.serverSideUserFilesList = null;
    }

    private Bootstrap settingClient() {
        Bootstrap client = new Bootstrap();
        client.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ch.pipeline().addLast(
                                new LengthFieldBasedFrameDecoder(
                                        1024 * 1024 * 200,
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
                });
        return client;
    }

    public void connectToServer() {
        isSessionAliveTimer.cancel();
        do {
            try {
                System.out.println("Trying to connect to the server...");
                channel = client.connect(serverHost, serverPort).sync();
                System.out.println("Connected to the server.");
                setIsSessionAliveTimer();
            } catch (Exception e) {
                System.out.println("Server not response.");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {/*do nothing*/}
            }
        } while (isProgramRunning && channel == null);

        try {
            channel.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            /*do nothing*/
        } finally {
            channel = null;
        }
    }

    private void setIsSessionAliveTimer() {
        this.isSessionAliveTimer = new Timer();
        isSessionAliveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isProgramRunning && !isAlive()) {
                    System.out.println("Connection lost.");
                    connectToServer();
                }
            }
        }, 1000);
    }

    public Channel getChannel() {
        return channel.channel();
    }

    public String getUsername() {
        return username;
    }

    public boolean isAlive() {
        return channel != null && getChannel().isActive();
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
    }

    public List<SimpleFile> getServerSideUserFilesList() {
        return serverSideUserFilesList;
    }

    public void setServerSideUserFilesList(List<SimpleFile> serverSideUserFilesList) {
        this.serverSideUserFilesList = serverSideUserFilesList;
    }

    public void shutdown() {
        this.isProgramRunning = false;
        client.config().group().shutdownGracefully();
    }
}

