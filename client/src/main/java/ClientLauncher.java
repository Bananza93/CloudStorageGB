import snapshot.FileTreeSnapshot;
import watcher.DirectoryWatcher;

import java.io.*;
import java.nio.file.Path;

public class ClientLauncher {

    public static void main(String[] args) throws IOException {
        FileTreeSnapshot fts = new FileTreeSnapshot(Path.of(System.getProperty("user.home") + "\\CloudStorageGBProject\\ClientDir"));
        DirectoryWatcher watcher = null;
        try {
            (watcher = DirectoryWatcher.init(fts)).start();
        } catch (IOException e) {
            if (watcher != null) watcher.shutdown();
        }
    }
}
