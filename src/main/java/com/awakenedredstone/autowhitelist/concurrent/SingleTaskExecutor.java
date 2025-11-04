package com.awakenedredstone.autowhitelist.concurrent;

import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.*;

public class SingleTaskExecutor<T extends Runnable> {
    private final ExecutorService executor;
    private volatile Queue<T> tasks = new ConcurrentLinkedQueue<>();

    public SingleTaskExecutor() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    public SingleTaskExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public SingleTaskExecutor(ThreadFactory threadFactory) {
        this.executor = Executors.newSingleThreadExecutor(threadFactory);
    }
    
    public synchronized void submit(T task) {
        tasks.offer(task);

        executor.submit(() -> {
            try {
                task.run();
            } finally {
                tasks.poll();
            }
        });
    }
    
    public @Nullable T getCurrentTask() {
        return tasks.peek();
    }

    public int getPendingTaskCount() {
        return Math.max(0, tasks.size() - 1);
    }

    public void shutdown() {
        executor.shutdown();
    }
}