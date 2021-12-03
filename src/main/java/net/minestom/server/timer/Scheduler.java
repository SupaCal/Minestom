package net.minestom.server.timer;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Supplier;

public sealed interface Scheduler permits SchedulerImpl {
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

    @NotNull OwnedTask submit(@NotNull Supplier<TaskSchedule> task,
                              @NotNull ExecutionType executionType);

    @NotNull Collection<@NotNull OwnedTask> scheduledTasks();

    static @NotNull Scheduler newScheduler() {
        return new SchedulerImpl();
    }
}