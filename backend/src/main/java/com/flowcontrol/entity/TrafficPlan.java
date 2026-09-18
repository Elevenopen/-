package com.flowcontrol.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 流量规划实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "traffic_plan")
public class TrafficPlan extends BaseEntity {

    /** 规划名称 */
    @Column(name = "plan_name", nullable = false, length = 128)
    private String planName;

    /** 目标带宽（如: 100Mbps） */
    @Column(name = "target_bandwidth", nullable = false, length = 32)
    private String targetBandwidth;

    /** 优先级: HIGH / MEDIUM / LOW */
    @Column(name = "priority", nullable = false, length = 16)
    private String priority;

    /** 规划描述 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 计划开始时间 */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /** 计划结束时间 */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /** 状态: ACTIVE / INACTIVE / EXPIRED */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    /** 创建人ID */
    @Column(name = "create_user_id", nullable = false)
    private Long createUserId;

    /** 创建人用户名（冗余字段，便于展示） */
    @Column(name = "create_username", length = 64)
    private String createUsername;
}
