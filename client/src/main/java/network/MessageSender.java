package network;

import watcher.Operation;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class MessageSender {

    public void send(Operation operation, SessionHandler session) {
        if (operation.getEntity() == Operation.Entity.FILE && (operation.getType() == Operation.Type.CREATE || operation.getType() == Operation.Type.MODIFY)) {
            sendMessageWithFile(operation, session);
        } else {
            sendMessage(operation, session);
        }
    }

    private void sendMessageWithFile(Operation operation, SessionHandler session) {
        String filename = operation.getOldEntityPath();
        byte[] buffer = new byte[1024 * 1024 * 5];

        operation.setOldEntityPath(excludeRootPath(filename, session.getUsername()));
        sendMessage(operation, session);

        Operation sendFile = Operation.writingFile(operation);
        try {
            @SuppressWarnings("resource")
            FileChannel channel = new RandomAccessFile(filename, "rw").getChannel();
            FileLock lock = null;
            try {
                lock = channel.lock();
                while (true) {
                    Message m = new Message();
                    m.setOperation(sendFile);
                    m.setPosition(channel.position());
                    int read = channel.read(ByteBuffer.wrap(buffer));
                    if (read == -1) break;
                    if (read < buffer.length - 1) {
                        byte[] tempBuffer = new byte[read];
                        System.arraycopy(buffer, 0, tempBuffer, 0, read);
                        m.setFile(tempBuffer);
                        session.getChannel().writeAndFlush(m);
                        break;
                    } else {
                        m.setFile(buffer);
                        session.getChannel().writeAndFlush(m);
                    }
                    buffer = new byte[1024 * 1024 * 5];
                }
            } catch (OverlappingFileLockException e){
                e.printStackTrace();
            }
            if (lock != null) lock.release();
            channel.close();
        } catch (Exception e) {
            //do nothing
        }
    }

    private void sendMessage(Operation operation, SessionHandler session) {
        Message m = new Message();
        m.setOperation(operation);
        operation.setOldEntityPath(excludeRootPath(operation.getOldEntityPath(), session.getUsername()));
        operation.setNewEntityPath(excludeRootPath(operation.getNewEntityPath(), session.getUsername()));
        session.getChannel().writeAndFlush(m);
    }

    /**
     * Обрезает корневую директорию у переданного пути и добавляет имя пользователя в начало пути. Для удобства работы с путями на стороне сервера.
     *
     * @param path путь до сущности
     * @return путь без корневой директории
     */
    private String excludeRootPath(String path, String username) {
        return path == null ? null : path.replace(Operation.getWatcherRootPath().toString(), username.toLowerCase() + "\\");
    }
}
