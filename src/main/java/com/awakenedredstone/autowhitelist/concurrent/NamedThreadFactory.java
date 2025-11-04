package com.awakenedredstone.autowhitelist.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class NamedThreadFactory implements ThreadFactory {
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final Function<Integer, String> nameCreator;
    private final Consumer<Thread> configurer;

    public NamedThreadFactory(Function<Integer, String> nameCreator, Consumer<Thread> configurer) {
        this.group = Thread.currentThread().getThreadGroup();
        this.nameCreator = nameCreator;
        this.configurer = configurer;
    }

    @Override
    public Thread newThread(@NotNull Runnable task) {
        Thread thread = new Thread(group, task, nameCreator.apply(threadNumber.getAndIncrement()), 0);
        if (thread.getPriority() != Thread.NORM_PRIORITY) thread.setPriority(Thread.NORM_PRIORITY);

        configurer.accept(thread);
        return thread;
    }
}
