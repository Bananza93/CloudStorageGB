package watcher;

import com.sun.nio.file.ExtendedWatchEventModifier;
import handlers.MessageHandler;
import handlers.SessionHandler;
import operations.FileOperation;
import operations.OperationType;
import files.ClientDirectory;
import files.ClientFile;
import files.FileSystemElement;
import files.FileTreeSnapshot;
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
    private List<FileOperation> operationsList;
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
        FileOperation.setWatcherRootPath(rootDirectory);
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
        FileOperation.setWatcherRootPath(null);
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
                ClientDirectory currElementParentDir = snapshot.getDirectory(currElementPath.getParent());
                String currElementName = currElementPath.getFileName().toString();
                FileSystemElement currElement;

                if (kind == ENTRY_CREATE) {
                    if (Files.isDirectory(currElementPath)) {
                        if (prevEvent != null && prevEvent.kind() == ENTRY_DELETE && !operationsList.isEmpty() && prevElement instanceof ClientDirectory prevDir) {
                            if (isDirectoryRename(currElementPath, prevDir)) {
                                Path prevPath = prevDir.getPath();
                                prevDir.rename(currElementName);
                                currElement = currElementParentDir.addSubdirectory(prevDir);
                                addOperation(OperationType.RENAME, FileOperation.Entity.DIRECTORY, prevPath, prevDir.getPath());
                            } else if (isDirectoryMoved(currElementPath, prevDir)) {
                                Path prevPath = prevDir.getPath();
                                prevDir.moveTo(currElementParentDir);
                                currElement = prevDir;
                                addOperation(OperationType.MOVE_TO, FileOperation.Entity.DIRECTORY, prevPath, prevDir.getPath());
                            } else {
                                currElement = currElementParentDir.addSubdirectory(currElementName);
                                addOperation(OperationType.CREATE, FileOperation.Entity.DIRECTORY, currElementPath);
                            }
                        } else {
                            currElement = currElementParentDir.addSubdirectory(currElementName);
                            addOperation(OperationType.CREATE, FileOperation.Entity.DIRECTORY, currElementPath);
                        }
                    } else {
                        if (prevEvent != null && prevEvent.kind() == ENTRY_DELETE && !operationsList.isEmpty() && prevElement instanceof ClientFile prevClientFile) {
                            if (isFileRename(currElementPath, prevClientFile)) {
                                Path prevPath = prevClientFile.getFilePath();
                                prevClientFile.rename(currElementName);
                                currElement = currElementParentDir.addFile(prevClientFile);
                                addOperation(OperationType.RENAME, FileOperation.Entity.FILE, prevPath, prevClientFile.getFilePath());
                            } else if (isFileMoved(currElementPath, prevClientFile)) {
                                Path prevPath = prevClientFile.getFilePath();
                                prevClientFile.moveTo(currElementParentDir);
                                currElement = prevClientFile;
                                addOperation(OperationType.MOVE_TO, FileOperation.Entity.FILE, prevPath, prevClientFile.getFilePath());
                            } else {
                                currElement = currElementParentDir.addFile(currElementPath);
                                addOperation(OperationType.CREATE, FileOperation.Entity.FILE, currElementPath);
                            }
                        } else {
                            currElement = currElementParentDir.addFile(currElementPath);
                            addOperation(OperationType.CREATE, FileOperation.Entity.FILE, currElementPath);
                        }
                    }
                } else if (kind == ENTRY_DELETE) {
                    if (currElementParentDir.containsSubdirectory(currElementName)) {
                        currElement = currElementParentDir.removeSubdirectory(currElementName);
                        addOperation(OperationType.DELETE, FileOperation.Entity.DIRECTORY, currElementPath);
                    } else {
                        currElement = currElementParentDir.removeFile(currElementName);
                        addOperation(OperationType.DELETE, FileOperation.Entity.FILE, currElementPath);
                    }
                } else if (kind == ENTRY_MODIFY) {
                    if (Files.isDirectory(currElementPath)) {
                        currElementParentDir.getSubdirectory(currElementName).updateLastModified();
                        addOperation(OperationType.MODIFY, FileOperation.Entity.DIRECTORY, currElementPath);
                    } else {
                        currElementParentDir.getFile(currElementName).updateInfoAfterModifying();

                        addOperation(OperationType.MODIFY, FileOperation.Entity.FILE, currElementPath);
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

    private void addOperation(OperationType type, FileOperation.Entity entity, Path pathToEntity) {
        addOperation(type, entity, pathToEntity, null);
    }

    private void addOperation(OperationType type, FileOperation.Entity entity, Path oldPath, Path newPath) {
        switch (type) {
            case CREATE -> operationsList.add(FileOperation.create(entity, oldPath));
            case DELETE -> operationsList.add(FileOperation.delete(entity, oldPath));
            case MODIFY -> operationsList.add(FileOperation.modify(entity, oldPath));
            case RENAME -> {
                operationsList.remove(operationsList.size() - 1);
                operationsList.add(FileOperation.rename(entity, oldPath, newPath));
            }
            case MOVE_TO -> {
                operationsList.remove(operationsList.size() - 1);
                operationsList.add(FileOperation.moveTo(entity, oldPath, newPath));
            }
        }
    }

    private void sendOperations() throws IOException {
        if (lock.tryLock()) {
            for (FileOperation op : operationsList) {
                System.out.println("SENDING: " + op);
                new MessageHandler().send(op, session);
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

    private boolean isFileMoved(Path newFile, ClientFile prevClientFile) throws IOException {
        return newFile.getFileName().toString().equals(prevClientFile.getName())
                && isFilesEqual(newFile, prevClientFile);
    }

    private boolean isFileRename(Path newFile, ClientFile prevClientFile) throws IOException {
        return newFile.getParent().equals(prevClientFile.getParentPath())
                && isFilesEqual(newFile, prevClientFile);
    }

    private boolean isFilesEqual(Path newFile, ClientFile prevClientFile) throws IOException {
        BasicFileAttributes bfa = Files.getFileAttributeView(newFile, BasicFileAttributeView.class).readAttributes();
        return new Date(bfa.creationTime().toMillis()).equals(prevClientFile.getCreationTime())
                && new Date(bfa.lastModifiedTime().toMillis()).equals(prevClientFile.getLastModifiedTime())
                && bfa.size() == prevClientFile.getSize()
                && CRC32Hash.calculateCrc32Hash(newFile) == prevClientFile.getCrc32Hash();
    }

    private boolean isDirectoryMoved(Path newDirectory, ClientDirectory prevClientDirectory) throws IOException {
        return newDirectory.getFileName().toString().equals(prevClientDirectory.getName())
                && isDirectoriesEqual(newDirectory, prevClientDirectory);
    }

    private boolean isDirectoryRename(Path newDirectory, ClientDirectory prevClientDirectory) throws IOException {
        return newDirectory.getParent().equals(prevClientDirectory.getParentPath())
                && isDirectoriesEqual(newDirectory, prevClientDirectory);
    }

    private boolean isDirectoriesEqual(Path newDirectory, ClientDirectory prevClientDirectory) throws IOException {
        BasicFileAttributes bfa = Files.getFileAttributeView(newDirectory, BasicFileAttributeView.class).readAttributes();
        return new Date(bfa.creationTime().toMillis()).equals(prevClientDirectory.getCreationTime())
                && new Date(bfa.lastModifiedTime().toMillis()).equals(prevClientDirectory.getLastModifiedTime())
                && !prevClientDirectory.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Path getChangedPath(WatchKey key, WatchEvent<?> event) {
        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path filename = ev.context();
        return ((Path) key.watchable()).resolve(filename);
    }
}
