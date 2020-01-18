package org.nagoya.video.player;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Originally contributed by Jason Pollastrini, with changes.
 */
public abstract class NanoTimer extends ScheduledService<Void> {

    private final long ONE_NANO = 1000000000L;
    private final double ONE_NANO_INV = 1f / 1000000000L;

    private long startTime;
    private long previousTime;

    private double frameRate;
    private double deltaTime;

    public NanoTimer(double period) {
        super();
        this.setPeriod(Duration.millis(period));
        this.setExecutor(Executors.newCachedThreadPool(new NanoThreadFactory()));
    }

    public final long getTime() {
        return System.nanoTime() - this.startTime;
    }

    public final double getTimeAsSeconds() {
        return this.getTime() * this.ONE_NANO_INV;
    }

    public final double getDeltaTime() {
        return this.deltaTime;
    }

    public final double getFrameRate() {
        return this.frameRate;
    }

    @Override
    public final void start() {
        super.start();
        if (this.startTime <= 0) {
            this.startTime = System.nanoTime();
        }
    }

    @Override
    public final void reset() {
        super.reset();
        this.startTime = System.nanoTime();
        this.previousTime = this.getTime();
    }

    @NotNull
    @Contract(value = " -> new", pure = true)
    @Override
    protected final Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                NanoTimer.this.updateTimer();
                return null;
            }
        };
    }

    private void updateTimer() {
        this.deltaTime = (this.getTime() - this.previousTime) * (1.0f / this.ONE_NANO);
        this.frameRate = 1.0f / this.deltaTime;
        this.previousTime = this.getTime();
    }

    @Override
    protected final void succeeded() {
        super.succeeded();
        this.onSucceeded();
    }

    @Override
    protected final void failed() {
        this.getException().printStackTrace(System.err);
        this.onFailed();
    }

    private static class NanoThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "NanoTimerThread");
            thread.setPriority(Thread.NORM_PRIORITY + 1);
            thread.setDaemon(true);
            return thread;
        }
    }

    /**
     *
     */
    protected abstract void onSucceeded();

    /**
     *
     */
    protected void onFailed() {
    }
}