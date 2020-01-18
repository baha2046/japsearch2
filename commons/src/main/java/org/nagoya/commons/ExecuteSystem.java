package org.nagoya.commons;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.*;

public class ExecuteSystem {
    private final static ExecuteSystem instance = new ExecuteSystem();

    @Contract(pure = true)
    public static ExecuteSystem get() {
        return ExecuteSystem.getInstance();
    }

    public static Future<?> useExecutors(Runnable run) {
        return useExecutors(ExecuteSystem.role.NORMAL, run);
    }

    public static Future<?> usePriorityExecutors(Runnable run) {
        return useExecutors(ExecuteSystem.role.IO, run);
    }

    public static Future<?> useExecutors(ExecuteSystem.role role, Runnable run) {
        return ExecuteSystem.getInstance().useExecutor(run, role);
    }

    public static ExecutorService getExecutorServices() {
        return getExecutorServices(ExecuteSystem.role.NORMAL);
    }

    public static ExecutorService getExecutorServices(ExecuteSystem.role role) {
        return ExecuteSystem.getInstance().getService(role);
    }

    private final ExecutorService normalExecutor;
    private final ExecutorService fileIOExecutor;
    private final ExecutorService movieExecutor;
    private final ExecutorService eventExecutor;
    private final ExecutorService imageExecutor;
    private final ExecutorService fileSystemExecutor;

    private final Scheduler ioScheduler;
    private final Scheduler normalScheduler;
    private final Scheduler fileSystemScheduler;

    private ExecuteSystem() {
        this.fileIOExecutor =
                Executors.newSingleThreadExecutor(this.makeThreadFactory(Thread.MAX_PRIORITY));
        this.fileSystemExecutor =
                new ThreadPoolExecutor(1, 4, 1L, TimeUnit.MINUTES, new ArrayBlockingQueue<>(80), new ThreadPoolExecutor.DiscardOldestPolicy());
        this.movieExecutor =
                new ThreadPoolExecutor(1, 1, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
        this.eventExecutor =
                new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.imageExecutor =
                new ThreadPoolExecutor(1, 2, 1L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.normalExecutor =
                Executors.newWorkStealingPool();

        this.fileSystemScheduler = Schedulers.fromExecutor(this.fileSystemExecutor);
        this.ioScheduler = Schedulers.fromExecutor(this.fileIOExecutor);
        this.normalScheduler = Schedulers.fromExecutor(this.normalExecutor);
        //this.movieExecutor = this.normalExecutor;
        //this.imageExecutor = this.normalExecutor;
    }

    public void fsInvoke(Runnable runnable) {
        try {
            this.fileSystemExecutor.invokeAll(List.of(() -> {
                runnable.run();
                return true;
            }), 2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static ExecuteSystem getInstance() {
        return instance;
    }

    private ThreadFactory makeThreadFactory(final int priority) {
        return r -> {
            Thread thread = new Thread(r);
            thread.setPriority(priority);
            return thread;
        };
    }

    public Scheduler getIoScheduler() {
        return this.ioScheduler;
    }

    public Scheduler getNormalScheduler() {
        return this.normalScheduler;
    }

    public Scheduler getFileSystemScheduler() {
        return this.fileSystemScheduler;
    }

    public Future<?> useExecutor(Runnable runnable, role role) {
        switch (role) {
            case EVENT:
                return this.eventExecutor.submit(runnable);
            case IO:
                return this.fileIOExecutor.submit(runnable);
            case MOVIE:
                return this.movieExecutor.submit(runnable);
            case IMAGE:
                return this.imageExecutor.submit(runnable);

            default:
                return this.normalExecutor.submit(runnable);
        }
    }

    public <T> Future<T> useExecutor(Callable<T> callable, @NotNull role role) {
        switch (role) {
            case EVENT:
                return this.eventExecutor.submit(callable);
            case IO:
                return this.fileIOExecutor.submit(callable);
            case MOVIE:
                return this.movieExecutor.submit(callable);
            case IMAGE:
                return this.imageExecutor.submit(callable);

            default:
                return this.normalExecutor.submit(callable);
        }
    }

    ExecutorService getService(role role) {
        switch (role) {
            case EVENT:
                return this.eventExecutor;
            case IO:
                return this.fileIOExecutor;
            case MOVIE:
                return this.movieExecutor;
            case IMAGE:
                return this.imageExecutor;

            default:
                return this.normalExecutor;
        }
    }

    public enum role {
        NORMAL, IO, EVENT, MOVIE, IMAGE, FILESYSTEM
    }

    public void shutdown() {
        this.normalExecutor.shutdown();
        this.fileIOExecutor.shutdown();
        this.movieExecutor.shutdown();
        this.imageExecutor.shutdown();
        this.eventExecutor.shutdown();
        this.fileSystemExecutor.shutdown();

        try {
            this.normalExecutor.awaitTermination(10, TimeUnit.SECONDS);
            this.fileIOExecutor.awaitTermination(10, TimeUnit.SECONDS);
            this.movieExecutor.awaitTermination(10, TimeUnit.SECONDS);
            this.imageExecutor.awaitTermination(10, TimeUnit.SECONDS);
            this.eventExecutor.awaitTermination(10, TimeUnit.SECONDS);
            this.fileSystemExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.normalExecutor.shutdownNow();
        this.fileIOExecutor.shutdownNow();
        this.movieExecutor.shutdownNow();
        this.imageExecutor.shutdownNow();
        this.eventExecutor.shutdownNow();
        this.fileSystemExecutor.shutdownNow();
    }
}
