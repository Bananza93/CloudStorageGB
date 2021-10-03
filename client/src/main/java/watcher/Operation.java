package watcher;

import java.nio.file.Path;

/**
 * Представляет собой сообщение о проведенной операции в отслеживаемой watcher.DirectoryWatcher директории.
 */
public class Operation {

    private static Path watcherRootPath;
    private Entity entity;
    private OperationType type;
    private Path entityPath;
    private Path oldName;
    private Path newName;
    private Path oldParentDirectory;
    private Path newParentDirectory;

    /**
     * Для операций CREATE, DELETE, MODIFY
     * @param entity вид сущности, над которой проводилась оперция
     * @param type вид операции над сущностью
     * @param entityPath путь до сущности
     */
    private Operation(Entity entity, OperationType type, Path entityPath) {
        if (watcherRootPath == null)
            throw new RuntimeException("watcher.DirectoryWatcher not initialized");
        this.entity = entity;
        this.type = type;
        this.entityPath = excludeRootPath(entityPath);
    }

    /**
     * Для операций RENAME, MOVE_TO
     * @param entity сущность, над которой проводилась оперция
     * @param type вид операции над сущностью
     * @param oldPath прежний путь до сущности
     * @param newPath новый путь до сущности
     */
    private Operation(Entity entity, OperationType type, Path oldPath, Path newPath) {
        if (watcherRootPath == null)
            throw new RuntimeException("watcher.DirectoryWatcher not initialized");
        this.entity = entity;
        this.type = type;
        switch (this.type) {
            case RENAME -> {
                this.oldName = excludeRootPath(oldPath);
                this.newName = excludeRootPath(newPath);
            }
            case MOVE_TO -> {
                this.oldParentDirectory = excludeRootPath(oldPath);
                this.newParentDirectory = excludeRootPath(newPath);
            }
        }
    }

    /**
     * Устанавливает путь до корневой дирекории, на которую настроен watcher.DirectoryWatcher
     * @param path путь до корневой директории
     */
    public static void setWatcherRootPath(Path path) {
        watcherRootPath = path;
    }

    public static Operation createOperation(Entity entity, Path entityPath) {
        return new Operation(entity, OperationType.CREATE, entityPath);
    }

    public static Operation deleteOperation(Entity entity, Path entityPath) {
        return new Operation(entity, OperationType.DELETE, entityPath);
    }

    public static Operation modifyOperation(Entity entity, Path entityPath) {
        return new Operation(entity, OperationType.MODIFY, entityPath);
    }

    public static Operation renameOperation(Entity entity, Path oldPath, Path newPath) {
        return new Operation(entity, OperationType.RENAME, oldPath, newPath);
    }

    public static Operation moveToOperation(Entity entity, Path oldPath, Path newPath) {
        return new Operation(entity, OperationType.MOVE_TO, oldPath, newPath);
    }

    /**
     * Обрезает корневую директорию у переданного пути. Для удобства работы с путями на стороне сервера.
     * @param path путь до сущности
     * @return путь без корневой директории
     */
    private Path excludeRootPath(Path path) {
        return Path.of(path.toString().replace(Operation.watcherRootPath.toString(), ""));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name()).append(" ").append(entity.name()).append(" (");
        switch (this.type) {
            case RENAME -> sb.append("oldName = \"").append(oldName).append("\"")
                    .append(", newName = \"").append(newName).append("\"");
            case MOVE_TO -> sb.append("oldParentDirectory = \"").append(oldParentDirectory).append("\"")
                    .append(", newParentDirectory = \"").append(newParentDirectory).append("\"");
            default -> sb.append("entityPath = \"").append(entityPath).append("\"");
        }
        sb.append(")");
        return sb.toString();
    }

    enum OperationType {
        CREATE, DELETE, MODIFY, RENAME, MOVE_TO
    }

    enum Entity {
        DIRECTORY, FILE
    }
}


