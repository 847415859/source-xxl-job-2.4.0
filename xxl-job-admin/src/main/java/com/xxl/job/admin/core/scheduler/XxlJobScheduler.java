package com.xxl.job.admin.core.scheduler;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.thread.*;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.client.ExecutorBizClient;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xuxueli 2018-10-28 00:18:17
 */

public class XxlJobScheduler  {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobScheduler.class);


    public void init() throws Exception {
        // init i18n       初始化或计划
        initI18n();

        // admin trigger pool start
        // 初始化触发器线程池
        JobTriggerPoolHelper.toStart();

        // admin registry monitor run
        // 30秒执行一次,维护注册表信息， 判断在线超时时间90s
        JobRegistryHelper.getInstance().start();

        // admin fail-monitor run
        // 运行失败监视器，主要失败发送邮箱，重试触发器
        JobFailMonitorHelper.getInstance().start();

        // admin lose-monitor run ( depend on JobTriggerPoolHelper )
        // 任务结果处理，包括执行器正常回调和任务结果丢失处理
        // 度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；
        JobCompleteHelper.getInstance().start();

        // admin log report start
        // 统计一些失败成功报表
        JobLogReportHelper.getInstance().start();

        // start-schedule  ( depend on JobTriggerPoolHelper )
        // 执行调度器
        JobScheduleHelper.getInstance().start();

        logger.info(">>>>>>>>> init xxl-job admin success.");
    }

    
    public void destroy() throws Exception {

        // stop-schedule
        JobScheduleHelper.getInstance().toStop();

        // admin log report stop
        JobLogReportHelper.getInstance().toStop();

        // admin lose-monitor stop
        JobCompleteHelper.getInstance().toStop();

        // admin fail-monitor stop
        JobFailMonitorHelper.getInstance().toStop();

        // admin registry stop
        JobRegistryHelper.getInstance().toStop();

        // admin trigger pool stop
        JobTriggerPoolHelper.toStop();

    }

    // ---------------------- I18n ----------------------

    private void initI18n(){
        for (ExecutorBlockStrategyEnum item:ExecutorBlockStrategyEnum.values()) {
            // SERIAL_EXECUTION=单机串行
            // DISCARD_LATER=丢弃后续调度
            // COVER_EARLY=覆盖之前调度
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }

    // ---------------------- executor-client ----------------------
    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<String, ExecutorBiz>();

    /**
     *
     * @param address
     * @return
     * @throws Exception
     */
    public static ExecutorBiz getExecutorBiz(String address) throws Exception {
        // valid
        if (address==null || address.trim().length()==0) {
            return null;
        }

        // load-cache
        // 从缓冲中通过地址获取ExecutorBiz
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }

        // set-cache
        // 找不到就新建,添加缓存
        executorBiz = new ExecutorBizClient(address, XxlJobAdminConfig.getAdminConfig().getAccessToken());

        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }

}
