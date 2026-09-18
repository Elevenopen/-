package com.flowcontrol.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 公告管理实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "announcement")
public class Announcement extends BaseEntity {

    /** 公告标题 */
    @Column(name = "title", nullable = false, length = 256)
    private String title;

    /** 公告内容（富文本） */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 公告摘要 */
    @Column(name = "summary", length = 512)
    private String summary;

    /** 优先级: TOP-置顶 / NORMAL-普通 */
    @Column(name = "priority", nullable = false, length = 16)
    private String priority = "NORMAL";

    /** 发布人 */
    @Column(name = "publisher", nullable = false, length = 64)
    private String publisher;

    /** 发布时间 */
    @Column(name = "publish_time", nullable = false)
    private LocalDateTime publishTime;

    /** 过期时间（NULL表示永不过期） */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /** 状态: PUBLISHED / DRAFT / ARCHIVED */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "PUBLISHED";

    /** 浏览次数 */
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    /** 创建人ID */
    @Column(name = "create_user_id")
    private Long createUserId;
}
