package net.bplearning.util.function;

/**
 * This interface allows for supplier calls that may
 * throw exceptions.
 */
public interface CheckedSupplier<T, E extends Throwable> {
	public T get() throws E;
}

