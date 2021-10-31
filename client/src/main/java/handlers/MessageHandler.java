package handlers;

import client.Client;
import network.Message;
import operations.AuthOperation;
import operations.FileOperation;
import operations.OperationType;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class MessageHandler {

    public void send(FileOperation operation, SessionHandler session) {
        if (operation.getEntity() == FileOperation.Entity.FILE && (operation.getType() == OperationType.CREATE || operation.getType() == OperationType.MODIFY)) {
            sendMessageWithFile(operation, session);
        } else {
            sendMessage(operation, session);
        }
        System.out.println("SENT: " + operation);
    }

    private void sendMessageWithFile(FileOperation operation, SessionHandler session) {
        String filename = operation.getOldEntityPath();
        byte[] buffer = new byte[1024 * 1024 * 5];

        sendMessage(operation, session);

        FileOperation sendFile = FileOperation.writingFile(operation);
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

    private void sendMessage(FileOperation operation, SessionHandler session) {
        Message m = new Message();
        m.setOperation(operation);
        operation.setOldEntityPath(replaceWorkDirWithUsername(operation.getOldEntityPath(), session.getUsername()));
        operation.setNewEntityPath(replaceWorkDirWithUsername(operation.getNewEntityPath(), session.getUsername()));
        session.getChannel().writeAndFlush(m);
    }

    public void sendAuthRequest(SessionHandler session) {
        Message m = new Message();
        m.setOperation(AuthOperation.createAuthRequest(session.getUsername()));
        session.getChannel().writeAndFlush(m);
    }

    /**
     * Обрезает корневую директорию у переданного пути и добавляет имя пользователя в начало пути. Для удобства работы с путями на стороне сервера.
     *
     * @param path путь до сущности
     * @return путь без корневой директории
     */
    private String replaceWorkDirWithUsername(String path, String username) {
        if (path == null) return null;
        String cutted = path.replace(Client.getWorkDirPath(), "");
        return username + cutted;
    }
}
