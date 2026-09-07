package com.example.web.utils;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyExecutorService {
    private final int threadpool;
    private final BlockingQueue<Runnable> jobsQueue;
    private final ArrayList<Thread> workers;
    private volatile boolean running = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(MyExecutorService.class);

    public MyExecutorService(int threadPool) {
        this.jobsQueue = new LinkedBlockingQueue<Runnable>();
        if (threadPool <= 0) {
            throw new IllegalArgumentException("ThreadPool must be > 0");
        }
        this.threadpool = threadPool;
        this.workers = new ArrayList<>();
    }

    public void addJob(Runnable job) {
        if (job == null) {
            throw new NullPointerException("Job cannot be null");
        }
        if (!this.running) {
            throw new IllegalStateException("Cannot add job if stopped");
        }

        this.jobsQueue.add(job);
    }

    public void start() {
        if (this.running) {
            throw new IllegalStateException("It is already running");
        }

        this.workers.clear();

        this.running = true;

        for (int i = 0; i < this.threadpool; i++) {
            Thread worker = new Thread(() -> {
                while (this.running) {
                    try {
                        Runnable job = this.jobsQueue.take();

                        try {
                            job.run();
                        } catch (Exception e) {
                            LOGGER.debug("Cannot run task");
                        }
                    } catch (InterruptedException e) {
                        if (!this.running) {
                            break;
                        }
                    }
                }
            });

            this.workers.add(worker);
            worker.start();
        }
    }

    public void stop() {
        if (!this.running) {
            throw new IllegalStateException("It is already stopped");
        }

        this.running = false;

        for (Thread worker : this.workers) {
            worker.interrupt();
        }

        for (Thread worker : this.workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
