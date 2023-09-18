package net.bplearning.util.threading;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.bplearning.util.function.CheckedSupplier;

/**
 * This is a helper class for Java threading to
 * make typical tasks easier.  Most of the time
 * the infrastructure of the bulkier threading
 * frameworks just aren't needed in ordinary
 * circumstances.
 */
public class ThreadUtil {
	private static Consumer<Runnable> mainThreadRunner;
	/**
	 * Since different systems have different mechanisms
	 * for running on the main thread, this can be used
	 * to configure this library as to how your system
	 * should run something on the main thread.
	 * 
	 * For instance, to set up this code for Android,
	 * you would do <code>ThreadUtil.setMainThreadRunner((r) -&gt; new Handler(Looper.getMainLooper().post(r)));</code>
	 * @param callback
	 */
	public void setMainThreadRunner(Consumer<Runnable> callback) { mainThreadRunner = callback; }

	/**
	 * This function starts a thread.  I've always
	 * thought that try to type <code>new Thread(() -&gt; blah).start()</code>
	 * was unintuitive, so this does both.
	 */
	public static Thread start(Runnable r) {
		Thread thread = new Thread(r);
		thread.start();
		return thread;
	}

	/**
	 * Most of the time when sleeping you aren't concerned
	 * with the InterruptedException.  This is the sleep
	 * without the exception (but note that if you are
	 * interrupted it will just end early).
	 * @param millis
	 */
	public static void easySleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException e) {
			// Ignore
		}
	}

	/**
	 * Runs the given runnable on the main thread.
	 * @param r
	 */
	public static void runOnMainThread(Runnable r) {
		mainThreadRunner.accept(r);
	}

	/**
	 * Combines sleeping and running in a separate thread.
	 * @param millis
	 * @param r
	 */
	public static void runAfterDelay(long millis, Runnable r) {
		start(() -> {
			easySleep(millis);
			r.run();
		});
	}

	/**
	 * Combines sleeping and running later on the main thread.
	 * @param millis
	 * @param r
	 */
	public static void runOnMainThreadAfterDelay(long millis, Runnable r) {
		start(() -> {
			easySleep(millis);
			runOnMainThread(r);
		});
	}

	/**
	 * Minimal replacement for AsyncTask.  Basically,
	 * allows a long-running call to be backgrounded
	 * and then the results delivered on the main
	 * thread easiy.
	 * @param <T>
	 * @param backgroundSupplierTask
	 * @param mainThreadCallback
	 */
	public static <T> void backgroundForMainThreadResult(Supplier<T> backgroundSupplierTask, Consumer<T> mainThreadCallback) {
		start(() -> {
			T result = backgroundSupplierTask.get();
			runOnMainThread(() -> {
				mainThreadCallback.accept(result);
			});
		});
	}

	/**
	 * Runs a task in the background that produces a result, but may also produce and exception.
	 * Delivers both the result and the exception to a callback in the main thread.
	 * Note that the callback can take any throwable.  This is for many reasons, including
	 * (a) what happens if we get a RuntimeException, and (b) the difficulty in handling inferred
	 * types for catches at runtime anyway.
	 * 
	 * @param <T> the type of the main data returned by supplier.
	 * @param <E> the type of exception signature the supplier uses
	 * @param backgroundSupplierTask the actual supplying task
	 * @param mainThreadCallback the callback to be called in the main thread.
	 */
	public static <T, E extends Throwable> void backgroundForMainThreadResultOrException(CheckedSupplier<T, E> backgroundSupplierTask, BiConsumer<T, Throwable> mainThreadCallback) {
		start(() -> {
			Throwable exc = null;
			T result = null;
			try {
				result = backgroundSupplierTask.get();
			} catch(Throwable e) {
				exc = e;
			}

			final T finalResult = result;
			final Throwable finalException = exc;

			runOnMainThread(() -> {
				mainThreadCallback.accept(finalResult, finalException);
			});
		});
	}

	/**
	 * Runs a task in the background that produces a result, but may also produce and exception.
	 * Delivers both the result and the exception to a callback.
	 * Note that the callback can take any throwable.  This is for many reasons, including
	 * (a) what happens if we get a RuntimeException, and (b) the difficulty in handling inferred
	 * types for catches at runtime anyway.
	 * 
	 * @param <T> the type of the main data returned by supplier.
	 * @param <E> the type of exception signature the supplier uses
	 * @param backgroundSupplierTask the actual supplying task
	 * @param callback the callback to be called.
	 */
	public static <T, E extends Throwable> void backgroundForResultOrException(CheckedSupplier<T, E> backgroundSupplierTask, BiConsumer<T, Throwable> callback) {
		start(() -> {
			Throwable exc = null;
			T result = null;
			try {
				result = backgroundSupplierTask.get();
			} catch(Throwable e) {
				exc = e;
			}

			callback.accept(result, exc);
		});
	}
}
