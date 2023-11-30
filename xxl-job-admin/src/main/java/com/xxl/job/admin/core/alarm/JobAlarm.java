package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;

/**
 * @author xuxueli 2020-01-19
 * 任务告警接口
 */
public interface JobAlarm {

    /**
     * job alarm
     *
     * @param info      任务信息
     * @param jobLog    任务日志
     * @return
     */
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog);

}
