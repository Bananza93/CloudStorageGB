package client;

import handlers.MessageHandler;
import handlers.SessionHandler;
import files.FileTreeSnapshot;
import utils.ThreadPool;

import java.io.IOException;
import java.nio.file.Path;

public class Client {
    private static final String workDir = System.getProperty("user.home") + "\\CloudStorageGBProject\\ClientDir";
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 11111;
    private static String username;
    private static SessionHandler session;

    public Client(String username) {
        Client.username = username;
    }

    public void start() throws IOException {
        session = new SessionHandler(SERVER_HOST, SERVER_PORT, username);
        ThreadPool.addTask(session::connectToServer);
        FileTreeSnapshot fts = new FileTreeSnapshot(Path.of(workDir));
        while (!session.isAlive()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        new MessageHandler().sendAuthRequest(session);

        while (!session.isAuthorized() || FileTreeSnapshot.isComputing()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        SynchronizationService.synchronizeWithServer(fts, session.getServerSideUserFilesList());
//        DirectoryWatcher watcher = null;
//        try {
//            (watcher = DirectoryWatcher.init(fts, session)).start();
//        } catch (IOException e) {
//            //do nothing
//        }
    }

    public static SessionHandler getCurrentClientSession() {
        return session;
    }

    public static String getWorkDirPath() {
        return workDir;
    }
}
