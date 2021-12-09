package files;

import client.Client;
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
public class ClientFile extends SimpleFile implements Comparable<ClientFile> {
    private ClientDirectory parentClientDirectory;
    private final Date creationTime;
    private final Date lastModifiedTime;

    /**
     * Создает "снимок" существующего в файловой системе файла.
     *
     * @param path            путь до файла
     * @param parentClientDirectory родительская директория
     * @throws IOException если такого файла не существует
     */
    public ClientFile(Path path, ClientDirectory parentClientDirectory) throws IOException {
        super(path);
        this.parentClientDirectory = parentClientDirectory;
        BasicFileAttributes bfa = Files.getFileAttributeView(this.getFilePath(), BasicFileAttributeView.class).readAttributes();
        this.creationTime = new Date(bfa.creationTime().toMillis());
        this.lastModifiedTime = new Date(bfa.lastModifiedTime().toMillis());
    }

    /**
     * Переименование файла.
     *
     * @param newName новое имя фала
     */
    public void rename(String newName) {
        this.setName(newName);
    }

    /**
     * Перемещение файла в другую директорию.
     *
     * @param newParentClientDirectory новая родительская директория файла
     */
    public void moveTo(ClientDirectory newParentClientDirectory) {
        newParentClientDirectory.addFile(this);
    }

    /**
     * Обновление параметров, которые изменяются после модификации файла.
     *
     * @throws IOException если такого файла уже не существует в файловой системе
     */
    public void updateInfoAfterModifying() throws IOException {
        BasicFileAttributes bfa = Files.getFileAttributeView(this.getFilePath(), BasicFileAttributeView.class).readAttributes();
        this.lastModifiedTime.setTime(bfa.lastModifiedTime().toMillis());
        this.setSize(bfa.size());
        this.setCrc32Hash(CRC32Hash.calculateCrc32Hash(this.getFilePath()));
    }

    public Path getFilePath() {
        return Path.of(this.getAbsolutePath());
    }

    public Path getParentPath() {
        return this.parentClientDirectory.getPath();
    }

    public Date getCreationTime() {
        return this.creationTime;
    }

    public Date getLastModifiedTime() {
        return this.lastModifiedTime;
    }

    public ClientDirectory getParentDirectory() {
        return this.parentClientDirectory;
    }

    public void setParentDirectory(ClientDirectory parentClientDirectory) {
        this.parentClientDirectory = parentClientDirectory;
    }

    public Path getPathWithoutRootPart() {
        return Path.of(this.getFilePath().toString().replace(Client.getWorkDirPath(), ""));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientFile clientFile = (ClientFile) o;
        return this.getFilePath().equals(clientFile.getFilePath())
                && this.creationTime.equals(clientFile.creationTime)
                && this.lastModifiedTime.equals(clientFile.lastModifiedTime)
                && this.getSize() == clientFile.getSize()
                && this.getCrc32Hash() == clientFile.getCrc32Hash();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getFilePath(), this.creationTime, this.lastModifiedTime, this.getSize(), this.getCrc32Hash());
    }

    @Override
    public int compareTo(ClientFile anotherClientFile) {
        return this.getFilePath().compareTo(anotherClientFile.getFilePath());
    }
}
