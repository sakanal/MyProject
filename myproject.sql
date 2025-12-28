DROP TABLE IF EXISTS `fail_picture`;
CREATE TABLE `fail_picture`
(
    `id`         bigint NOT NULL AUTO_INCREMENT,
    `picture_id` bigint       DEFAULT NULL,
    `user_id`    bigint       DEFAULT NULL,
    `user_name`  varchar(255) DEFAULT NULL,
    `title`      varchar(255) DEFAULT NULL,
    `page_count` int          DEFAULT NULL,
    `src`        varchar(255) DEFAULT NULL,
    `status`     int(10)      DEFAULT '0' COMMENT '0- 默认状态 1-下载成功 2-下载失败 3-重复图片',
    `type`       varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS `picture`;
CREATE TABLE `picture`
(
    `id`         bigint NOT NULL AUTO_INCREMENT,
    `picture_id` bigint       DEFAULT NULL,
    `user_id`    bigint       DEFAULT NULL,
    `user_name`  varchar(255) DEFAULT NULL,
    `title`      varchar(255) DEFAULT NULL,
    `page_count` int          DEFAULT NULL,
    `src`        varchar(500) DEFAULT NULL,
    `status`     int(10)      DEFAULT '0' COMMENT '0- 默认状态1-下载成功 2-下载失败 3-重复图片',
    `type`       varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`        bigint NOT NULL AUTO_INCREMENT,
    `user_id`   bigint       DEFAULT NULL,
    `user_name` varchar(255) DEFAULT NULL,
    `sort`      int          DEFAULT NULL,
    `type`      varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;


CREATE TABLE `my_lock`
(
    `lock_name`      varchar(255) NOT NULL,
    `available_time` bigint       NOT NULL COMMENT '可用时间，超过该时间则无效',
    PRIMARY KEY (`lock_name`)
) ENGINE = InnoDB;