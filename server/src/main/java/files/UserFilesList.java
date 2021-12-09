package files;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserFilesList {

    private Path userDir;
    private Map<Path, SimpleFile> userFiles;
    private Map<Long, ArrayList<SimpleFile>> fileHashes;

    public UserFilesList(Path userDir) {
        this.userDir = userDir;
        this.userFiles = new HashMap<>();
        this.fileHashes = new HashMap<>();
        fill();
    }

    private void fill() {
        try {
            Files.walkFileTree(userDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    SimpleFile f = new SimpleFile(file);
                    userFiles.put(file, f);
                    ArrayList<SimpleFile> currPathListForHash = fileHashes.getOrDefault(f.getCrc32Hash(), new ArrayList<>());
                    if (currPathListForHash.isEmpty()) {
                        fileHashes.put(f.getCrc32Hash(), currPathListForHash);
                    } else {
                        currPathListForHash.add(f);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.out.println("Problem with file: " + file.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println("Caught exception while filling the userFiles (processDir = " + userDir + "): " + e.getCause() + " | " + e.getMessage());
        }
    }

    public List<SimpleFile> getFilesListForTransfer() {
        ArrayList<SimpleFile> list = new ArrayList<>(userFiles.values());
        for (SimpleFile f : list) {
            f.setPath(f.getPath().replace(userDir.toString(), ""));
        }
        return list;
    }

    public List<SimpleFile> getFilesByHashSum(long hashSum) {
        return fileHashes.get(hashSum);
    }

    public SimpleFile getFileByPath(Path path) {
        return userFiles.get(path);
    }
}
