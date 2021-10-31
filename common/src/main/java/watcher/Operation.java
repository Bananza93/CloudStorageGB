package watcher;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Представляет собой сообщение о проведенной операции в отслеживаемой watcher.DirectoryWatcher директории.
 */
public class Operation {

    @JsonIgnore
    private static Path watcherRootPath;
    private Entity entity;
    private Type type;
    private String oldEntityPath;
    private String newEntityPath;


    public Operation() {
    }

    /**
     * Для операций CREATE, DELETE, MODIFY
     *
     * @param entity     вид сущности, над которой проводилась оперция
     * @param type       вид операции над сущностью
     * @param entityPath путь до сущности
     */
    private Operation(Entity entity, Type type, String entityPath) {
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
    private Operation(Entity entity, Type type, String oldPath, String newPath) {
        if (watcherRootPath == null)
            throw new RuntimeException("watcher.DirectoryWatcher not initialized");
        this.entity = entity;
        this.type = type;
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

    public static Operation create(Entity entity, Path entityPath) {
        return new Operation(entity, Type.CREATE, entityPath.toString());
    }

    public static Operation delete(Entity entity, Path entityPath) {
        return new Operation(entity, Type.DELETE, entityPath.toString());
    }

    public static Operation modify(Entity entity, Path entityPath) {
        return new Operation(entity, Type.MODIFY, entityPath.toString());
    }

    public static Operation rename(Entity entity, Path oldPath, Path newPath) {
        return new Operation(entity, Type.RENAME, oldPath.toString(), newPath.toString());
    }

    public static Operation moveTo(Entity entity, Path oldPath, Path newPath) {
        return new Operation(entity, Type.MOVE_TO, oldPath.toString(), newPath.toString());
    }

    public static Operation writingFile(Operation operation) {
        if (operation.getEntity() == Entity.FILE) {
            if (operation.getType() == Type.CREATE || operation.getType() == Type.MODIFY) {
                return new Operation(Entity.FILE, Type.FILE_WRITING, operation.oldEntityPath);
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

    public Type getType() {
        return type;
    }

    public String getOldEntityPath() {
        return oldEntityPath;
    }

    public String getNewEntityPath() {
        return newEntityPath;
    }

    public void setType(Type type) {
        this.type = type;
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
        Operation operation = (Operation) o;
        return entity == operation.entity && type == operation.type && Objects.equals(oldEntityPath, operation.oldEntityPath) && Objects.equals(newEntityPath, operation.newEntityPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, type, oldEntityPath, newEntityPath);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name()).append(" ").append(entity.name()).append(" (");
        switch (this.type) {
            case RENAME -> sb.append("oldName = \"").append(oldEntityPath).append("\"")
                    .append(", newName = \"").append(newEntityPath).append("\"");
            case MOVE_TO -> sb.append("oldParentDirectory = \"").append(oldEntityPath).append("\"")
                    .append(", newParentDirectory = \"").append(newEntityPath).append("\"");
            default -> sb.append("entityPath = \"").append(oldEntityPath).append("\"");
        }
        sb.append(")");
        return sb.toString();
    }

    public enum Type {
        CREATE, DELETE, MODIFY, RENAME, MOVE_TO, FILE_WRITING
    }

    public enum Entity {
        DIRECTORY, FILE
    }
}


