package com.xxl.job.admin.core.scheduler;

import com.xxl.job.admin.core.util.I18nUtil;

/**
 * 调度类型
 * @author xuxueli 2020-10-29 21:11:23
 */
public enum ScheduleTypeEnum {

    /**
     * 当前调度类型禁止启动
     */
    NONE(I18nUtil.getString("schedule_type_none")),

    /**
     * 通过 Cron 表达式调度
     * schedule by cron
     */
    CRON(I18nUtil.getString("schedule_type_cron")),

    /**
     * 通过固定的频率调度，单位（秒）
     * schedule by fixed rate (in seconds)
     */
    FIX_RATE(I18nUtil.getString("schedule_type_fix_rate")),

    /**
     * schedule by fix delay (in seconds)， after the last time
     */
    /*FIX_DELAY(I18nUtil.getString("schedule_type_fix_delay"))*/;

    private String title;

    ScheduleTypeEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultItem){
        for (ScheduleTypeEnum item: ScheduleTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }

}
