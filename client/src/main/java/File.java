import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class File implements Comparable<File> {
    private String name;
    private Path parentPath;
    private Directory parentDirectory;
    private FileTime lastModified;
    private long size;
    private String hashSum;

    public File(String name, Path parentPath) {
        this.name = name;
        this.parentPath = parentPath;
    }

    String getName() {
        return this.name;
    }

    @Override
    public int compareTo(File anotherFile) {
        return this.name.compareTo(anotherFile.name);
    }
}
