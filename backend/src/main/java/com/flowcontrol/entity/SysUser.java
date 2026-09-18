package com.flowcontrol.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * 系统用户实体（同时实现 Spring Security UserDetails）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity implements UserDetails {

    /** 用户名（唯一） */
    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    /** 密码（BCrypt加密存储） */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /** 昵称 */
    @Column(name = "nickname", length = 64)
    private String nickname;

    /** 邮箱 */
    @Column(name = "email", length = 128)
    private String email;

    /** 手机号 */
    @Column(name = "phone", length = 32)
    private String phone;

    /** 头像URL */
    @Column(name = "avatar", length = 512)
    private String avatar;

    /** 角色: ADMIN / USER */
    @Column(name = "role", nullable = false, length = 16)
    private String role = "USER";

    /** 状态: 1-正常 / 0-禁用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 最后登录IP */
    @Column(name = "last_login_ip", length = 64)
    private String lastLoginIp;

    /** 最后登录时间 */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    // ===== UserDetails 接口实现 =====

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() { return true; }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() { return status != null && status == 1; }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    @JsonIgnore
    public boolean isEnabled() { return status != null && status == 1; }
}
