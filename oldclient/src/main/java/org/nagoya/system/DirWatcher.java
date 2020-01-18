package org.nagoya.system;

import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nagoya.commons.ExecuteSystem;
import org.reactfx.EventSource;
import org.reactfx.EventStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.*;
import static java.nio.file.StandardWatchEventKinds.*;

class DirWatcher {
    private final EventSource<WatchKey> signalledKeys = new EventSource<>();
    private final EventSource<Throwable> errors = new EventSource<>();
    private final Option<WatchService> watcher;

    private final Executor eventThreadExecutor;

    private final BlockingQueue<Runnable> executorQueue = new LinkedBlockingQueue<>();
    private final Thread ioThread;

    private volatile boolean shutdown = false;
    private boolean mayInterrupt = false;
    private boolean interrupted = false;


    public DirWatcher() {
        this.watcher = Try.of(() -> FileSystems.getDefault().newWatchService()).onFailure(Throwable::printStackTrace).toOption();

        this.eventThreadExecutor = ExecuteSystem.getExecutorServices(ExecuteSystem.role.EVENT);

        this.ioThread = new Thread(this::loop, "DirWatchIO");
        this.ioThread.setDaemon(true);
        this.ioThread.start();
    }

    public void shutdown() {
        this.shutdown = true;
        this.interrupt();
    }

    public EventStream<WatchKey> signalledKeys() {
        return this.signalledKeys;
    }

    public EventStream<Throwable> errors() {
        return this.errors;
    }

    public void watch(Path dir) throws IOException {
        if (this.watcher.isDefined()) {
            dir.register(this.watcher.get(), ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        }
    }

    public void watchOrLogError(Path dir) {
        try {
            this.watch(dir);
        } catch (IOException e) {
            this.errors.push(e);
        }
    }

    public CompletionStage<PathNode> getTree(Path root) {
        CompletableFuture<PathNode> res = new CompletableFuture<>();
        this.executeOnIOThread(() -> {
            try {
                res.complete(PathNode.getTree(root));
            } catch (IOException e) {
                res.completeExceptionally(e);
            }
        });
        return res;
    }

    public void createFile(Path file, Consumer<FileTime> onSuccess, Consumer<Throwable> onError) {
        this.executeIOOperation(() -> this.createFile(file), onSuccess, onError);
    }

    public void createDirectory(Path dir, Runnable onSuccess, Consumer<Throwable> onError) {
        this.executeIOOperation(
                () -> {
                    Files.createDirectory(dir);
                    return null;
                },
                none -> onSuccess.run(),
                onError);
    }

    public void saveTextFile(Path file, String content, Charset charset,
                             Consumer<FileTime> onSuccess, Consumer<Throwable> onError) {
        this.executeIOOperation(
                () -> this.writeTextFile(file, content, charset),
                onSuccess,
                onError);
    }

    public void saveBinaryFile(Path file, byte[] content,
                               Consumer<FileTime> onSuccess, Consumer<Throwable> onError) {
        this.executeIOOperation(
                () -> this.writeBinaryFile(file, content),
                onSuccess,
                onError);
    }

    public void deleteFileOrEmptyDirectory(Path fileOrDir,
                                           Runnable onSuccess, Consumer<Throwable> onError) {
        this.executeIOOperation(
                () -> {
                    Files.deleteIfExists(fileOrDir);
                    return null;
                },
                NULL -> onSuccess.run(),
                onError);
    }

    public void deleteTree(Path root,
                           Runnable onSuccess, Consumer<Throwable> onError) {
        this.executeIOOperation(
                () -> {
                    if (Files.exists(root)) {
                        this.deleteRecursively(root);
                    }
                    return null;
                },
                NULL -> onSuccess.run(),
                onError);
    }

    public void loadBinaryFile(Path file,
                               Consumer<byte[]> onSuccess, Consumer<Throwable> onError) {
        this.executeIOOperation(
                () -> Files.readAllBytes(file),
                onSuccess,
                onError);
    }

    public void loadTextFile(Path file, Charset charset,
                             Consumer<String> onSuccess, Consumer<Throwable> onError) {
        this.executeIOOperation(
                () -> this.readTextFile(file, charset),
                onSuccess,
                onError);
    }

    private <T> void executeIOOperation(Callable<T> action,
                                        Consumer<T> onSuccess, Consumer<Throwable> onError) {
        this.executeOnIOThread(() -> {
            try {
                T res = action.call();
                this.executeOnEventThread(() -> onSuccess.accept(res));
            } catch (Throwable t) {
                this.executeOnEventThread(() -> onError.accept(t));
            }
        });
    }

    @NotNull
    private FileTime createFile(Path file) throws IOException {
        Files.createFile(file);
        return Files.getLastModifiedTime(file);
    }

    private void deleteRecursively(Path root) throws IOException {
        if (Files.isDirectory(root)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                for (Path path : stream) {
                    this.deleteRecursively(path);
                }
            }
        }
        Files.delete(root);
    }

    @NotNull
    private FileTime writeBinaryFile(Path file, byte[] content) throws IOException {
        Files.write(file, content, CREATE, WRITE, TRUNCATE_EXISTING);
        return Files.getLastModifiedTime(file);
    }

    @NotNull
    private FileTime writeTextFile(Path file, @NotNull String content, Charset charset) throws IOException {
        byte[] bytes = content.getBytes(charset);
        return this.writeBinaryFile(file, bytes);
    }

    @NotNull
    private String readTextFile(Path file, @NotNull Charset charset) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharBuffer chars = charset.decode(ByteBuffer.wrap(bytes));
        return chars.toString();
    }

    private void executeOnIOThread(Runnable action) {
        this.executorQueue.add(action);
        this.interrupt();
    }

    private void executeOnEventThread(Runnable action) {
        this.eventThreadExecutor.execute(action);
    }

    private synchronized void interrupt() {
        if (this.mayInterrupt) {
            this.ioThread.interrupt();
        } else {
            this.interrupted = true;
        }
    }

    @Nullable
    private WatchKey take() throws InterruptedException {
        synchronized (this) {
            if (this.interrupted) {
                this.interrupted = false;
                throw new InterruptedException();
            } else {
                this.mayInterrupt = true;
            }
        }

        try {
            return this.watcher.isDefined() ? this.watcher.get().take() : null;
        } finally {
            synchronized (this) {
                this.mayInterrupt = false;
            }
        }
    }

    @Nullable
    private WatchKey takeOrNullIfInterrupted() {
        try {
            return this.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    private void loop() {
        for (; ; ) {
            WatchKey key = this.takeOrNullIfInterrupted();
            if (key != null) {
                this.emitKey(key);
            } else if (this.shutdown) {
                this.watcher.peek(w -> Try.run(w::close));
                break;
            } else {
                this.processIOQueues();
            }
        }
    }

    private void emitKey(WatchKey key) {
        this.executeOnEventThread(() -> this.signalledKeys.push(key));
    }

    private void emitError(Throwable e) {
        this.executeOnEventThread(() -> this.errors.push(e));
    }

    private void processIOQueues() {
        Runnable action;
        while ((action = this.executorQueue.poll()) != null) {
            try {
                action.run();
            } catch (Throwable t) {
                this.errors.push(t);
            }
        }
    }
}
