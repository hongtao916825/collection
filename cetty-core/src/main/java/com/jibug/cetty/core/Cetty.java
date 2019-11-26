package com.jibug.cetty.core;

import com.jibug.cetty.core.concurrent.CountableThreadPool;
import com.jibug.cetty.core.concurrent.NamedThreadFactory;
import com.jibug.cetty.core.handler.HandlerPipeline;
import com.jibug.cetty.core.handler.HttpDownloadHandler;
import com.jibug.cetty.core.net.AsyncHttpClientGenerator;
import com.jibug.cetty.core.net.HttpClientGenerator;
import com.jibug.cetty.core.net.SyncHttpClientGenerator;
import com.jibug.cetty.core.scheduler.QueueScheduler;
import com.jibug.cetty.core.scheduler.Scheduler;
import com.google.common.collect.Lists;
import com.jibug.cetty.core.utils.UrlUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author heyingcai
 * @date 2018/7/3
 */
public class Cetty implements Runnable {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private AtomicInteger stat = new AtomicInteger(STAT_INIT);

    private final static int STAT_INIT = 0;

    private final static int STAT_RUNNING = 1;

    private final static int STAT_STOPPED = 2;

    private String name;

    private CountableThreadPool countableThreadPool;

    private ThreadPoolExecutor threadPoolExecutor;

    private int threadNum = 1;

    private ReentrantLock newTask = new ReentrantLock();

    private Condition newTaskCondition = newTask.newCondition();

    private long stopAwaitTime = 20;

    private long newTaskWaitTime = 30000;

    private List<Seed> startSeeds;

    /**
     * crawler duration grab
     * default value is not duration
     * when there is no task, the crawler stops after a period of time.
     */
    private boolean duration = false;

    /**
     * crawler is support async
     * default value is sync
     */
    private boolean async = false;

    private HttpClientGenerator<CloseableHttpAsyncClient> asyncHttpClientGenerator;

    private HttpClientGenerator<CloseableHttpClient> httpClientHttpClientGenerator;

    private CloseableHttpAsyncClient httpAsyncClient;

    private CloseableHttpClient httpClient;

    /**
     * crawler request payload
     */
    private Payload payload;

    /**
     * the crawler global handler
     * these handler all in the pipeline
     */
    private HandlerPipeline pipeline;

    /**
     * url scheduler
     */
    private Scheduler scheduler = new QueueScheduler();

    public Cetty() {
        this.pipeline = new HandlerPipeline(this);
        // downloader handler must have one
        boolean hasDownloadHandler = pipeline.checkDownloadHandler();
        if (!hasDownloadHandler) {
            pipeline.addLast(new HttpDownloadHandler(), "downloader");
        }

    }

    public Cetty setPayload(Payload payload) {
        this.payload = payload;
        return this;
    }

    public Payload getPayload() {
        return payload;
    }

    public Cetty setStartUrl(String url) {
        checkRunningStat();
        this.startSeeds = Arrays.asList(new Seed(url));
        return this;
    }

    public Cetty setStartUrls(List<String> urls) {
        checkRunningStat();
        this.startSeeds = convertSeed(urls);
        return this;
    }

    public Cetty setStartSeed(Seed seed) {
        checkRunningStat();
        this.startSeeds = Arrays.asList(seed);
        return this;
    }

    public Cetty setStartSeeds(List<Seed> seeds) {
        checkRunningStat();
        this.startSeeds = seeds;
        return this;
    }

    private List<Seed> convertSeed(List<String> urls) {
        List<Seed> seeds = Lists.newArrayListWithCapacity(urls.size());
        urls.forEach(url -> {
            seeds.add(new Seed(url));
        });
        return seeds;
    }

    public Cetty setScheduler(Scheduler scheduler) {
        checkRunningStat();
        Scheduler oldScheduler = this.scheduler;
        this.scheduler = scheduler;
        if (oldScheduler != null) {
            Seed seed;
            while ((seed = oldScheduler.poll()) != null) {
                scheduler.push(seed);
            }
        }
        return this;
    }

    public Cetty setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        checkRunningStat();
        this.threadPoolExecutor = threadPoolExecutor;
        return this;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public HandlerPipeline pipeline() {
        return pipeline;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public void setDuration(boolean duration) {
        this.duration = duration;
    }

    public HttpClientGenerator<CloseableHttpAsyncClient> getAsyncHttpClientGenerator() {
        return asyncHttpClientGenerator;
    }

    public HttpClientGenerator<CloseableHttpClient> getHttpClientHttpClientGenerator() {
        return httpClientHttpClientGenerator;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public CloseableHttpAsyncClient getHttpAsyncClient() {
        return httpAsyncClient;
    }

    public Cetty setThreadNum(int threadNum) {
        this.threadNum = threadNum;
        return this;
    }

    public String getName() {
        if (name != null) {
            return name;
        }
        if (payload.getDomain() != null) {
            return payload.getDomain();
        }
        if (startSeeds.size() > 0) {
            Seed seed = startSeeds.get(0);
            return UrlUtils.getDomain(seed.getUrl());
        }
        name = UUID.randomUUID().toString();
        return name;
    }

    @Override
    public void run() {
        checkRunningStat();
        initComponent();
        logger.info("Crawler {} started!", getName());
        while (!Thread.currentThread().isInterrupted() && stat.get() == STAT_RUNNING) {
            final Seed seed = scheduler.poll();

            if (seed == null) {
                if (!duration) {
                    if (countableThreadPool.getThreadAliveCount() == 0 || stat.get() == STAT_STOPPED) {
                        break;
                    }
                }
                waitTask();
            } else {
                countableThreadPool.execute(new SeedTask(seed));
            }
        }
        if (!countableThreadPool.isShutdown()) {
            countableThreadPool.isShutdown();
            try {
                countableThreadPool.getThreadPoolExecutor().awaitTermination(stopAwaitTime, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Cetty {} crawler wait failed !", getName());
            }
        }
        stopCrawler();
    }

    private class SeedTask implements Runnable {

        private Seed seed;

        SeedTask(Seed seed) {
            this.seed = seed;
        }

        @Override
        public void run() {
            try {
                pipeline.download(seed);
            } catch (Exception e) {
                logger.error("Cetty crawler run error {}", e);
            } finally {
                signalTask();
            }
        }
    }

    public void stopCrawler() {
        if (stat.compareAndSet(STAT_RUNNING, STAT_STOPPED)) {
            logger.info("Cetty {} crawler closed!", getName());
        }

        releaseObject();

        if (!Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
        }
    }

    public void startCrawler() {
        Thread thread = new Thread(this);
        thread.setDaemon(false);
        thread.start();
    }

    private void releaseObject() {
        if (httpAsyncClient != null) {
            try {
                httpAsyncClient.close();
            } catch (IOException e) {
                logger.warn("close httpAsyncClient error {}", e);
            }
        }
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.warn("close httpClient error {}", e);
            }
        }
    }

    public void close() {
        releaseObject();
        countableThreadPool.shutdown();
    }

    private void waitTask() {
        newTask.lock();
        try {
            newTaskCondition.await(newTaskWaitTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.warn("waitNewTask interrupted, error {}", e);
        } finally {
            newTask.unlock();
        }
    }

    private void signalTask() {
        try {
            newTask.lock();
            newTaskCondition.signalAll();
        } finally {
            newTask.unlock();
        }
    }

    protected void checkRunningStat() {
        if (stat.get() == STAT_RUNNING) {
            throw new IllegalStateException("Crawler is already running!");
        }
    }

    private void pushSeed(Seed seed) {
        if (seed != null && seed.getUrl() != null) {
            scheduler.push(seed);
        }
    }

    private void initComponent() {
        HandlerPipeline pipeline = this.pipeline();

        if (async) {
            asyncHttpClientGenerator = new AsyncHttpClientGenerator();
            httpAsyncClient = asyncHttpClientGenerator.getClient(getPayload());
            httpAsyncClient.start();
        } else {
            httpClientHttpClientGenerator = new SyncHttpClientGenerator();
            httpClient = httpClientHttpClientGenerator.getClient(getPayload());
        }

        boolean threadPoolAvailable = threadNum > 0 && countableThreadPool == null || countableThreadPool.isShutdown();
        if (threadPoolAvailable) {
            if (threadPoolExecutor != null && !threadPoolExecutor.isShutdown()) {
                countableThreadPool = new CountableThreadPool(threadNum, threadPoolExecutor);
            } else {
                countableThreadPool = new CountableThreadPool(threadNum);
            }
        }

        if (startSeeds != null) {
            startSeeds.forEach(seed -> {
                pushSeed(seed);
            });
        }

        pipeline.start();

        stat.set(STAT_RUNNING);
    }

}
