package utils;

import snapshot.Directory;
import snapshot.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

/**
 * Вспомогательный класс для расчета хеш-суммы файла с помощью алгоритма CRC32
 */
public class CRC32Hash {
    /**
     * Предельный размер файла, который будет загружен в память полностью, без открытия IO stream'а.
     */
    private static final int THRESHOLD_SIZE = 0x02000000; //(32 MB)
    /**
     * Размер буффера для IO stream'а.
     */
    private static final int BUFFER_SIZE = 0x00040000; //(256 KB)

    /**
     * Расчитывает значение хеш-суммы для переданного файла.
     * @param path путь до файла
     * @return хеш-сумма файла
     * @throws IOException если такого файла не существует
     */
    public static long calculateCrc32Hash(Path path) throws IOException {
        CRC32 crc = new CRC32();
        if (Files.size(path) < THRESHOLD_SIZE) {
            crc.update(Files.readAllBytes(path));
        } else {
            try (InputStream is = new FileInputStream(path.toFile())) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    crc.update(buffer, 0, bytesRead);
                }
            }
        }
        return crc.getValue();
    }


    public static long calcXorHashSum(Directory startDirectory) {
        return calcXorHashSum0(startDirectory, 0L);
    }

    private static long calcXorHashSum0(Directory currDirectory, long currValue) {
        long result = currValue;
        for (Directory dir : currDirectory.getSubdirectories()) {
            result ^= calcXorHashSum0(dir, result);
        }
        for (File f : currDirectory.getFiles()) {
            result ^= f.getCrc32Hash();
        }
        return result;
    }
}
