package snapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;

/**
 * Класс, который представляет собой "снимок" существующей в файловой системе директории.
 * Содержит в себе необходимые параметры директории для работы с watcher.DirectoryWatcher.
 * Для упрощения описания, ""снимок" директории", в большинстве случаев, заменен на "директория".
 */
public class Directory implements Comparable<Directory>, FileSystemElement {

    private String name;
    private Path parentPathForRoot;
    private Directory parentDirectory;
    private final FileTime creationTime;
    private FileTime lastModifiedTime;
    /**
     * Перечень всех подпапок в текущей директории.
     */
    private final Map<String, Directory> subdirectories;
    /**
     * Перечень всех файлов в текущей директории.
     */
    private final Map<String, File> files;

    /**
     * Создает "снимок" существующей в файловой системе директории.
     *
     * @param name   имя директории
     * @param parent родительская директория
     * @throws IOException если такой директории не существует в файловой системе
     */
    private Directory(String name, Path parent) throws IOException {
        this.name = name;
        this.parentPathForRoot = parent;
        this.parentDirectory = null;
        BasicFileAttributes bfa = Files.getFileAttributeView(this.getPath(), BasicFileAttributeView.class).readAttributes();
        this.creationTime = bfa.creationTime();
        this.lastModifiedTime = bfa.lastModifiedTime();
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
    public static Directory createRootDirectory(Path rootPath) throws IOException {
        return new Directory(rootPath.getFileName().toString(), rootPath.getParent());
    }

    /**
     * Создает и возвращает "снимок" директории.
     *
     * @param name       имя директории
     * @param parentPath путь до родительской директории
     * @return "снимок" директории
     * @throws IOException если такой директории не существует в файловой системе
     */
    public static Directory createDirectory(String name, Path parentPath) throws IOException {
        return new Directory(name, parentPath);
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
     * @param newParentDirectory новая родительская директория
     */
    public void moveTo(Directory newParentDirectory) {
        newParentDirectory.addSubdirectory(this);
    }

    /**
     * Добавляет переданную директорию в свой перечень поддиректорий.
     * Для переданной директории текущая директория устанавливается как родительская.
     *
     * @param directory директория, которая будет являтся поддиректорией для текущей
     * @return переданная в метод директория
     */
    public Directory addSubdirectory(Directory directory) {
        this.subdirectories.put(directory.getName(), directory);
        directory.setParentDirectory(this);
        directory.parentPathForRoot = null;
        return directory;
    }

    /**
     * Создает новую директорию с переданным именем и добавляет её в перечень поддиректорий текущей директории.
     *
     * @param name имя директории, которая будет являтся поддиректорией для текущей
     * @return вновь созданная директория с переданным в метод именем
     * @throws IOException если такой директории не существует в файловой системе
     */
    public Directory addSubdirectory(String name) throws IOException {
        return addSubdirectory(new Directory(name, this.getPath()));
    }

    /**
     * Добавляет переданный файл в свой перечень файлов.
     * Для переданной файла текущая директория устанавливается как родительская.
     *
     * @param file файл, для которого текущая директория будет родительской
     * @return переданный в метод файл
     */
    public File addFile(File file) {
        this.files.put(file.getName(), file);
        file.setParentDirectory(this);
        return file;
    }

    /**
     * Создает новый файл с переданным именем и добавляет его в перечень файлов текущей директории.
     *
     * @param name имя файла, для которого текущая директория будет родительской
     * @return вновь созданный файл с переданным в метод именем
     * @throws IOException если такого файла не существует в файловой системе
     */
    public File addFile(String name) throws IOException {
        return addFile(new File(name, this));
    }

    /**
     * Удаляет директорию с переданным именем из списка поддиректорий текущей директории.
     *
     * @param name имя директории
     * @return удаленная из списка поддиректорий директория
     */
    public Directory removeSubdirectory(String name) {
        return this.subdirectories.remove(name);
    }

    /**
     * Удаляет файл с переданным именем из списка файлов текущей директории.
     *
     * @param name имя файла
     * @return удалённый из списка файлов файл
     */
    public File removeFile(String name) {
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

    public Directory getSubdirectory(String name) {
        return this.subdirectories.get(name);
    }

    public File getFile(String name) {
        return this.files.get(name);
    }

    public Set<Directory> getSubdirectories() {
        return new TreeSet<>(this.subdirectories.values());
    }

    public Set<File> getFiles() {
        return new TreeSet<>(this.files.values());
    }

    public String getName() {
        return this.name;
    }

    public Path getPath() {
        return Path.of((parentPathForRoot == null ? parentDirectory.getPath() : this.parentPathForRoot) + "\\" + this.name);
    }

    public Directory getParentDirectory() {
        return parentDirectory;
    }

    public Path getParentPath() {
        return parentDirectory.getPath();
    }

    public FileTime getCreationTime() {
        return creationTime;
    }

    public FileTime getLastModifiedTime() {
        return this.lastModifiedTime;
    }

    public void setParentDirectory(Directory directory) {
        this.parentDirectory = directory;
    }

    public void updateLastModified() throws IOException {
        this.lastModifiedTime = Files.getLastModifiedTime(this.getPath());
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
    Directory getDirectory(String[] splitPath, int pos) {
        Directory dir = subdirectories.get(splitPath[pos]);
        if (pos == splitPath.length - 1) return dir;
        return dir.getDirectory(splitPath, pos + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Directory directory = (Directory) o;
        return this.getPath().equals(directory.getPath())
                && this.creationTime.equals(directory.creationTime)
                && this.lastModifiedTime.equals(directory.lastModifiedTime)
                && this.subdirectories.size() == directory.subdirectories.size()
                && this.files.size() == directory.files.size();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPath(), this.creationTime, this.lastModifiedTime, this.subdirectories.size(), this.files.size());
    }

    @Override
    public int compareTo(Directory anotherDirectory) {
        return this.getPath().compareTo(anotherDirectory.getPath());
    }
}
