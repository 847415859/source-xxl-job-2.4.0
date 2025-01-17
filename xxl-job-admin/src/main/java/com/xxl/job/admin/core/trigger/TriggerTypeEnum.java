package com.xxl.job.admin.core.trigger;

import com.xxl.job.admin.core.util.I18nUtil;

/**
 * trigger type enum
 * 触发器类型
 *
 * @author xuxueli 2018-09-16 04:56:41
 */
public enum TriggerTypeEnum {

    // 手动触发
    MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),
    // crom表达式触发
    CRON(I18nUtil.getString("jobconf_trigger_type_cron")),
    // 重试
    RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),
    // 父任务触发
    PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),
    // API触发
    API(I18nUtil.getString("jobconf_trigger_type_api")),
    // 立即触发调度任务，且触发类型为misfire
    MISFIRE(I18nUtil.getString("jobconf_trigger_type_misfire"));

    private TriggerTypeEnum(String title){
        this.title = title;
    }
    private String title;
    public String getTitle() {
        return title;
    }

}
