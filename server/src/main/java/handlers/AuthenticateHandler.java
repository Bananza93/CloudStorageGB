package handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import network.Message;
import server.Server;
import operations.AuthOperation;
import operations.OperationType;

public class AuthenticateHandler extends SimpleChannelInboundHandler<Message> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) {
        if (message.getOperation().getType() == OperationType.AUTH_REQUEST) {
            AuthOperation request = (AuthOperation) message.getOperation();
            AuthOperation response;
            if (Server.getSession(channelHandlerContext.channel().id().asShortText()) == null) {
                SessionHandler session = new SessionHandler(channelHandlerContext.channel(), request.getUsername());
                Server.addSession(channelHandlerContext.channel().id().asShortText(), session);
                response = AuthOperation.createAuthSuccess(request.getUsername(), session.getUserFiles().getFilesListForTransfer());
                channelHandlerContext.pipeline().removeLast();
                channelHandlerContext.pipeline().addLast(new FileHandler());
            } else {
                response = AuthOperation.createAuthFailed(request.getUsername(), "Already logged in");
            }
            Message m = new Message();
            m.setOperation(response);
            channelHandlerContext.channel().writeAndFlush(m);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("Inside AuthenticateHandler: " + cause.getCause() + " | " + cause.getMessage());
    }
}
