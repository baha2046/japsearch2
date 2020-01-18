package org.nagoya.system;


import org.fxmisc.livedirs.InitiatorTrackingIOFacility;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

class LiveDirsIO<I> implements InitiatorTrackingIOFacility<I> {
    private final DirWatcher dirWatcher;
    private final LiveDirsModel<I, ?> model;
    private final Executor clientThreadExecutor;

    public LiveDirsIO(DirWatcher dirWatcher, LiveDirsModel<I, ?> model, Executor clientThreadExecutor) {
        this.dirWatcher = dirWatcher;
        this.model = model;
        this.clientThreadExecutor = clientThreadExecutor;
    }

    @Override
    public CompletionStage<Void> createFile(Path file, I initiator) {
        CompletableFuture<Void> created = new CompletableFuture<>();
        this.dirWatcher.createFile(file,
                lastModified -> {
                    this.model.addFile(file, initiator, lastModified);
                    created.complete(null);
                },
                created::completeExceptionally);
        return this.wrap(created);
    }

    @Override
    public CompletionStage<Void> createDirectory(Path dir, I initiator) {
        CompletableFuture<Void> created = new CompletableFuture<>();
        this.dirWatcher.createDirectory(dir,
                () -> {
                    if (this.model.containsPrefixOf(dir)) {
                        this.model.addDirectory(dir, initiator);
                        this.dirWatcher.watchOrLogError(dir);
                    }
                    created.complete(null);
                },
                created::completeExceptionally);
        return this.wrap(created);
    }

    @Override
    public CompletionStage<Void> saveTextFile(Path file, String content, Charset charset, I initiator) {
        CompletableFuture<Void> saved = new CompletableFuture<>();
        this.dirWatcher.saveTextFile(file, content, charset,
                lastModified -> {
                    this.model.updateModificationTime(file, lastModified, initiator);
                    saved.complete(null);
                },
                saved::completeExceptionally);
        return this.wrap(saved);
    }

    @Override
    public CompletionStage<Void> saveBinaryFile(Path file, byte[] content, I initiator) {
        CompletableFuture<Void> saved = new CompletableFuture<>();
        this.dirWatcher.saveBinaryFile(file, content,
                lastModified -> {
                    this.model.updateModificationTime(file, lastModified, initiator);
                    saved.complete(null);
                },
                saved::completeExceptionally);
        return this.wrap(saved);
    }

    @Override
    public CompletionStage<Void> delete(Path file, I initiator) {
        CompletableFuture<Void> deleted = new CompletableFuture<>();
        this.dirWatcher.deleteFileOrEmptyDirectory(file,
                () -> {
                    this.model.delete(file, initiator);
                    deleted.complete(null);
                },
                deleted::completeExceptionally);
        return this.wrap(deleted);
    }

    @Override
    public CompletionStage<Void> deleteTree(Path root, I initiator) {
        CompletableFuture<Void> deleted = new CompletableFuture<>();
        this.dirWatcher.deleteTree(root,
                () -> {
                    this.model.delete(root, initiator);
                    deleted.complete(null);
                },
                deleted::completeExceptionally);
        return this.wrap(deleted);
    }

    @Override
    public CompletionStage<String> loadTextFile(Path file, Charset charset) {
        CompletableFuture<String> loaded = new CompletableFuture<>();
        this.dirWatcher.loadTextFile(file, charset,
                loaded::complete,
                loaded::completeExceptionally);
        return this.wrap(loaded);
    }

    @Override
    public CompletionStage<byte[]> loadBinaryFile(Path file) {
        CompletableFuture<byte[]> loaded = new CompletableFuture<>();
        this.dirWatcher.loadBinaryFile(file,
                loaded::complete,
                loaded::completeExceptionally);
        return this.wrap(loaded);
    }

    private <T> CompletionStage<T> wrap(CompletionStage<T> stage) {
        return new CompletionStageWithDefaultExecutor<>(stage, this.clientThreadExecutor);
    }
}