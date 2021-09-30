import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;

public class Directory implements Comparable<Directory> {
    private String name;
    private Path parentPath;
    private Directory parentDirectory;
    private FileTime lastModified;
    private Map<String, Directory> subdirectories;
    private Map<String, File> files;

    public Directory(String name, Path parent) throws IOException {
        this.name = name;
        this.parentPath = parent;
        this.parentDirectory = null;
        this.lastModified = Files.getLastModifiedTime(this.getPath());
        this.subdirectories = new HashMap<>();
        this.files = new HashMap<>();
    }

    public Directory getParentDirectory() {
        return parentDirectory;
    }

    public boolean isInitialDirectory() {
        return parentDirectory == null;
    }

    public void addDirectory(Directory directory) {
        this.subdirectories.put(directory.getName(), directory);
        directory.setParentDirectory(this);
    }

    public void addFile(File file) {
        this.files.put(file.getName(), file);
    }

    public Directory getSubdirectory(String name) {
        return this.subdirectories.get(name);
    }

    public File getFile(String name) {
        return this.files.get(name);
    }

    public Directory removeSubdirectory(String name) {
        return this.subdirectories.remove(name);
    }

    public File removeFile(String name) {
        return this.files.remove(name);
    }

    public String getName() {
        return this.name;
    }

    public Path getParentPath() {
        return this.parentPath;
    }

    public Path getPath() {
        return Path.of(this.parentPath + "\\" + this.name);
    }

    public Set<Directory> getSubdirectories() {
        return new TreeSet<>(this.subdirectories.values());
    }

    public Set<File> getFiles() {
        return new TreeSet<>(this.files.values());
    }

    public FileTime getLastModified() {
        return this.lastModified;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public void move(Path newRootPath) {
        this.parentPath = newRootPath;
    }

    public void setParentDirectory(Directory directory) {
        this.parentDirectory = directory;
    }

    public void updateLastModified() throws IOException {
        this.lastModified = Files.getLastModifiedTime(this.getPath());;
    }

    public boolean isEmpty() {
        return this.subdirectories.isEmpty() && this.files.isEmpty();
    }

    private boolean isNameEquals(Directory anotherDirectory) {
        return this.name.equals(anotherDirectory.name);
    }

    private boolean isRootPathEquals(Directory anotherDirectory) {
        return this.parentPath.equals(anotherDirectory.parentPath);
    }

    private boolean isLastModifiedEquals(Directory anotherDirectory) {
        return this.lastModified.equals(anotherDirectory.lastModified);
    }

    @Override
    public int compareTo(Directory anotherDirectory) {
        return this.name.compareTo(anotherDirectory.name);
    }
}
