package files;

import utils.ThreadPool;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Используется для создания "снимка" дерева файлов, начиная от заданной директории.
 * Для последующей работы с watcher.DirectoryWatcher.
 */
public class FileTreeSnapshot {

    private final Path initialPath;
    private ClientDirectory initialClientDirectory;
    private static boolean computing = false;

    /**
     * Создает "снимок" файловой системы, стартовой точкой которого будет являться корневой каталог.
     * Если директории по указанному пути не существует - создает ее.
     *
     * @param initialPath путь до корневого каталога
     * @throws IOException если в процессе создания "снимка" в стартовой директории произойдут изменения или в случае недостатка привелегий для открытия директории/файла.
     */
    public FileTreeSnapshot(Path initialPath) throws IOException {
        if (!Files.exists(initialPath)) {
            Files.createDirectories(initialPath);
        }
        this.initialPath = initialPath;
        ThreadPool.addTask(() -> {
            try {
                createSnapshot();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void createSnapshot() throws IOException {
        long start = System.currentTimeMillis();
        System.out.println("Creating snapshot for " + initialPath + "...");
        this.initialClientDirectory = fillDirectory(initialPath);
        System.out.printf("Snapshot created (%.3f sec).\n", ((System.currentTimeMillis() - start) / 1000.0));
    }

    /**
     * @param startPath стартовый путь "снимка"
     * @return "снимок" файловой системы
     * @throws IOException если в процессе создания "снимка" в стартовой директории произойдут изменения или в случае недостатка привелегий для открытия директории/файла.
     */
    public ClientDirectory fillDirectory(Path startPath) throws IOException {
        computing = true;
        ClientDirectory result = ClientDirectory.createRootDirectory(startPath);
        final ClientDirectory[] currDir = {result};
        try {
            Files.walkFileTree(currDir[0].getPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(startPath)) {
                        ClientDirectory d = ClientDirectory.createDirectory(dir.getFileName().toString(), dir.getParent());
                        currDir[0].addSubdirectory(d);
                        currDir[0] = d;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    currDir[0].addFile(new ClientFile(file, currDir[0]));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.out.println("Problem with file: " + file.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    currDir[0] = currDir[0].getParentDirectory();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IOException(e);
        }
        computing = false;
        return result;
    }

    /**
     * Рекурсивный поиск директории по переданному пути.
     *
     * @param path путь до директории
     * @return директория, соответствующая переданному пути
     */
    public ClientDirectory getDirectory(Path path) {
        String cutPath = path.toString().replace(initialPath.toString(), "");
        if (cutPath.isEmpty()) return initialClientDirectory;
        else {
            String[] s = cutPath.substring(1).split("\\\\");
            return initialClientDirectory.getDirectory(s, 0);
        }
    }

    public static boolean isComputing() {
        return computing;
    }

    public ClientDirectory getInitialDirectory() {
        return this.initialClientDirectory;
    }

    public Path getInitialPath() {
        return this.initialPath;
    }

    /**
     * Выводит в консоль визуальное представление текущего "снимка"
     */
    public void printFileTree() {
        printFileTree0(initialClientDirectory, 0);
    }

    private void printFileTree0(ClientDirectory start, int offset) {
        System.out.println("\t".repeat(offset) + "[" + start.getName() + "]");
        for (ClientDirectory dir : start.getSubdirectories()) {
            printFileTree0(dir, offset + 1);
        }
        for (ClientFile f : start.getFiles()) {
            System.out.println("\t".repeat(offset + 1) + f.getName());
        }
    }

}
