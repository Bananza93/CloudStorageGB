package handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import network.Message;
import server.Server;
import operations.FileOperation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class FileHandler extends MessageToMessageDecoder<Message> {
    @Override
    protected void decode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
        if (msg.getOperation() instanceof FileOperation fo) {
            Path oldPath = Paths.get(Server.getWorkDirectory(), "\\", fo.getOldEntityPath());
            Path newPath = Paths.get(Server.getWorkDirectory(), "\\", fo.getNewEntityPath());

            if (fo.getEntity() == FileOperation.Entity.FILE) {
                switch (msg.getOperation().getType()) {
                    case CREATE, MODIFY -> checkFile(oldPath, true);
                    case FILE_WRITING -> {
                        checkFile(oldPath, false);
                        ctx.pipeline().addLast(new FileWriteHandler());
                        out.add(msg);
                    }
                    case DELETE -> {
                        if (Files.exists(oldPath)) {
                            Path toRecycle = Path.of(Server.getUserRecycleBinDirectory(ctx.channel().id().asShortText()) + addPrefixForDeletingFile(oldPath.getFileName()));
                            Files.move(oldPath, toRecycle);
                        }
                    }
                    case RENAME -> {
                        if (Files.exists(oldPath)) Files.move(oldPath, oldPath.resolveSibling(newPath.getFileName()));
                    }
                    case COPY -> {
                        if (Files.exists(oldPath)) Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    case MOVE_TO -> {
                        if (Files.exists(oldPath)) Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } else if (fo.getEntity() == FileOperation.Entity.DIRECTORY) {
                switch (msg.getOperation().getType()) {
                    case CREATE -> checkDirectory(oldPath);
                    case DELETE -> Files.deleteIfExists(oldPath);
                    case RENAME -> {
                        if (Files.exists(oldPath)) Files.move(oldPath, oldPath.resolveSibling(newPath.getFileName()));
                    }
                    case MOVE_TO -> {
                        if (Files.exists(oldPath)) Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    private void checkFile(Path path, boolean forceCreate) throws IOException {
        checkDirectory(path.getParent());
        if (forceCreate) {
            Files.deleteIfExists(path);
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    private void checkDirectory(Path path) throws IOException {
        Files.createDirectories(path);
    }

    private String addPrefixForDeletingFile(Path filename) {
        return "\\$" + System.currentTimeMillis() + "_" + filename.toString();
    }
}
