package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobLogReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * job log report helper
 * 处理报表，日志清理
 * @author xuxueli 2019-11-22
 */
public class JobLogReportHelper {
    private static Logger logger = LoggerFactory.getLogger(JobLogReportHelper.class);

    private static JobLogReportHelper instance = new JobLogReportHelper();
    public static JobLogReportHelper getInstance(){
        return instance;
    }


    private Thread logrThread;
    private volatile boolean toStop = false;

    /**
     * 初始化，启动一个守护线程处理任务报表
     * 每分钟执行一次
     */
    public void start(){
        logrThread = new Thread(new Runnable() {

            @Override
            public void run() {

                // 上次清理日志时间
                long lastCleanLogTime = 0;


                while (!toStop) {
                    // 1.任务报表刷新，默认统计今天,昨天,前天
                    // 1、log-report refresh: refresh log report in 3 days
                    try {
                        // 分别统计今天,昨天,前天0~24点的数据  每天开始时间为 00:00:00.000 结束时间为：23:59:59.999
                        for (int i = 0; i < 3; i++) {

                            // 获取当前迁移i天的开始时间数据。
                            Calendar itemDay = Calendar.getInstance();
                            itemDay.add(Calendar.DAY_OF_MONTH, -i);
                            itemDay.set(Calendar.HOUR_OF_DAY, 0);
                            itemDay.set(Calendar.MINUTE, 0);
                            itemDay.set(Calendar.SECOND, 0);
                            itemDay.set(Calendar.MILLISECOND, 0);
                            // 开始时间： 00:00:00.000
                            Date todayFrom = itemDay.getTime();

                            itemDay.set(Calendar.HOUR_OF_DAY, 23);
                            itemDay.set(Calendar.MINUTE, 59);
                            itemDay.set(Calendar.SECOND, 59);
                            itemDay.set(Calendar.MILLISECOND, 999);
                            // 结束时间： 23:59:59.999
                            Date todayTo = itemDay.getTime();

                            // refresh log-report every minute
                            XxlJobLogReport xxlJobLogReport = new XxlJobLogReport();
                            xxlJobLogReport.setTriggerDay(todayFrom);
                            xxlJobLogReport.setRunningCount(0);
                            xxlJobLogReport.setSucCount(0);
                            xxlJobLogReport.setFailCount(0);

                            // 查询当前数据 开始时间为 00:00:00.000 结束时间为：23:59:59.999
                            Map<String, Object> triggerCountMap = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findLogReport(todayFrom, todayTo);
                            if (triggerCountMap!=null && triggerCountMap.size()>0) {
                                // 触发总数
                                int triggerDayCount = triggerCountMap.containsKey("triggerDayCount")?Integer.valueOf(String.valueOf(triggerCountMap.get("triggerDayCount"))):0;
                                // 运行中 trigger_code in (0, 200) and handle_code = 0
                                int triggerDayCountRunning = triggerCountMap.containsKey("triggerDayCountRunning")?Integer.valueOf(String.valueOf(triggerCountMap.get("triggerDayCountRunning"))):0;
                                // 成功 handle_code = 200
                                int triggerDayCountSuc = triggerCountMap.containsKey("triggerDayCountSuc")?Integer.valueOf(String.valueOf(triggerCountMap.get("triggerDayCountSuc"))):0;
                                // 失败数据
                                int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;

                                xxlJobLogReport.setRunningCount(triggerDayCountRunning);
                                xxlJobLogReport.setSucCount(triggerDayCountSuc);
                                xxlJobLogReport.setFailCount(triggerDayCountFail);
                            }

                            // do refresh 先执行更新，无数据才插入，能在一定程度上解决调度器执行器多节点并发问题
                            // 旧数据执行更新，新数据执行保存。更新返回的是变动行数，小于1则表示库里不存在 。根据报表时间更新数据
                            int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobLogReportDao().update(xxlJobLogReport);
                            if (ret < 1) {
                                // 这里还是有很小的可能会同时执行到，导致数据有多份的情况
                                XxlJobAdminConfig.getAdminConfig().getXxlJobLogReportDao().save(xxlJobLogReport);
                            }
                        }

                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> xxl-job, job log report thread error:{}", e);
                        }
                    }

                    // 2, 日志清理，开关打卡并且每24小时执行一次
                    // 2、log-clean: switch open & once each day
                    if (XxlJobAdminConfig.getAdminConfig().getLogretentiondays()>0
                            && System.currentTimeMillis() - lastCleanLogTime > 24*60*60*1000) {

                        // expire-time 获取开始清理时间。例如配置了7天，今天是2023-08-12 那么clearBeforeTime就是2023-08-05 00:00:00.000
                        Calendar expiredDay = Calendar.getInstance();
                        expiredDay.add(Calendar.DAY_OF_MONTH, -1 * XxlJobAdminConfig.getAdminConfig().getLogretentiondays());
                        expiredDay.set(Calendar.HOUR_OF_DAY, 0);
                        expiredDay.set(Calendar.MINUTE, 0);
                        expiredDay.set(Calendar.SECOND, 0);
                        expiredDay.set(Calendar.MILLISECOND, 0);
                        Date clearBeforeTime = expiredDay.getTime();

                        // clean expired log
                        List<Long> logIds = null;
                        do {
                            // 每次1000条 执行清理，mysql in最多1000个， 感觉可以直接用时间判断delete一次删除即可
                            logIds = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findClearLogIds(0, 0, clearBeforeTime, 0, 1000);
                            if (logIds!=null && logIds.size()>0) {
                                XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().clearLog(logIds);
                            }
                        } while (logIds!=null && logIds.size()>0);

                        // 更新清理时间
                        lastCleanLogTime = System.currentTimeMillis();
                    }

                    try {
                        // 每1分钟钟执行一次
                        TimeUnit.MINUTES.sleep(1);
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                }

                logger.info(">>>>>>>>>>> xxl-job, job log report thread stop");

            }
        });
        logrThread.setDaemon(true);
        logrThread.setName("xxl-job, admin JobLogReportHelper");
        logrThread.start();
    }

    public void toStop(){
        toStop = true;
        // interrupt and wait
        logrThread.interrupt();
        try {
            logrThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
