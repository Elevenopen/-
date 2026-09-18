package com.flowcontrol.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务器峰值记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "server_peak")
public class ServerPeak extends BaseEntity {

    /** 服务器名称 */
    @Column(name = "server_name", nullable = false, length = 128)
    private String serverName;

    /** 峰值时间 */
    @Column(name = "peak_time", nullable = false)
    private LocalDateTime peakTime;

    /** CPU使用率（百分比） */
    @Column(name = "cpu_usage", nullable = false, precision = 6, scale = 2)
    private BigDecimal cpuUsage;

    /** 内存使用率（百分比） */
    @Column(name = "memory_usage", nullable = false, precision = 6, scale = 2)
    private BigDecimal memoryUsage;

    /** 磁盘使用率（百分比） */
    @Column(name = "disk_usage", precision = 6, scale = 2)
    private BigDecimal diskUsage;

    /** 入流量（MB） */
    @Column(name = "network_in", precision = 18, scale = 4)
    private BigDecimal networkIn;

    /** 出流量（MB） */
    @Column(name = "network_out", precision = 18, scale = 4)
    private BigDecimal networkOut;

    /** 告警阈值 */
    @Column(name = "threshold", nullable = false, precision = 6, scale = 2)
    private BigDecimal threshold = new BigDecimal("80.00");

    /** 告警标记: 0-未告警 / 1-已告警 */
    @Column(name = "alert_flag", nullable = false)
    private Integer alertFlag = 0;

    /** 告警级别: WARNING / CRITICAL */
    @Column(name = "alert_level", length = 16)
    private String alertLevel;

    /** 备注说明 */
    @Column(name = "description", length = 512)
    private String description;
}
