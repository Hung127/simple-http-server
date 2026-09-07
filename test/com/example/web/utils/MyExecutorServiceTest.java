package com.example.web.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MyExecutorServiceTest {

    private MyExecutorService executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            try {
                executor.stop();
            } catch (IllegalStateException e) {
                // already stopped; nothing to do
            }
        }
    }

    @Test
    void startExecutesJobs() throws Exception {
        executor = new MyExecutorService(2);
        executor.start();

        int jobCount = 5;
        CountDownLatch allDone = new CountDownLatch(jobCount);
        for (int i = 0; i < jobCount; i++) {
            executor.addJob(allDone::countDown);
        }

        assertTrue(allDone.await(5, TimeUnit.SECONDS));
    }

    @Test
    void startRunsJobsConcurrently() throws Exception {
        executor = new MyExecutorService(3);

        int poolSize = 3;
        CountDownLatch started = new CountDownLatch(poolSize);
        CountDownLatch release = new CountDownLatch(1);
        Set<String> runningThreads = ConcurrentHashMap.newKeySet();

        executor.start();

        for (int i = 0; i < poolSize; i++) {
            executor.addJob(() -> {
                runningThreads.add(Thread.currentThread().getName());
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(started.await(5, TimeUnit.SECONDS));
        release.countDown();

        assertEquals(poolSize, runningThreads.size());
    }

    @Test
    void rejectsNullJob() {
        executor = new MyExecutorService(1);
        assertThrows(NullPointerException.class, () -> executor.addJob(null));
    }

    @Test
    void rejectsZeroPoolSize() {
        assertThrows(IllegalArgumentException.class, () -> new MyExecutorService(0));
    }

    @Test
    void rejectsNegativePoolSize() {
        assertThrows(IllegalArgumentException.class, () -> new MyExecutorService(-1));
    }

    @Test
    void addJobBeforeStartThrows() {
        executor = new MyExecutorService(1);
        assertThrows(IllegalStateException.class, () -> executor.addJob(() -> { }));
    }

    @Test
    void addJobAfterStopThrows() {
        executor = new MyExecutorService(1);
        executor.start();
        executor.stop();
        assertThrows(IllegalStateException.class, () -> executor.addJob(() -> { }));
    }

    @Test
    void startTwiceThrows() {
        executor = new MyExecutorService(1);
        executor.start();
        assertThrows(IllegalStateException.class, executor::start);
    }

    @Test
    void stopWhenNotRunningThrows() {
        executor = new MyExecutorService(1);
        assertThrows(IllegalStateException.class, executor::stop);
    }

    @Test
    void stopWaitsForRunningJob() throws Exception {
        executor = new MyExecutorService(1);
        executor.start();

        CountDownLatch jobFinished = new CountDownLatch(1);
        executor.addJob(jobFinished::countDown);

        executor.stop();

        assertEquals(0, jobFinished.getCount());
    }

    @Test
    void jobExceptionIsSwallowedAndWorkersSurvive() throws Exception {
        executor = new MyExecutorService(1);

        executor.start();
        executor.addJob(() -> {
            throw new RuntimeException("boom");
        });

        CountDownLatch after = new CountDownLatch(1);
        executor.addJob(after::countDown);

        assertTrue(after.await(5, TimeUnit.SECONDS));
    }

    @Test
    void runsMoreJobsThanPoolSize() throws Exception {
        executor = new MyExecutorService(2);

        int jobCount = 50;
        AtomicInteger executed = new AtomicInteger(0);
        CountDownLatch allDone = new CountDownLatch(jobCount);

        executor.start();
        for (int i = 0; i < jobCount; i++) {
            executor.addJob(() -> {
                executed.incrementAndGet();
                allDone.countDown();
            });
        }

        assertTrue(allDone.await(10, TimeUnit.SECONDS));
        assertEquals(jobCount, executed.get());
    }
}
