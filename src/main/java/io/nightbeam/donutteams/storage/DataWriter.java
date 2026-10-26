package io.nightbeam.donutteams.storage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The one thread that talks to the database. Changes are made in memory first and saved here in the order they were
 * made, so a later save never lands before an earlier one. Shutting down waits for every queued save.
 */
public final class DataWriter {

    @FunctionalInterface
    public interface SqlTask {
        void run(TeamRepository repository) throws Exception;
    }

    @FunctionalInterface
    public interface SqlCall<T> {
        T call(TeamRepository repository) throws Exception;
    }

    private final TeamRepository repository;
    private final Logger logger;
    private final ExecutorService executor;

    public DataWriter(TeamRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "DonutTeams-Database");
            thread.setDaemon(true);
            return thread;
        });
    }

    public TeamRepository repository() {
        return repository;
    }

    /** Queues a save. Failures are logged; the in-memory state stays as it is. */
    public void write(String what, SqlTask task) {
        if (executor.isShutdown()) {
            runNow(what, task);
            return;
        }
        executor.execute(() -> runNow(what, task));
    }

    /** Runs a query on the database thread. */
    public <T> CompletableFuture<T> query(SqlCall<T> call) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable job = () -> {
            try {
                future.complete(call.call(repository));
            } catch (Throwable error) {
                future.completeExceptionally(error);
            }
        };
        if (executor.isShutdown()) {
            job.run();
        } else {
            executor.execute(job);
        }
        return future;
    }

    /** Waits for every queued save (up to 30 seconds), then stops the thread. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warning("Some team data was still being saved after 30 seconds.");
                executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void runNow(String what, SqlTask task) {
        try {
            task.run(repository);
        } catch (Throwable error) {
            logger.log(Level.SEVERE, "Could not save " + what, error);
        }
    }
}
