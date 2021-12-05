package net.minestom.server.timer;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Supplier;

public sealed interface Scheduler permits SchedulerImpl, SchedulerManager {
    /**
     * Process scheduled tasks based on time.
     * <p>
     * Can be used to increase scheduling precision.
     */
    void process();

    /**
     * Advance 1 tick and call {@link #process()}.
     */
    void processTick();

    @NotNull Task submit(@NotNull Supplier<TaskSchedule> task,
                         @NotNull ExecutionType executionType);

    @NotNull Task submitAfter(@NotNull TaskSchedule schedule,
                              @NotNull Supplier<TaskSchedule> task,
                              @NotNull ExecutionType executionType);

    default @NotNull Task.Builder buildTask(@NotNull Runnable task) {
        return new Task.Builder(this, task);
    }

    @NotNull Collection<@NotNull Task> scheduledTasks();

    static @NotNull Scheduler newScheduler() {
        return new SchedulerImpl();
    }
}
