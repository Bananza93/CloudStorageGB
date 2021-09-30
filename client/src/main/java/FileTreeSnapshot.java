import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileTreeSnapshot {

    private final Path initialPath;
    private final Directory initialDirectory;
    private boolean busy;

    public FileTreeSnapshot(Path initialPath) throws IOException {
        this.initialPath = initialPath;
        this.busy = false;
        System.out.println("Root: " + initialPath.getParent() + ", Name: " + initialPath.getFileName().toString());
        this.initialDirectory = createSnapshot();
    }

    public Directory fillDirectory(Path startPath) throws IOException {
        busy = true;
        final Directory[] currDir = {new Directory(startPath.getFileName().toString(), startPath.getParent())};
        try {
            Files.walkFileTree(currDir[0].getPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    //System.out.println("Start working with dir: " + dir.toString());
                    if (!dir.equals(startPath)) {
                        Directory d = new Directory(dir.getFileName().toString(), dir.getParent());
                        currDir[0].addDirectory(d);
                        currDir[0] = d;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    //System.out.println("Start working with file: " + file.toString());
                    currDir[0].addFile(new File(file.getFileName().toString(), file.getParent()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.out.println("Problem with file: " + file.toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    //System.out.println("Stop working with dir: " + dir.toString());
                    currDir[0] = currDir[0].getParentDirectory();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }
        busy = false;
        return currDir[0];
    }

    public boolean isBusy() {
        return this.busy;
    }

    public Directory createSnapshot() throws IOException {
        return fillDirectory(initialPath);
    }

    public Directory getInitialDirectory() {
        return this.initialDirectory;
    }

    public void printFileTree() {
        printFileTree0(initialDirectory, 0);
    }

    private void printFileTree0(Directory start, int offset) {
        System.out.println("\t".repeat(offset) + "[" + start.getName() + "]");
        for (Directory dir : start.getSubdirectories()) {
            printFileTree0(dir, offset + 1);
        }
        for (File f : start.getFiles()) {
            System.out.println("\t".repeat(offset + 1) + f.getName());
        }
    }
}
