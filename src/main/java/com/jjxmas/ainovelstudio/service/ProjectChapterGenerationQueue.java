package com.jjxmas.ainovelstudio.service;

import com.jjxmas.ainovelstudio.pojo.dto.ChapterStreamEvent;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Service
public class ProjectChapterGenerationQueue {

    private final ConcurrentHashMap<Long, ProjectQueue> queues = new ConcurrentHashMap<>();

    public Flux<ChapterStreamEvent> enqueue(Long projectId, Supplier<Flux<ChapterStreamEvent>> taskSupplier) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(taskSupplier, "taskSupplier must not be null");

        ProjectQueue queue = queues.computeIfAbsent(projectId, ignored -> new ProjectQueue());
        QueuedTask task = new QueuedTask(projectId, queue, taskSupplier);
        boolean runNow;
        int position;

        queue.lock.lock();
        try {
            runNow = !queue.running;
            if (runNow) {
                queue.running = true;
                position = 0;
            } else {
                queue.tasks.addLast(task);
                position = queue.tasks.size();
            }
        } finally {
            queue.lock.unlock();
        }

        if (runNow) {
            task.start();
            return task.flux();
        }
        return Mono.just(ChapterStreamEvent.queued("queued:" + position))
                .concatWith(task.flux())
                .doOnCancel(task::cancelIfWaiting);
    }

    private void finish(Long projectId, ProjectQueue queue) {
        QueuedTask next;
        queue.lock.lock();
        try {
            next = queue.tasks.pollFirst();
            if (next == null) {
                queue.running = false;
            }
        } finally {
            queue.lock.unlock();
        }

        if (next == null) {
            queues.remove(projectId, queue);
            return;
        }
        next.start();
    }

    private final class QueuedTask {

        private final Long projectId;
        private final ProjectQueue queue;
        private final Supplier<Flux<ChapterStreamEvent>> taskSupplier;
        private final Sinks.Many<ChapterStreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        private final AtomicBoolean finished = new AtomicBoolean(false);

        private QueuedTask(Long projectId, ProjectQueue queue, Supplier<Flux<ChapterStreamEvent>> taskSupplier) {
            this.projectId = projectId;
            this.queue = queue;
            this.taskSupplier = taskSupplier;
        }

        private Flux<ChapterStreamEvent> flux() {
            return sink.asFlux();
        }

        private void start() {
            Flux.defer(taskSupplier)
                    .onErrorResume(error -> Flux.just(ChapterStreamEvent.error(errorMessage(error))))
                    .doOnNext(sink::tryEmitNext)
                    .doFinally(signalType -> {
                        completeSink();
                        finish(projectId, queue);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
        }

        private void cancelIfWaiting() {
            queue.lock.lock();
            try {
                if (queue.tasks.remove(this)) {
                    completeSink();
                }
            } finally {
                queue.lock.unlock();
            }
        }

        private void completeSink() {
            if (finished.compareAndSet(false, true)) {
                sink.tryEmitComplete();
            }
        }

        private String errorMessage(Throwable error) {
            return error.getMessage() == null || error.getMessage().isBlank()
                    ? "章节生成失败"
                    : error.getMessage();
        }
    }

    private final class ProjectQueue {

        private final ReentrantLock lock = new ReentrantLock();
        private final ArrayDeque<QueuedTask> tasks = new ArrayDeque<>();
        private boolean running;
    }
}
