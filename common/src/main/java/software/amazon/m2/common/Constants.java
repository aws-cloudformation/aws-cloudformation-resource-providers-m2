package software.amazon.m2.common;

import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;

public class Constants {
    // for retrying calls during stabilization
    public static final Constant BACKOFF_STRATEGY_STABILIZE = Constant.of()
        .timeout(Duration.ofDays(3L))
        .delay(Duration.ofSeconds(20L))
        .build();
}
