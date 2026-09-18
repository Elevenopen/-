package com.flowcontrol.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备使用统计实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "device_usage")
public class DeviceUsage extends BaseEntity {

    /** 设备ID（唯一标识） */
    @Column(name = "device_id", nullable = false, unique = true, length = 64)
    private String deviceId;

    /** 设备名称 */
    @Column(name = "device_name", nullable = false, length = 128)
    private String deviceName;

    /** 设备类型: PC / MOBILE / TABLET / IOT / SERVER */
    @Column(name = "device_type", length = 32)
    private String deviceType;

    /** MAC地址 */
    @Column(name = "device_mac", length = 64)
    private String deviceMac;

    /** IP地址 */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** 在线时长（单位: 秒） */
    @Column(name = "online_duration", nullable = false)
    private Long onlineDuration = 0L;

    /** 流量消耗（单位: MB） */
    @Column(name = "traffic_used", nullable = false, precision = 18, scale = 4)
    private BigDecimal trafficUsed = BigDecimal.ZERO;

    /** 峰值速率（Mbps） */
    @Column(name = "peak_speed", precision = 10, scale = 2)
    private BigDecimal peakSpeed;

    /** 最后活跃时间 */
    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    /** 首次接入时间 */
    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    /** 状态: ONLINE / OFFLINE */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "ONLINE";
}
