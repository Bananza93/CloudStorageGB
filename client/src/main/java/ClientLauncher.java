import snapshot.FileTreeSnapshot;
import watcher.DirectoryWatcher;

import java.io.*;
import java.nio.file.Path;

public class ClientLauncher {
    public static void main(String[] args) throws IOException {
        new Client("bananza").start();
    }
}
