package com.flowcontrol.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时监控记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "monitor_record")
public class MonitorRecord extends BaseEntity {

    /** 设备ID */
    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    /** 设备名称 */
    @Column(name = "device_name", length = 128)
    private String deviceName;

    /** 流量值（单位: MB） */
    @Column(name = "traffic_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal trafficValue;

    /** 流量速率（单位: Mbps） */
    @Column(name = "traffic_rate", precision = 18, scale = 4)
    private BigDecimal trafficRate;

    /** 记录时间 */
    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    /** 状态: NORMAL / ABNORMAL */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "NORMAL";

    /** 异常标记: 0-正常 / 1-异常 */
    @Column(name = "anomaly_flag", nullable = false)
    private Integer anomalyFlag = 0;

    /** 异常描述 */
    @Column(name = "anomaly_desc", length = 512)
    private String anomalyDesc;
}
