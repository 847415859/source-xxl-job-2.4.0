package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.core.trigger.XxlJobTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * job trigger thread pool helper
 * 触发器线程池协助类
 * @author xuxueli 2018-07-03 21:08:07
 */
public class JobTriggerPoolHelper {
    private static Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);


    // ---------------------- trigger pool ----------------------

    // fast/slow thread pool
    private ThreadPoolExecutor fastTriggerPool = null;
    private ThreadPoolExecutor slowTriggerPool = null;

    /**
     * 初始化
     * 调度器启动时，初始化了两个线程池，除了慢线程池的队列大一些以及最大线程数由用户自定义以外，其他配置都一致。
     * 快线程池用于处理时间短的任务，慢线程池用于处理时间长的任务
     */
    public void start(){
        // 核心线程数10，最大线程数来自配置，存活时间为60s，队列大小1000，线程工厂配置线程名。拒绝策略为AbortPolicy，直接抛出异常
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-job, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode());
                    }
                });
        //最大100线程，最多处理2000任务,一分钟内超时10次，则采用慢触发器执行
        slowTriggerPool = new ThreadPoolExecutor(
                10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "xxl-job, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode());
                    }
                });
    }


    public void stop() {
        //triggerPool.shutdown();
        fastTriggerPool.shutdownNow();
        slowTriggerPool.shutdownNow();
        logger.info(">>>>>>>>> xxl-job trigger thread pool shutdown success.");
    }


    // 任务超时时间
    private volatile long minTim = System.currentTimeMillis()/60000;     // ms > min
    private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();


    /**
     * add trigger
     * 触发任务，将任务交给触发器线程池
     * @param jobId                     任务id
     * @param triggerType               触发类型
     * @param failRetryCount            剩余失败冲刺次数
     * 			>=0: use this param
     * 			<0: use param from job info config
     * @param executorShardingParam     执行分片参数
     * @param executorParam             执行参数
     *          null: use job param
     *          not null: cover job param
     * @param addressList               执行客户端地址
     */
    public void addTrigger(final int jobId,
                           final TriggerTypeEnum triggerType,
                           final int failRetryCount,
                           final String executorShardingParam,
                           final String executorParam,
                           final String addressList) {

        // choose thread pool
        ThreadPoolExecutor triggerPool_ = fastTriggerPool;
        // 超时次数
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        // 在一分钟之内超时十次则使用慢线程池
        if (jobTimeoutCount!=null && jobTimeoutCount.get() > 10) {      // job-timeout 10 times in 1 min
            triggerPool_ = slowTriggerPool;
        }

        // trigger
        triggerPool_.execute(new Runnable() {
            @Override
            public void run() {

                long start = System.currentTimeMillis();

                try {
                    // 执行触发器
                    XxlJobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                } finally {

                    // 每分钟置空超时记录
                    long minTim_now = System.currentTimeMillis()/60000;
                    if (minTim != minTim_now) {
                        minTim = minTim_now;
                        jobTimeoutCountMap.clear();
                    }

                    // incr timeout-count-map
                    long cost = System.currentTimeMillis()-start;
                    // 花费时间超过 500ms 则超时次数+1
                    if (cost > 500) {       // ob-timeout threshold 500ms
                        AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                        if (timeoutCount != null) {
                            timeoutCount.incrementAndGet();
                        }
                    }

                }

            }
        });
    }



    // ---------------------- helper ----------------------

    private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    public static void toStart() {
        helper.start();
    }
    public static void toStop() {
        helper.stop();
    }

    /**
     * 触发任务，将任务交给触发器线程池
     * @param jobId                     任务id
     * @param triggerType               触发类型
     * @param failRetryCount            剩余失败冲刺次数
     * 			>=0: use this param                 任用当前的配置
     * 			<0: use param from job info config  使用任务配置
     * @param executorShardingParam     执行分片参数
     * @param executorParam             执行参数
     *          null: use job param
     *          not null: cover job param
     * @param addressList               执行客户端地址
     */
    public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam, String addressList) {
        helper.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
    }

}
