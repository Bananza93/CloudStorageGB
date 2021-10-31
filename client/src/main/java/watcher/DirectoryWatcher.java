package watcher;

import com.sun.nio.file.ExtendedWatchEventModifier;
import network.*;
import snapshot.Directory;
import snapshot.File;
import snapshot.FileSystemElement;
import snapshot.FileTreeSnapshot;
import utils.CRC32Hash;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatcher {

    private static DirectoryWatcher currentWatcher;

    private final WatchService watcher;
    private final FileTreeSnapshot snapshot;
    private final SessionHandler session;
    private final Path rootDirectory;
    private List<Operation> operationsList;
    private final ReentrantLock lock;
    private Timer timer;

    private DirectoryWatcher(FileTreeSnapshot fts, SessionHandler session) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.snapshot = fts;
        this.session = session;
        this.rootDirectory = fts.getInitialPath();
        this.operationsList = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.timer = new Timer();
        this.rootDirectory.register(watcher,
                new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW},
                ExtendedWatchEventModifier.FILE_TREE);
        currentWatcher = this;
        Operation.setWatcherRootPath(rootDirectory);
    }

    public static DirectoryWatcher init(FileTreeSnapshot fts, SessionHandler session) throws IOException {
        if (currentWatcher != null)
            throw new RuntimeException("watcher.DirectoryWatcher already initialized");
        return new DirectoryWatcher(fts, session);
    }

    public void shutdown() throws IOException {
        this.timer.cancel();
        this.lock.unlock();
        this.watcher.close();
        Operation.setWatcherRootPath(null);
        currentWatcher = null;
    }

    public void start() throws IOException {
        WatchEvent<?> prevEvent = null;
        FileSystemElement prevElement = null;
        System.out.println("Watcher started.");

        while (true) {
            WatchKey key;

            try {
                while (FileTreeSnapshot.isComputing()) {
                    synchronized (this) {
                        this.wait(500);
                    }
                }
                key = watcher.take();
                lock.lock();
            } catch (InterruptedException e) {
                shutdown();
                return;
            }

            for (WatchEvent<?> currEvent : key.pollEvents()) {
                WatchEvent.Kind<?> kind = currEvent.kind();
                if (kind == OVERFLOW) continue;
                Path currElementPath = getChangedPath(key, currEvent);
                Directory currElementParentDir = snapshot.getDirectory(currElementPath.getParent());
                String currElementName = currElementPath.getFileName().toString();
                FileSystemElement currElement;

                if (kind == ENTRY_CREATE) {
                    if (Files.isDirectory(currElementPath)) {
                        if (prevEvent != null && prevEvent.kind() == ENTRY_DELETE && !operationsList.isEmpty() && prevElement instanceof Directory prevDir) {
                            if (isDirectoryRename(currElementPath, prevDir)) {
                                Path prevPath = prevDir.getPath();
                                prevDir.rename(currElementName);
                                currElement = currElementParentDir.addSubdirectory(prevDir);
                                addOperation(Operation.Type.RENAME, Operation.Entity.DIRECTORY, prevPath, prevDir.getPath());
                            } else if (isDirectoryMoved(currElementPath, prevDir)) {
                                Path prevPath = prevDir.getPath();
                                prevDir.moveTo(currElementParentDir);
                                currElement = prevDir;
                                addOperation(Operation.Type.MOVE_TO, Operation.Entity.DIRECTORY, prevPath, prevDir.getPath());
                            } else {
                                currElement = currElementParentDir.addSubdirectory(currElementName);
                                addOperation(Operation.Type.CREATE, Operation.Entity.DIRECTORY, currElementPath);
                            }
                        } else {
                            currElement = currElementParentDir.addSubdirectory(currElementName);
                            addOperation(Operation.Type.CREATE, Operation.Entity.DIRECTORY, currElementPath);
                        }
                    } else {
                        if (prevEvent != null && prevEvent.kind() == ENTRY_DELETE && !operationsList.isEmpty() && prevElement instanceof File prevFile) {
                            if (isFileRename(currElementPath, prevFile)) {
                                Path prevPath = prevFile.getPath();
                                prevFile.rename(currElementName);
                                currElement = currElementParentDir.addFile(prevFile);
                                addOperation(Operation.Type.RENAME, Operation.Entity.FILE, prevPath, prevFile.getPath());
                            } else if (isFileMoved(currElementPath, prevFile)) {
                                Path prevPath = prevFile.getPath();
                                prevFile.moveTo(currElementParentDir);
                                currElement = prevFile;
                                addOperation(Operation.Type.MOVE_TO, Operation.Entity.FILE, prevPath, prevFile.getPath());
                            } else {
                                currElement = currElementParentDir.addFile(currElementName);
                                addOperation(Operation.Type.CREATE, Operation.Entity.FILE, currElementPath);
                            }
                        } else {
                            currElement = currElementParentDir.addFile(currElementName);
                            addOperation(Operation.Type.CREATE, Operation.Entity.FILE, currElementPath);
                        }
                    }
                } else if (kind == ENTRY_DELETE) {
                    if (currElementParentDir.containsSubdirectory(currElementName)) {
                        currElement = currElementParentDir.removeSubdirectory(currElementName);
                        addOperation(Operation.Type.DELETE, Operation.Entity.DIRECTORY, currElementPath);
                    } else {
                        currElement = currElementParentDir.removeFile(currElementName);
                        addOperation(Operation.Type.DELETE, Operation.Entity.FILE, currElementPath);
                    }
                } else if (kind == ENTRY_MODIFY) {
                    if (Files.isDirectory(currElementPath)) {
                        currElementParentDir.getSubdirectory(currElementName).updateLastModified();
                        addOperation(Operation.Type.MODIFY, Operation.Entity.DIRECTORY, currElementPath);
                    } else {
                        currElementParentDir.getFile(currElementName).updateInfoAfterModifying();

                        addOperation(Operation.Type.MODIFY, Operation.Entity.FILE, currElementPath);
                    }
                    continue;
                } else {
                    throw new UnsupportedOperationException("Unknown operation with entry");
                }
                prevEvent = currEvent;
                prevElement = currElement;
            }
            resetTimer();
            lock.unlock();
            if (!key.reset()) break;
        }
        shutdown();
    }

    private void addOperation(Operation.Type type, Operation.Entity entity, Path pathToEntity) {
        addOperation(type, entity, pathToEntity, null);
    }

    private void addOperation(Operation.Type type, Operation.Entity entity, Path oldPath, Path newPath) {
        switch (type) {
            case CREATE -> operationsList.add(Operation.create(entity, oldPath));
            case DELETE -> operationsList.add(Operation.delete(entity, oldPath));
            case MODIFY -> operationsList.add(Operation.modify(entity, oldPath));
            case RENAME -> {
                operationsList.remove(operationsList.size() - 1);
                operationsList.add(Operation.rename(entity, oldPath, newPath));
            }
            case MOVE_TO -> {
                operationsList.remove(operationsList.size() - 1);
                operationsList.add(Operation.moveTo(entity, oldPath, newPath));
            }
        }
    }

    private void sendOperations() throws IOException {
        if (lock.tryLock()) {
            for (Operation op : operationsList) {
                System.out.println("SENDING: " + op);
                new MessageSender().send(op, session);
            }
            operationsList = new ArrayList<>();
            lock.unlock();
        }
    }

    private void resetTimer() {
        timer.cancel();
        (timer = new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendOperations();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 100);
    }

    private boolean isFileMoved(Path newFile, File prevFile) throws IOException {
        return newFile.getFileName().toString().equals(prevFile.getName())
                && isFilesEqual(newFile, prevFile);
    }

    private boolean isFileRename(Path newFile, File prevFile) throws IOException {
        return newFile.getParent().equals(prevFile.getParentPath())
                && isFilesEqual(newFile, prevFile);
    }

    private boolean isFilesEqual(Path newFile, File prevFile) throws IOException {
        BasicFileAttributes bfa = Files.getFileAttributeView(newFile, BasicFileAttributeView.class).readAttributes();
        return new Date(bfa.creationTime().toMillis()).equals(prevFile.getCreationTime())
                && new Date(bfa.lastModifiedTime().toMillis()).equals(prevFile.getLastModifiedTime())
                && bfa.size() == prevFile.getSize()
                && CRC32Hash.calculateCrc32Hash(newFile) == prevFile.getCrc32Hash();
    }

    private boolean isDirectoryMoved(Path newDirectory, Directory prevDirectory) throws IOException {
        return newDirectory.getFileName().toString().equals(prevDirectory.getName())
                && isDirectoriesEqual(newDirectory, prevDirectory);
    }

    private boolean isDirectoryRename(Path newDirectory, Directory prevDirectory) throws IOException {
        return newDirectory.getParent().equals(prevDirectory.getParentPath())
                && isDirectoriesEqual(newDirectory, prevDirectory);
    }

    private boolean isDirectoriesEqual(Path newDirectory, Directory prevDirectory) throws IOException {
        BasicFileAttributes bfa = Files.getFileAttributeView(newDirectory, BasicFileAttributeView.class).readAttributes();
        return new Date(bfa.creationTime().toMillis()).equals(prevDirectory.getCreationTime())
                && new Date(bfa.lastModifiedTime().toMillis()).equals(prevDirectory.getLastModifiedTime())
                && !prevDirectory.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Path getChangedPath(WatchKey key, WatchEvent<?> event) {
        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path filename = ev.context();
        return ((Path) key.watchable()).resolve(filename);
    }
}
