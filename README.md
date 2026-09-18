校园网络流量检测与可视化系统

针对高校校园网络规模扩张带来的运维问题——传统人工巡检效率低、流量监控滞后、故障定位难，以及商用网管软件成本高、定制化系统适配性差等问题，设计并实现了一套基于SpringBoot+Vue3前后端分离架构的校园网络流量管理系统。系统核心围绕“可视化+智能化”展开：后端用SpringBoot2.7构建RESTful API，集成Spring Security+JWT实现双角色（管理员/普通用户）权限控制，MySQL8.0存储流量规划、实时监控、设备统计等7类业务数据，Redis做缓存与限流；前端基于Vue3+Element Plus+Vite开发，用ECharts实现流量趋势、设备分布、网络稳定性等多维度可视化。功能上覆盖流量规划管理（支持时段/用户分组配额）、实时监控（异常流量高亮告警）、网络稳定性分析（可用率/延迟/丢包率仪表盘）、设备使用统计（用户端仅看自有设备）、服务器峰值预警（CPU/内存超阈值提醒）、公告管理（富文本+Redis缓存）六大核心模块，还创新性地集成了AI智能助手，兼容DeepSeek/OpenAI等多模型，能结合实时数据生成运维报告。

功能特性

- 用户认证与权限：ADMIN / USER 双角色，JWT 无状态认证，登录限流
- 流量规划管理：带宽配额、优先级、时段、状态筛选、Excel 导出
- 实时监控：设备流量记录、异常高亮告警
- 网络稳定性分析：可用率、延迟、丢包率、仪表盘
- 设备使用统计：PC / 移动 / 平板 / IOT / 服务器分类，用户仅看自有设备
- 服务器峰值监控：CPU / 内存 / 磁盘阈值告警
- 公告管理：富文本、置顶、Redis 缓存
- AI 智能助手：兼容 DeepSeek / OpenAI / Doubao 等，基于实时数据生成运维分析
- 数据可视化：ECharts 图表

技术栈
后端：Spring Boot 2.7.18、Spring Security、JWT、MySQL 8.0、Redis 7.0、Spring Data JPA、WebFlux WebClient、Apache POI  
前端：Vue 3.3.8、Vite 5.0.6、Element Plus、ECharts、Pinia、Vue Router、Axios  
部署：Nginx、Spring Boot 内嵌 Tomcat
