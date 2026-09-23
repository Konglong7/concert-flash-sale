-- ============================================================
-- 演唱会抢票秒杀系统 数据库初始化脚本
-- 执行方式：mysql -uroot -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS flash_sale DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE flash_sale;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`   VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `id_card`    VARCHAR(30)  DEFAULT NULL COMMENT '身份证号',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户表';

-- 2. 演出表
CREATE TABLE IF NOT EXISTS `concert` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '演出ID',
    `name`            VARCHAR(100) NOT NULL COMMENT '演出名称',
    `venue`           VARCHAR(100) NOT NULL COMMENT '场馆',
    `show_time`       DATETIME     NOT NULL COMMENT '开演时间',
    `sale_start_time` DATETIME     NOT NULL COMMENT '开售时间',
    `sale_end_time`   DATETIME     NOT NULL COMMENT '停售时间',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0未开售 1销售中 2已结束',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_sale_time` (`sale_start_time`, `sale_end_time`)
) ENGINE = InnoDB COMMENT = '演出表';

-- 3. 票档表
CREATE TABLE IF NOT EXISTS `ticket_tier` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '票档ID',
    `concert_id`     BIGINT        NOT NULL COMMENT '演出ID',
    `name`           VARCHAR(50)   NOT NULL COMMENT '票档名称(内场/看台等)',
    `price`          DECIMAL(10,2) NOT NULL COMMENT '票价',
    `total_stock`    INT           NOT NULL COMMENT '总库存',
    `stock`          INT           NOT NULL COMMENT '剩余库存',
    `limit_per_user` INT           NOT NULL DEFAULT 1 COMMENT '每人限购数量',
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_concert` (`concert_id`)
) ENGINE = InnoDB COMMENT = '票档表';

-- 4. 订单表
CREATE TABLE IF NOT EXISTS `orders` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`   VARCHAR(32)   NOT NULL COMMENT '订单号',
    `user_id`    BIGINT        NOT NULL COMMENT '用户ID',
    `concert_id` BIGINT        NOT NULL COMMENT '演出ID',
    `tier_id`    BIGINT        NOT NULL COMMENT '票档ID',
    `id_card`    VARCHAR(30)   NOT NULL COMMENT '观演人身份证',
    `price`      DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status`     TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0待支付 1已支付 2已取消 3超时',
    `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `paid_at`    DATETIME      DEFAULT NULL COMMENT '支付时间',
    `expire_at`  DATETIME      NOT NULL COMMENT '支付截止时间(创建后5分钟)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`, `status`)
) ENGINE = InnoDB COMMENT = '订单表';

-- 5. 限购表
CREATE TABLE IF NOT EXISTS `user_ticket_limit` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id`         BIGINT   NOT NULL COMMENT '用户ID',
    `concert_id`      BIGINT   NOT NULL COMMENT '演出ID',
    `purchased_count` INT      NOT NULL DEFAULT 0 COMMENT '已购数量',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_concert` (`user_id`, `concert_id`)
) ENGINE = InnoDB COMMENT = '用户限购表';
