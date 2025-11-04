package com.awakenedredstone.autowhitelist.concurrent.atomic;

import java.io.Serial;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * An object reference that may be updated atomically. See the {@link
 * VarHandle} specification for descriptions of the properties of
 * atomic accesses.
 * <br/>
 * A lock may be enabled to prevent modifications of the reference.
 * <br/>
 * Please don't use this as an atomic reference, this is here just
 * so I can use it as a late final field
 *
 * @param <V> The type of object referred to by this reference
 * @author Doug Lea
 * @since 1.5
 */
public class LockingAtomicReference<V> implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = -854442909835293183L;
    private static final VarHandle VALUE;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(LockingAtomicReference.class, "value", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // Conditionally serializable
    private volatile V value;
    private volatile boolean locked = false;

    /**
     * Creates a new AtomicReference with the given initial value.
     *
     * @param initialValue the initial value
     */
    public LockingAtomicReference(V initialValue) {
        value = initialValue;
    }

    /**
     * Creates a new AtomicReference with null initial value.
     */
    public LockingAtomicReference() {
    }

    /**
     * Lock the atomic reference, with memory effects as specified by {@link VarHandle#setVolatile}.
     * <br/>
     * Once locked the reference can no longer be modified not unlocked.
     */
    public final void lock() {
        locked = true;
    }

    /**
     * Get if the atomic reference is currently locked, with memory effects as specified by {@link VarHandle#getVolatile}.
     *
     * @return the lock state
     */
    public final boolean isLocked() {
        return locked;
    }

    /**
     * Returns the current value,
     * with memory effects as specified by {@link VarHandle#getVolatile}.
     *
     * @return the current value
     */
    public final V get() {
        return value;
    }

    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setVolatile}.
     *
     * @param newValue the new value
     */
    public final void set(V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        value = newValue;
    }

    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setVolatile},
     * and locks the reference immediately after.
     *
     * @param newValue the new value
     */
    public final void setAndLock(V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        value = newValue;
        lock();
    }

    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setRelease}.
     *
     * @param newValue the new value
     * @since 1.6
     */
    public final void lazySet(V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        VALUE.setRelease(this, newValue);
    }

    /**
     * Atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by {@link VarHandle#compareAndSet}.
     *
     * @param expectedValue the expected value
     * @param newValue      the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet(V expectedValue, V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return VALUE.compareAndSet(this, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by {@link VarHandle#weakCompareAndSetPlain}.
     *
     * @param expectedValue the expected value
     * @param newValue      the new value
     * @return {@code true} if successful
     * @see #weakCompareAndSetPlain
     * @deprecated This method has plain memory effects but the method
     * name implies volatile memory effects (see methods such as
     * {@link #compareAndExchange} and {@link #compareAndSet}).  To avoid
     * confusion over plain or volatile memory effects it is recommended that
     * the method {@link #weakCompareAndSetPlain} be used instead.
     */
    @Deprecated(since = "9")
    public final boolean weakCompareAndSet(V expectedValue, V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return VALUE.weakCompareAndSetPlain(this, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by {@link VarHandle#weakCompareAndSetPlain}.
     *
     * @param expectedValue the expected value
     * @param newValue      the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetPlain(V expectedValue, V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return VALUE.weakCompareAndSetPlain(this, expectedValue, newValue);
    }

    /**
     * Atomically sets the value to {@code newValue} and returns the old value,
     * with memory effects as specified by {@link VarHandle#getAndSet}.
     *
     * @param newValue the new value
     * @return the previous value
     */
    @SuppressWarnings("unchecked")
    public final V getAndSet(V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return (V) VALUE.getAndSet(this, newValue);
    }

    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the current value with the results of
     * applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    public final V getAndUpdate(UnaryOperator<V> updateFunction) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        V prev = get(), next = null;
        for (boolean haveNext = false; ; ) {
            if (!haveNext)
                next = updateFunction.apply(prev);
            if (weakCompareAndSetVolatile(prev, next))
                return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the current value with the results of
     * applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    public final V updateAndGet(UnaryOperator<V> updateFunction) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        V prev = get(), next = null;
        for (boolean haveNext = false; ; ) {
            if (!haveNext)
                next = updateFunction.apply(prev);
            if (weakCompareAndSetVolatile(prev, next))
                return next;
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the current value with the results of
     * applying the given function to the current and given values,
     * returning the previous value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function is
     * applied with the current value as its first argument, and the
     * given update as the second argument.
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        V prev = get(), next = null;
        for (boolean haveNext = false; ; ) {
            if (!haveNext)
                next = accumulatorFunction.apply(prev, x);
            if (weakCompareAndSetVolatile(prev, next))
                return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * Atomically updates (with memory effects as specified by {@link
     * VarHandle#compareAndSet}) the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function is
     * applied with the current value as its first argument, and the
     * given update as the second argument.
     *
     * @param x                   the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        V prev = get(), next = null;
        for (boolean haveNext = false; ; ) {
            if (!haveNext)
                next = accumulatorFunction.apply(prev, x);
            if (weakCompareAndSetVolatile(prev, next))
                return next;
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString() {
        return String.valueOf(get());
    }

    // jdk9

    /**
     * Returns the current value, with memory semantics of reading as
     * if the variable was declared non-{@code volatile}.
     *
     * @return the value
     * @since 9
     */
    public final V getPlain() {
        return (V) VALUE.get(this);
    }

    /**
     * Sets the value to {@code newValue}, with memory semantics
     * of setting as if the variable was declared non-{@code volatile}
     * and non-{@code final}.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setPlain(V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        VALUE.set(this, newValue);
    }

    /**
     * Returns the current value,
     * with memory effects as specified by {@link VarHandle#getOpaque}.
     *
     * @return the value
     * @since 9
     */
    public final V getOpaque() {
        return (V) VALUE.getOpaque(this);
    }

    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setOpaque}.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setOpaque(V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        VALUE.setOpaque(this, newValue);
    }

    /**
     * Returns the current value,
     * with memory effects as specified by {@link VarHandle#getAcquire}.
     *
     * @return the value
     * @since 9
     */
    public final V getAcquire() {
        return (V) VALUE.getAcquire(this);
    }

    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setRelease}.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setRelease(V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        VALUE.setRelease(this, newValue);
    }

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#compareAndExchange}.
     *
     * @param expectedValue the expected value
     * @param newValue      the new value
     * @return the <em>witness value</em>, which will be the same as the
     * expected value if successful
     * @since 9
     */
    public final V compareAndExchange(V expectedValue, V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return (V) VALUE.compareAndExchange(this, expectedValue, newValue);
    }

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#compareAndExchangeAcquire}.
     *
     * @param expectedValue the expected value
     * @param newValue      the new value
     * @return the <em>witness value</em>, which will be the same as the
     * expected value if successful
     * @since 9
     */
    public final V compareAndExchangeAcquire(V expectedValue, V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return (V) VALUE.compareAndExchangeAcquire(this, expectedValue, newValue);
    }

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#compareAndExchangeRelease}.
     *
     * @param expectedValue the expected value
     * @param newValue      the new value
     * @return the <em>witness value</em>, which will be the same as the
     * expected value if successful
     * @since 9
     */
    public final V compareAndExchangeRelease(V expectedValue, V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return (V) VALUE.compareAndExchangeRelease(this, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#weakCompareAndSet}.
     *
     * @param expectedValue the expected value
     * @param newValue      the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetVolatile(V expectedValue, V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return VALUE.weakCompareAndSet(this, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#weakCompareAndSetAcquire}.
     *
     * @param expectedValue the expected value
     * @param newValue      the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetAcquire(V expectedValue, V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return VALUE.weakCompareAndSetAcquire(this, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#weakCompareAndSetRelease}.
     *
     * @param expectedValue the expected value
     * @param newValue      the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetRelease(V expectedValue, V newValue) {
        if (locked) throw new IllegalStateException("Reference is locked!");
        return VALUE.weakCompareAndSetRelease(this, expectedValue, newValue);
    }
}
