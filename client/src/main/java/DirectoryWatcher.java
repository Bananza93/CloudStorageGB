import java.io.IOException;
import java.nio.file.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatcher {
    private final WatchService watcher;
    private final Path workDir;
    private final HashMap<Path, WatchKey> keys;

    boolean wasCreated = false;
    boolean wasDeleted = false;
    boolean wasModified = false;
    boolean isDirectory = false;
    boolean isFile = false;
    Path created = null;
    Path deleted = null;
    Path modified = null;

    public DirectoryWatcher(String workDir) throws IOException {
        watcher = FileSystems.getDefault().newWatchService();
        this.workDir = Path.of(workDir);
        keys = new HashMap<>();
        keys.put(this.workDir, this.workDir.register(this.watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
    }


    public void start() throws IOException, InterruptedException {
        while (true) {
            WatchKey key;

            try {
                key = watcher.take();
                Thread.sleep(500); // для корректной наполянемости ключа событиями (исключение разрыва событий для одного контекста?)
            } catch (InterruptedException e) {
                return;
            }

            WatchEvent<?> prevEvent = null;
            Path prevPath = null;
            List<WatchEvent<?>> events = key.pollEvents();
            printToConsole("Number of events: " + events.size());

            for (WatchEvent<?> event : events) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == OVERFLOW) continue;
                Path changed = getChangedPath(key, event);

                if (prevPath != null && !changed.equals(prevPath)) {
                    // выполняем нужные дейсвтия для предшествующего контекста
                    process(key, wasCreated, wasDeleted, wasModified, isDirectory, isFile, created, deleted, modified);
                    // и сбрасываем флаги
                    resetFlags();
                }

                printToConsole("Prev: " + (prevEvent == null ? null : prevEvent.kind()) + " - " + (prevEvent == null ? null : prevEvent.context())
                        + " | Curr: " + event.kind() + " - " + event.context()
                        + " | isEquals: " + Objects.equals(prevEvent, event));

                if (kind == ENTRY_CREATE) {
                    wasCreated = true;
                    created = changed;
                    if (Files.isDirectory(created)) {
                        keys.put(changed, changed.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
                        isDirectory = true;
                    } else {
                        isFile = true;
                    }
                } else if (kind == ENTRY_DELETE) {
                    wasDeleted = true;
                    deleted = changed;
                    if (keys.containsKey(deleted)) {
                        keys.remove(changed).cancel();
                        isDirectory = true;
                    } else {
                        isFile = true;
                    }
                } else if (kind == ENTRY_MODIFY) {
                    wasModified = true;
                    modified = changed;
                    if (Files.isDirectory(modified)) {
                        isDirectory = true;
                    } else {
                        isFile = true;
                    }
                }
                prevEvent = event;
                prevPath = changed;
            }

            process(key, wasCreated, wasDeleted, wasModified, isDirectory, isFile, created, deleted, modified);
            resetFlags();

            boolean valid = key.reset();
            if (!valid) {
                if (((Path) key.watchable()).compareTo(workDir) == 0) break;
            }
        }
        watcher.close();
    }

    private void resetFlags() {
        wasCreated = false;
        wasDeleted = false;
        wasModified = false;
        isDirectory = false;
        isFile = false;
        created = null;
        deleted = null;
        modified = null;
    }

    private void process(WatchKey key, boolean wasCreated, boolean wasDeleted, boolean wasModified, boolean isDirectory, boolean isFile, Path created, Path deleted, Path modified) throws InterruptedException {
        if (isDirectory) {
            if (wasDeleted) {
                if (wasCreated) {
                    //переименование директории
                    printToConsole("Dir renamed: from " + excludeWorkDirFromPath(deleted) + " to " + excludeWorkDirFromPath(created));
                } else {
                    //удаление директории
                    printToConsole("Dir deleted: " + excludeWorkDirFromPath(deleted));
                }
            } else if (wasCreated) {
                //Создание директории
                printToConsole("Dir created: " + excludeWorkDirFromPath(created));
            } else if (wasModified) {
                //модификация директории
                printToConsole("Dir modified: " + excludeWorkDirFromPath(modified));
            } else {
                printToConsole("Something wrong");
            }
        } else if (isFile) {
            if (wasDeleted) {
                if (wasCreated) {
                    //переименование файла
                    printToConsole("File renamed: from " + excludeWorkDirFromPath(deleted) + " to " + excludeWorkDirFromPath(created));
                } else {
                    //удаление файла
                    printToConsole("File deleted: " + excludeWorkDirFromPath(deleted));
                }
            } else if (wasCreated) {
                //Создание файла
                printToConsole("File created: " + excludeWorkDirFromPath(created));
            } else if (wasModified) {
                //модификация файла
                printToConsole("File modified: " + excludeWorkDirFromPath(modified));
            } else {
                printToConsole("Something wrong");
            }
        } else {
            //Рекурсивное удаление?
            Path p = (Path) key.watchable();
            if (keys.containsKey(p)) {
                keys.remove(p).cancel();
            }
            printToConsole("KEY: " + key.watchable() + " recursively deleted");
        }
    }

    @SuppressWarnings("unchecked")
    private Path getChangedPath(WatchKey key, WatchEvent<?> event) {
        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path filename = ev.context();
        return ((Path) key.watchable()).resolve(filename);
    }

    private String excludeWorkDirFromPath(Path path) {
        return path.toString().replace(workDir.toString(), "");
    }

    private void printToConsole(String msg) throws InterruptedException {
        String prefix = new Date() + " (" + Thread.currentThread().getName() + ") ";
        System.out.println(prefix + msg);
    }
}
