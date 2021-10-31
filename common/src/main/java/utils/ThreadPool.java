package utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    private static final ExecutorService POOL = Executors.newCachedThreadPool();

    public static void addTask(Runnable task) {
        POOL.execute(task);
    }

    public static void shutdown() {
        POOL.shutdownNow();
    }
}
