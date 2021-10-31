package files;

import com.fasterxml.jackson.annotation.JsonIgnore;
import utils.CRC32Hash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

public class SimpleFile implements FileSystemElement {

    private String path;
    private String name;
    private long size;
    private long crc32Hash;

    public SimpleFile() {
    }

    public SimpleFile(Path path) throws IOException {
        this.path = path.getParent().toString();
        this.name = path.getFileName().toString();
        BasicFileAttributes bfa = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
        this.size = bfa.size();
        crc32Hash = CRC32Hash.calculateCrc32Hash(path);
    }

    public boolean compare(SimpleFile anotherFile) {
        return this.crc32Hash == anotherFile.crc32Hash && compareBySizeAndExtension(anotherFile);
    }

    public boolean compareBySizeAndExtension(SimpleFile anotherFile) {
        return this.size == anotherFile.size
                && this.getFileExtension().equalsIgnoreCase(anotherFile.getFileExtension());
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public String getFileExtension() {
        String ext = "";
        if (name.contains(".")) ext = name.substring(name.lastIndexOf(".") + 1);
        return ext;
    }

    @JsonIgnore
    public String getAbsolutePath() {
        return path + "\\" + name;
    }

    public long getSize() {
        return size;
    }

    public long getCrc32Hash() {
        return crc32Hash;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setCrc32Hash(long crc32Hash) {
        this.crc32Hash = crc32Hash;
    }

    @Override
    public String toString() {
        return "File{" +
                "path='" + path + '\'' +
                ", filename='" + name + '\'' +
                ", size=" + size +
                ", hashSum=" + crc32Hash +
                '}';
    }
}
