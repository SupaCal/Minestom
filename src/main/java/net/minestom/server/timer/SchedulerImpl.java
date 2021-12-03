package net.minestom.server.timer;

import com.zaxxer.sparsebits.SparseBitSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minestom.server.MinecraftServer;
import org.jctools.queues.MpscGrowableArrayQueue;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

final class SchedulerImpl implements Scheduler {
    private static final AtomicInteger TASK_COUNTER = new AtomicInteger();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final ForkJoinPool EXECUTOR = ForkJoinPool.commonPool();

    private final Set<OwnedTask> tasks = ConcurrentHashMap.newKeySet();
    private final SparseBitSet bitSet = new SparseBitSet();
    private final ReadWriteLock bitSetLock = new ReentrantReadWriteLock();

    private final Int2ObjectAVLTreeMap<List<OwnedTask>> tickTaskQueue = new Int2ObjectAVLTreeMap<>();
    private final MpscGrowableArrayQueue<OwnedTask> taskQueue = new MpscGrowableArrayQueue<>(64);
    private final Set<OwnedTask> parkedTasks = ConcurrentHashMap.newKeySet();

    private final AtomicInteger tickState = new AtomicInteger();

    @Override
    public void process() {
        processTick(tickState.get());
    }

    @Override
    public void processTick() {
        processTick(tickState.incrementAndGet());
    }

    private synchronized void processTick(int tick) {
        // Tasks based on tick
        synchronized (tickTaskQueue) {
            if (!tickTaskQueue.isEmpty()) {
                int tickToProcess;
                while ((tickToProcess = tickTaskQueue.firstIntKey()) <= tick) {
                    final List<OwnedTask> tickScheduledTasks = tickTaskQueue.remove(tickToProcess);
                    if (tickScheduledTasks != null) tickScheduledTasks.forEach(this::execute);
                }
            }
        }
        // Unparked tasks & based on time
        this.taskQueue.drain(this::execute);
    }

    @Override
    public @NotNull OwnedTask submit(@NotNull Supplier<TaskSchedule> task,
                                     @NotNull ExecutionType executionType) {
        OwnedTaskImpl taskRef = new OwnedTaskImpl(TASK_COUNTER.getAndIncrement(), task,
                executionType, this);
        var lock = bitSetLock.writeLock();
        lock.lock();
        try {
            this.bitSet.set(taskRef.id());
            this.tasks.add(taskRef);
        } finally {
            lock.unlock();
        }
        execute(taskRef);
        return taskRef;
    }

    @Override
    public @NotNull Collection<@NotNull OwnedTask> scheduledTasks() {
        return Collections.unmodifiableSet(tasks);
    }

    void unparkTask(OwnedTask task) {
        if (!parkedTasks.remove(task)) {
            throw new IllegalStateException("Task is not parked");
        }
        execute(task);
        // TODO task executed on next processing?
        // this.taskQueue.relaxedOffer(task);
    }

    void stopTask(OwnedTask task) {
        var lock = bitSetLock.writeLock();
        lock.lock();
        try {
            this.bitSet.clear(task.id());
            if (!tasks.remove(task))
                throw new IllegalStateException("Task is not scheduled");
        } finally {
            lock.unlock();
        }
    }

    boolean isTaskAlive(OwnedTaskImpl task) {
        var lock = bitSetLock.readLock();
        lock.lock();
        try {
            return bitSet.get(task.id());
        } finally {
            lock.unlock();
        }
    }

    private void execute(OwnedTask task) {
        if (!task.isAlive()) return;
        switch (task.executionType()) {
            case SYNC -> handleStatus(task);
            case ASYNC -> EXECUTOR.submit(() -> handleStatus(task));
        }
    }

    private void handleStatus(OwnedTask task) {
        final TaskSchedule schedule = ((OwnedTaskImpl) task).task().get();
        if (schedule instanceof TaskScheduleImpl.DurationSchedule durationSchedule) {
            final Duration duration = durationSchedule.duration();
            SCHEDULER.schedule(() -> taskQueue.offer(task), duration.toMillis(), TimeUnit.MILLISECONDS);
        } else if (schedule instanceof TaskScheduleImpl.TickSchedule tickSchedule) {
            final int target = tickState.get() + tickSchedule.tick();
            synchronized (tickTaskQueue) {
                this.tickTaskQueue.computeIfAbsent(target, i -> new ArrayList<>()).add(task);
            }
        } else if (schedule instanceof TaskScheduleImpl.FutureSchedule futureSchedule) {
            futureSchedule.future().whenComplete((o, throwable) -> {
                if (throwable != null) {
                    MinecraftServer.getExceptionManager().handleException(throwable);
                    return;
                }
                execute(task);
            });
        } else if (schedule instanceof TaskScheduleImpl.Park) {
            this.parkedTasks.add(task);
        } else if (schedule instanceof TaskScheduleImpl.Stop) {
            this.tasks.remove(task);
        }
    }
}