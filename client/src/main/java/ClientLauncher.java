import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClientLauncher {
    private static int filesCount = 0;
    public static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                filesCount++;
                length += file.length();
            }
            else
                length += folderSize(file);
        }
        return length;
    }
    public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException {
        Path source = Path.of("C:\\Users\\Bananza_mini\\Downloads");
        Directory dir = new Directory("generateFiles", source);
        System.out.println(dir.getPath());

        System.out.println(new Date(Files.getFileAttributeView(dir.getPath(), BasicFileAttributeView.class).readAttributes().creationTime().toMillis()));
        FileTreeSnapshot fts = new FileTreeSnapshot(Path.of("C:\\Users\\Bananza_mini\\CloudStorageGBProject\\ClientDir"));
        Directory d = fts.getInitialDirectory();
        System.out.println("Init dir: " + d.getPath());
        fts.createSnapshot();
        System.out.println();
        fts.printFileTree();
//        List<File> list = new ArrayList<>();
//        for (int i = 0; i < 20; i++) {
//            File f  = new File(source + "\\folder_" + i);
//            list.add(f);
//            f.mkdir();
//        }
//        for (File file : list) {
//            System.out.println("For folder " + file.getName() + " lastModifyTime = " + file.lastModified());
//        }

//        int filesNumber = 32;
//        Path p = Path.of("C:\\Program Files (x86)\\Steam");
//        Path p1 = Path.of("C:\\Users\\Bananza_mini\\Downloads\\generateFiles1");
//        for (int i = 1; i <= filesNumber; i++) {
//            System.out.println(new File(p + "\\file_" + i + ".txt").createNewFile());
//        }
//        File f = p.toFile();
//        long start = System.currentTimeMillis();
//        long size = folderSize(p.toFile());
//        long stop = System.currentTimeMillis();
//        System.out.println("Total files: " + filesCount);
//        System.out.println("Size = " + size + ", time = " + (stop - start) + " ms");
        //System.out.println("getBLockSize: " + Files.getFileStore(p).getBlockSize());
        //System.out.println("getTotalSpace: " + Files.getFileStore(p).getTotalSpace());
        //System.out.println("size: " + Files.size(p));
//
//        System.out.println(p.toFile().hashCode());
//        System.out.println(p.toFile().hashCode());
//        Files.createDirectories(p1);
//        Path newPath = Files.copy(p,p1.resolve(p.getFileName()));
//        System.out.println(newPath.toFile().hashCode());

//        Path p = Path.of("C:\\Users\\Bananza_mini\\Downloads\\Amouranth1.mp4");
//        byte[] b = Files.readAllBytes(p);
//        long start = System.currentTimeMillis();
//        System.out.println("MD5: " + DigestUtils.md5Hex(b));
//        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");
//        start = System.currentTimeMillis();
//        System.out.println("SHA1: " + DigestUtils.sha1Hex(b));
//        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");
//        start = System.currentTimeMillis();
//        System.out.println("SHA256: " + DigestUtils.sha256Hex(b));
//        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");
//        start = System.currentTimeMillis();
//        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");
//        System.out.println("Size: " + (Files.size(p)) + " B");
//        System.out.println("Last modified: " + new Date(Files.getLastModifiedTime(p).toMillis()));

//        p = Path.of("C:\\Users\\Bananza_mini\\Downloads\\file2.txt");
//        start = System.currentTimeMillis();
//        b = Files.readAllBytes(p);
//        System.out.println("MD5: " + DigestUtils.md5Hex(b));
//        System.out.println("SHA1: " + DigestUtils.sha1Hex(b));
//        System.out.println("SHA256: " + DigestUtils.sha256Hex(b));
//        System.out.println("Time: " + (System.currentTimeMillis() - start) + " ms");
//        System.out.println("Size: " + (Files.size(p)) + " B");
//        System.out.println("Last modified: " + new Date(Files.getLastModifiedTime(p).toMillis()));
//        new DirectoryWatcher("C:\\Users\\Bananza_mini\\CloudStorageGBProject\\ClientDir").start();
//        new Thread(() -> {
//            try {
//                new Client("C:\\Users\\Bananza_mini\\CloudStorageGBProject\\ClientDir").start();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
//        Thread.sleep(1000);
//        new File("C:\\Users\\Bananza_mini\\CloudStorageGBProject\\ClientDir\\dir1").mkdir();
//        Thread.sleep(1000);
//        new File("C:\\Users\\Bananza_mini\\CloudStorageGBProject\\ClientDir\\dir1\\dir2").mkdir();
//        Thread.sleep(1000);
//        new File("C:\\Users\\Bananza_mini\\CloudStorageGBProject\\ClientDir\\dir1\\dir2\\dir3").mkdir();

    }
}
