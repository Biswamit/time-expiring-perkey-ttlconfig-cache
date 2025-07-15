package com.biswamit.cache;

import com.github.benmanes.caffeine.cache.Ticker;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

public class FakeTicker implements Ticker {
    private long nanos = System.nanoTime();

    @Override
    public long read() {
        return this.nanos;
    }

    public void advance(long time, TimeUnit timeUnit) {
        this.nanos += timeUnit.toNanos(time);
    }

    public void advance(Duration duration) {
        this.nanos += duration.toNanos();
    }
}
