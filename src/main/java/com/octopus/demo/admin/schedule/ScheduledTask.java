package com.octopus.demo.admin.schedule;

/**
 * Contract for a Spring bean addressable through {@code sys_job.bean_name}.
 * Implementations are invoked by {@link QuartzJob} on every fire.
 */
public interface ScheduledTask {

    /**
     * Runs the task.
     *
     * @param params the row's free-form {@code params} column, may be {@code null}
     * @return a short result message recorded with the execution
     */
    String execute(String params) throws Exception;
}
