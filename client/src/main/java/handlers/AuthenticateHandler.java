package handlers;

import client.Client;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import network.Message;
import operations.AuthOperation;

public class AuthenticateHandler extends SimpleChannelInboundHandler<Message> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) {
        switch (message.getOperation().getType()) {
            case AUTH_SUCCESS -> {
                Client.getCurrentClientSession().setServerSideUserFilesList(((AuthOperation) message.getOperation()).getUserFilesList());
                Client.getCurrentClientSession().setAuthorized(true);
                System.out.println("Auth success");
                channelHandlerContext.pipeline().removeLast();
            }
            case AUTH_FAILED -> System.out.println(((AuthOperation) message.getOperation()).getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("Inside AuthenticateHandler: " + cause.getCause() + " | " + cause.getMessage());
    }
}
