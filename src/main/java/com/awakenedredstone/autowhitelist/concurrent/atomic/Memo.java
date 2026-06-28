package com.awakenedredstone.autowhitelist.concurrent.atomic;

import java.util.function.Supplier;

public final class Memo<T> implements Supplier<T> {
    private final transient Object lock = new Object();

    private final Supplier<T> delegate;
    private transient volatile boolean initialized;
    private transient T value;

    public Memo(Supplier<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T get() {
        if (!initialized) {
            synchronized (lock) {
                if (!initialized) {
                    T value = delegate.get();
                    this.value = value;
                    initialized = true;
                    return value;
                }
            }
        }

        return value;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void reset() {
        synchronized (lock) {
            initialized = false;
            value = null;
        }
    }
}
