package handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import network.Message;
import server.Server;
import operations.FileOperation;

import java.io.RandomAccessFile;

public class FileWriteHandler extends SimpleChannelInboundHandler<Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        try (RandomAccessFile accessFile = new RandomAccessFile(Server.getWorkDirectory() + "\\" + ((FileOperation) msg.getOperation()).getOldEntityPath(), "rw")) {
            accessFile.seek(msg.getPosition());
            accessFile.write(msg.getFile());
        }
        ctx.pipeline().remove(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
