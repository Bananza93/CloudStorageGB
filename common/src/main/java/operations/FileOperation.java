package operations;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Представляет собой сообщение о проведенной операции в отслеживаемой watcher.DirectoryWatcher директории.
 */
public class FileOperation extends Operation {

    @JsonIgnore
    private static Path watcherRootPath;
    private Entity entity;
    private String oldEntityPath;
    private String newEntityPath;


    public FileOperation() {
    }

    /**
     * Для операций CREATE, DELETE, MODIFY
     *
     * @param entity     вид сущности, над которой проводилась оперция
     * @param type       вид операции над сущностью
     * @param entityPath путь до сущности
     */
    private FileOperation(Entity entity, OperationType type, String entityPath) {
        this(entity, type, entityPath, null);
    }

    /**
     * Для операций RENAME, MOVE_TO
     *
     * @param entity  сущность, над которой проводилась оперция
     * @param type    вид операции над сущностью
     * @param oldPath прежний путь до сущности
     * @param newPath новый путь до сущности
     */
    private FileOperation(Entity entity, OperationType type, String oldPath, String newPath) {
        super(type);
        this.entity = entity;
        this.oldEntityPath = oldPath;
        this.newEntityPath = newPath == null ? "" : newPath;
    }

    /**
     * Устанавливает путь до корневой дирекории, на которую настроен watcher.DirectoryWatcher
     *
     * @param path путь до корневой директории
     */
    public static void setWatcherRootPath(Path path) {
        watcherRootPath = path;
    }

    public static FileOperation create(Entity entity, Path entityPath) {
        return new FileOperation(entity, OperationType.CREATE, entityPath.toString());
    }

    public static FileOperation delete(Entity entity, Path entityPath) {
        return new FileOperation(entity, OperationType.DELETE, entityPath.toString());
    }

    public static FileOperation modify(Entity entity, Path entityPath) {
        return new FileOperation(entity, OperationType.MODIFY, entityPath.toString());
    }

    public static FileOperation rename(Entity entity, Path oldPath, Path newPath) {
        return new FileOperation(entity, OperationType.RENAME, oldPath.toString(), newPath.toString());
    }

    public static FileOperation copy(Entity entity, Path fromPath, Path toPath) {
        return new FileOperation(entity, OperationType.COPY, fromPath.toString(), toPath.toString());
    }

    public static FileOperation moveTo(Entity entity, Path fromPath, Path toPath) {
        return new FileOperation(entity, OperationType.MOVE_TO, fromPath.toString(), toPath.toString());
    }

    public static FileOperation writingFile(FileOperation operation) {
        if (operation.getEntity() == Entity.FILE) {
            if (operation.getType() == OperationType.CREATE || operation.getType() == OperationType.MODIFY) {
                return new FileOperation(Entity.FILE, OperationType.FILE_WRITING, operation.oldEntityPath);
            }
        }
        throw new RuntimeException("Unsupported combination (ENTITY = " + operation.getEntity() + ", TYPE = " + operation.getType() + ") for this operation.");
    }

    public static Path getWatcherRootPath() {
        return watcherRootPath;
    }

    public Entity getEntity() {
        return entity;
    }

    public String getOldEntityPath() {
        return oldEntityPath;
    }

    public String getNewEntityPath() {
        return newEntityPath;
    }

    public void setOldEntityPath(String oldEntityPath) {
        this.oldEntityPath = oldEntityPath;
    }

    public void setNewEntityPath(String newEntityPath) {
        this.newEntityPath = newEntityPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileOperation operation = (FileOperation) o;
        return entity == operation.entity && this.getType() == operation.getType() && Objects.equals(oldEntityPath, operation.oldEntityPath) && Objects.equals(newEntityPath, operation.newEntityPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, this.getType(), oldEntityPath, newEntityPath);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getType().name()).append(" ").append(entity.name()).append(" (");
        switch (this.getType()) {
            case RENAME -> sb.append("oldName = \"").append(oldEntityPath).append("\"")
                    .append(", newName = \"").append(newEntityPath).append("\"");
            case MOVE_TO -> sb.append("oldParentDirectory = \"").append(oldEntityPath).append("\"")
                    .append(", newParentDirectory = \"").append(newEntityPath).append("\"");
            default -> sb.append("entityPath = \"").append(oldEntityPath).append("\"");
        }
        sb.append(")");
        return sb.toString();
    }

    public enum Entity {
        DIRECTORY, FILE
    }
}


