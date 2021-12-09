package files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Класс, который представляет собой "снимок" существующей в файловой системе директории.
 * Содержит в себе необходимые параметры директории для работы с watcher.DirectoryWatcher.
 * Для упрощения описания, ""снимок" директории", в большинстве случаев, заменен на "директория".
 */
public class ClientDirectory implements Comparable<ClientDirectory>, FileSystemElement {
    private String name;
    private Path parentPathForRoot;
    private ClientDirectory parentClientDirectory;
    private Date creationTime;
    private Date lastModifiedTime;

    /**
     * Перечень всех подпапок в текущей директории.
     */
    private Map<String, ClientDirectory> subdirectories;

    /**
     * Перечень всех файлов в текущей директории.
     */
    private Map<String, ClientFile> files;

    /**
     * Создает "снимок" существующей в файловой системе директории.
     *
     * @param name   имя директории
     * @param parent родительская директория
     * @throws IOException если такой директории не существует в файловой системе
     */
    private ClientDirectory(String name, Path parent) throws IOException {
        this.name = name;
        this.parentPathForRoot = parent;
        this.parentClientDirectory = null;
        BasicFileAttributes bfa = Files.getFileAttributeView(this.getPath(), BasicFileAttributeView.class).readAttributes();
        this.creationTime = new Date(bfa.creationTime().toMillis());
        this.lastModifiedTime = new Date(bfa.lastModifiedTime().toMillis());
        this.subdirectories = new HashMap<>();
        this.files = new HashMap<>();
    }

    /**
     * Создает и возвращает "снимок" корневой директории.
     *
     * @param rootPath путь до корневой директории
     * @return "снимок" корневой директории
     * @throws IOException если такой директории не существует в файловой системе
     */
    public static ClientDirectory createRootDirectory(Path rootPath) throws IOException {
        ClientDirectory d =  new ClientDirectory(rootPath.getFileName().toString(), rootPath.getParent());
        d.setParentDirectory(new ClientDirectory("", Path.of("")));
        return d;
    }

    /**
     * Создает и возвращает "снимок" директории.
     *
     * @param name       имя директории
     * @param parentPath путь до родительской директории
     * @return "снимок" директории
     * @throws IOException если такой директории не существует в файловой системе
     */
    public static ClientDirectory createDirectory(String name, Path parentPath) throws IOException {
        return new ClientDirectory(name, parentPath);
    }

    /**
     * Переименовывает текущую директорию.
     *
     * @param newName новое имя директории
     */
    public void rename(String newName) {
        this.name = newName;
    }

    /**
     * Перемещает текущую директорию.
     *
     * @param newParentClientDirectory новая родительская директория
     */
    public void moveTo(ClientDirectory newParentClientDirectory) {
        newParentClientDirectory.addSubdirectory(this);
    }

    /**
     * Добавляет переданную директорию в свой перечень поддиректорий.
     * Для переданной директории текущая директория устанавливается как родительская.
     *
     * @param clientDirectory директория, которая будет являтся поддиректорией для текущей
     * @return переданная в метод директория
     */
    public ClientDirectory addSubdirectory(ClientDirectory clientDirectory) {
        this.subdirectories.put(clientDirectory.getName(), clientDirectory);
        clientDirectory.setParentDirectory(this);
        clientDirectory.parentPathForRoot = null;
        return clientDirectory;
    }

    /**
     * Создает новую директорию с переданным именем и добавляет её в перечень поддиректорий текущей директории.
     *
     * @param name имя директории, которая будет являтся поддиректорией для текущей
     * @return вновь созданная директория с переданным в метод именем
     * @throws IOException если такой директории не существует в файловой системе
     */
    public ClientDirectory addSubdirectory(String name) throws IOException {
        return addSubdirectory(new ClientDirectory(name, this.getPath()));
    }

    /**
     * Добавляет переданный файл в свой перечень файлов.
     * Для переданной файла текущая директория устанавливается как родительская.
     *
     * @param clientFile файл, для которого текущая директория будет родительской
     * @return переданный в метод файл
     */
    public ClientFile addFile(ClientFile clientFile) {
        this.files.put(clientFile.getName(), clientFile);
        clientFile.setParentDirectory(this);
        clientFile.setPath(this.getPath().toString());
        return clientFile;
    }

    /**
     * Создает новый файл с переданным именем и добавляет его в перечень файлов текущей директории.
     *
     * @param path путь до файла, для которого текущая директория будет родительской
     * @return вновь созданный файл с переданным в метод именем
     * @throws IOException если такого файла не существует в файловой системе
     */
    public ClientFile addFile(Path path) throws IOException {
        return addFile(new ClientFile(path, this));
    }

    /**
     * Удаляет директорию с переданным именем из списка поддиректорий текущей директории.
     *
     * @param name имя директории
     * @return удаленная из списка поддиректорий директория
     */
    public ClientDirectory removeSubdirectory(String name) {
        return this.subdirectories.remove(name);
    }

    /**
     * Удаляет файл с переданным именем из списка файлов текущей директории.
     *
     * @param name имя файла
     * @return удалённый из списка файлов файл
     */
    public ClientFile removeFile(String name) {
        return this.files.remove(name);
    }

    /**
     * Проверяет, находится ли директория с переданным именем в списке поддиректорий текущей директории.
     *
     * @param directoryName имя директории
     * @return true - если директория с переданным именем находится в списке поддиректорий
     */
    public boolean containsSubdirectory(String directoryName) {
        return subdirectories.containsKey(directoryName);
    }

    /**
     * Проверяет, находится ли файл с переданным именем в списке файлов текущей директории.
     *
     * @param fileName имя файла
     * @return true - если файл с переданным именем находится в списке файлов
     */
    public boolean containsFile(String fileName) {
        return files.containsKey(fileName);
    }

    public ClientDirectory getSubdirectory(String name) {
        return this.subdirectories.get(name);
    }

    public ClientFile getFile(String name) {
        return this.files.get(name);
    }

    public Set<ClientDirectory> getSubdirectories() {
        return new TreeSet<>(this.subdirectories.values());
    }

    public Set<ClientFile> getFiles() {
        return new TreeSet<>(this.files.values());
    }

    public String getName() {
        return this.name;
    }

    public Path getPath() {
        return Path.of((parentPathForRoot == null ? parentClientDirectory.getPath() : this.parentPathForRoot) + "\\" + this.name);
    }

    public ClientDirectory getParentDirectory() {
        return parentClientDirectory;
    }

    public Path getParentPath() {
        return parentClientDirectory.getPath();
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public Date getLastModifiedTime() {
        return this.lastModifiedTime;
    }

    public void setParentDirectory(ClientDirectory clientDirectory) {
        this.parentClientDirectory = clientDirectory;
    }

    public void updateLastModified() throws IOException {
        this.lastModifiedTime.setTime(Files.getLastModifiedTime(this.getPath()).toMillis());
    }

    public boolean isEmpty() {
        return subdirectories.isEmpty() && files.isEmpty();
    }

    /**
     * Рекурсивно осуществляет поиск директории по переданному пути.
     * Составная часть метода getDirectory() класса FileTreeSnapshot.
     *
     * @param splitPath путь, поделенный на отдельные директории
     * @param pos       текущая позиция в массиве splitPath
     * @return директорию, которой соответствует переданный путь, иначе - null
     */
    ClientDirectory getDirectory(String[] splitPath, int pos) {
        ClientDirectory dir = subdirectories.get(splitPath[pos]);
        if (pos == splitPath.length - 1) return dir;
        return dir.getDirectory(splitPath, pos + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientDirectory clientDirectory = (ClientDirectory) o;
        return this.getPath().equals(clientDirectory.getPath())
                && this.creationTime.equals(clientDirectory.creationTime)
                && this.lastModifiedTime.equals(clientDirectory.lastModifiedTime)
                && this.subdirectories.size() == clientDirectory.subdirectories.size()
                && this.files.size() == clientDirectory.files.size();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPath(), this.creationTime, this.lastModifiedTime, this.subdirectories.size(), this.files.size());
    }

    @Override
    public int compareTo(ClientDirectory anotherClientDirectory) {
        return this.getPath().compareTo(anotherClientDirectory.getPath());
    }
}
