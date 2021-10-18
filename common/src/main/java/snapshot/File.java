package snapshot;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import utils.CRC32Hash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Objects;

/**
 * Класс, который содержит в себе необходимые параметры файла для работы с watcher.DirectoryWatcher.
 */
public class File implements Comparable<File>, FileSystemElement {
    @JsonProperty
    private String name;
    @JsonBackReference
    @JsonProperty
    private Directory parentDirectory;
    @JsonProperty
    private Date creationTime;
    @JsonProperty
    private Date lastModifiedTime;
    @JsonProperty
    private long size;
    @JsonProperty
    private long crc32Hash;

    public File() {
    }

    /**
     * Создает "снимок" существующего в файловой системе файла.
     *
     * @param name            имя файла
     * @param parentDirectory родительская директория
     * @throws IOException если такого файла не существует
     */
    public File(String name, Directory parentDirectory) throws IOException {
        this.name = name;
        this.parentDirectory = parentDirectory;
        BasicFileAttributes bfa = Files.getFileAttributeView(this.getPath(), BasicFileAttributeView.class).readAttributes();
        this.creationTime = new Date(bfa.creationTime().toMillis());
        this.lastModifiedTime = new Date(bfa.lastModifiedTime().toMillis());
        this.size = bfa.size();
        this.crc32Hash = CRC32Hash.calculateCrc32Hash(this.getPath());
    }

    /**
     * Переименование файла.
     *
     * @param newName новое имя фала
     */
    public void rename(String newName) {
        this.name = newName;
    }

    /**
     * Перемещение файла в другую директорию.
     *
     * @param newParentDirectory новая родительская директория файла
     */
    public void moveTo(Directory newParentDirectory) {
        newParentDirectory.addFile(this);
    }

    /**
     * Обновление параметров, которые изменяются после модификации файла.
     *
     * @throws IOException если такого файла уже не существует в файловой системе
     */
    public void updateInfoAfterModifying() throws IOException {
        BasicFileAttributes bfa = Files.getFileAttributeView(this.getPath(), BasicFileAttributeView.class).readAttributes();
        this.lastModifiedTime.setTime(bfa.lastModifiedTime().toMillis());
        this.size = bfa.size();
        this.crc32Hash = CRC32Hash.calculateCrc32Hash(this.getPath());
    }

    public String getName() {
        return this.name;
    }

    public Path getPath() {
        return Path.of(parentDirectory.getPath() + "\\" + name);
    }

    public Directory getParentDirectory() {
        return this.parentDirectory;
    }

    public Path getParentPath() {
        return this.parentDirectory.getPath();
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public long getSize() {
        return size;
    }

    public long getCrc32Hash() {
        return crc32Hash;
    }

    public void setParentDirectory(Directory parentDirectory) {
        this.parentDirectory = parentDirectory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        File file = (File) o;
        return this.getPath().equals(file.getPath())
                && this.creationTime.equals(file.creationTime)
                && this.lastModifiedTime.equals(file.lastModifiedTime)
                && this.size == file.size
                && this.crc32Hash == file.crc32Hash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPath(), this.creationTime, this.lastModifiedTime, this.size, this.crc32Hash);
    }

    @Override
    public int compareTo(File anotherFile) {
        return this.getPath().compareTo(anotherFile.getPath());
    }
}
