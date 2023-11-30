package com.xxl.job.core.enums;

/**
 * 注册配置
 * Created by xuxueli on 17/5/10.
 */
public class RegistryConfig {

    /**
     * 心跳超时时间 30s
     */
    public static final int BEAT_TIMEOUT = 30;
    /**
     * 剔除服务机器， 90s
     */
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType{ EXECUTOR, ADMIN }

}
