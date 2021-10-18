package utils;

import snapshot.Directory;
import snapshot.File;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.zip.CRC32;

/**
 * Вспомогательный класс для расчета хеш-суммы файла с помощью алгоритма CRC32
 */
public class CRC32Hash {

    /**
     * Размер буффера для IO stream'а.
     */
    private static final int BUFFER_SIZE = 0x00040000; //(256 KB)

    /**
     * Расчитывает значение хеш-суммы для переданного файла.
     *
     * @param path путь до файла
     * @return хеш-сумма файла
     */
    public static long calculateCrc32Hash(Path path) {
        CRC32 crc = new CRC32();
        try {
            @SuppressWarnings("resource")
            FileChannel channel = new RandomAccessFile(path.toString(), "rw").getChannel();
            FileLock lock = null;
            try {
                lock = channel.lock();
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = channel.read(ByteBuffer.wrap(buffer))) != -1) {
                    crc.update(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                //do nothing
            }
            if (lock != null) lock.release();
            channel.close();
        } catch (Exception e) {
            //do nothing
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
