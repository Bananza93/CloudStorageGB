import network.SessionHandler;
import snapshot.FileTreeSnapshot;
import utils.ThreadPool;
import watcher.DirectoryWatcher;

import java.io.IOException;
import java.nio.file.Path;

public class Client {
    private static final String workDir = System.getProperty("user.home") + "\\CloudStorageGBProject\\ClientDir";
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 11111;
    private String username;

    public Client(String username) {
        this.username = username;
    }

    public void start() throws IOException {
        SessionHandler session = new SessionHandler(SERVER_HOST, SERVER_PORT, username);
        ThreadPool.addTask(session::connectToServer);
        FileTreeSnapshot fts = new FileTreeSnapshot(Path.of(workDir));
        DirectoryWatcher watcher = null;
        try {
            (watcher = DirectoryWatcher.init(fts, session)).start();
        } catch (IOException e) {
            //do nothing
        }
    }
}
