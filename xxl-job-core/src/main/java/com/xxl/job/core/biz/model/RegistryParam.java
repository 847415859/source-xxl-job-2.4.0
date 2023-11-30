package com.xxl.job.core.biz.model;

import java.io.Serializable;

/**
 * 注册参数
 *
 * Created by xuxueli on 2017-05-10 20:22:42
 */
public class RegistryParam implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 分组 执行器名称
     */
    private String registryGroup;
    /**
     * appname
     */
    private String registryKey;
    /**
     * 调度中心地址
     */
    private String registryValue;

    public RegistryParam(){}
    public RegistryParam(String registryGroup, String registryKey, String registryValue) {
        this.registryGroup = registryGroup;
        this.registryKey = registryKey;
        this.registryValue = registryValue;
    }

    public String getRegistryGroup() {
        return registryGroup;
    }

    public void setRegistryGroup(String registryGroup) {
        this.registryGroup = registryGroup;
    }

    public String getRegistryKey() {
        return registryKey;
    }

    public void setRegistryKey(String registryKey) {
        this.registryKey = registryKey;
    }

    public String getRegistryValue() {
        return registryValue;
    }

    public void setRegistryValue(String registryValue) {
        this.registryValue = registryValue;
    }

    @Override
    public String toString() {
        return "RegistryParam{" +
                "registryGroup='" + registryGroup + '\'' +
                ", registryKey='" + registryKey + '\'' +
                ", registryValue='" + registryValue + '\'' +
                '}';
    }
}
