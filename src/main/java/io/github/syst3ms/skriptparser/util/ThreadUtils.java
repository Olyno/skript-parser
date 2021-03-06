package io.github.syst3ms.skriptparser.util;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {

	/**
	 * Run certain code once on a separate thread
	 * @param code the runnable that needs to be executed
	 */
	public static void runAsync(Runnable code) {
		ExecutorService executor = Executors.newCachedThreadPool();
		executor.submit(code);
		executor.shutdown();
	}

	/**
	 * Run certain code once after a certain delay.
	 * @param code the runnable that needs to be executed
	 * @param duration the delay
	 */
	public static void runAfter(Runnable code, Duration duration) {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.schedule(code, duration.toMillis(), TimeUnit.MILLISECONDS);
		executor.shutdown();
	}

	/**
	 * Runs certain code periodically.
	 * @param code the runnable that needs to be executed
	 * @param duration the delay
	 */
	public static void runPeriodically(Runnable code, Duration duration) {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(code, duration.toMillis(), duration.toMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Runs certain code periodically.
	 * @param code the runnable that needs to be executed
	 * @param initialDelay the initial delay
	 * @param duration the delay
	 */
	public static void runPeriodically(Runnable code, Duration initialDelay, Duration duration) {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(code, initialDelay.toMillis(), duration.toMillis(), TimeUnit.MILLISECONDS);
	}
}
