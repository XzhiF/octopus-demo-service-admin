package com.octopus.demo.admin.schedule;

import org.springframework.stereotype.Component;

/** Sample task used to verify the scheduling chain; every run records the message {@code heartbeat}. */
@Component("demoHeartbeatJob")
public class DemoHeartbeatJob implements ScheduledTask {

    @Override
    public String execute(String params) {
        return "heartbeat";
    }
}
