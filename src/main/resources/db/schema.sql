-- Single source of truth for the octopus_demo_system schema.
-- Consumed by both docker-compose (container init) and Testcontainers.
-- The init client (docker-entrypoint -> mysql) picks latin1 from the C locale; without this
-- the UTF-8 seed rows below are converted latin1->utf8mb4 and stored double-encoded.
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS octopus_demo_system
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE octopus_demo_system;

CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname      VARCHAR(64)           DEFAULT NULL,
    gender        VARCHAR(16)           DEFAULT NULL,
    email         VARCHAR(128)          DEFAULT NULL,
    phone         VARCHAR(32)           DEFAULT NULL,
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0=disabled, 1=enabled',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(64) NOT NULL,
    code       VARCHAR(64) NOT NULL,
    status     TINYINT     NOT NULL DEFAULT 1 COMMENT '0=disabled, 1=enabled',
    remark     VARCHAR(255)         DEFAULT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_menu (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id  BIGINT       NOT NULL DEFAULT 0,
    name       VARCHAR(64)  NOT NULL,
    type       VARCHAR(16)  NOT NULL COMMENT 'catalog/menu/button',
    path       VARCHAR(255)          DEFAULT NULL,
    component  VARCHAR(255)          DEFAULT NULL,
    icon       VARCHAR(64)           DEFAULT NULL,
    perms      VARCHAR(128)          DEFAULT NULL,
    sort       INT          NOT NULL DEFAULT 0,
    visible    TINYINT      NOT NULL DEFAULT 1 COMMENT '0=hidden, 1=visible',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '0=disabled, 1=enabled',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_menu_parent_id (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    config_key   VARCHAR(128) NOT NULL,
    config_value VARCHAR(1024)         DEFAULT NULL,
    remark       VARCHAR(255)          DEFAULT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    dict_type  VARCHAR(100) NOT NULL,
    dict_name  VARCHAR(100) NOT NULL,
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '0=disabled, 1=enabled',
    remark     VARCHAR(255)          DEFAULT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    dict_type  VARCHAR(100) NOT NULL,
    dict_label VARCHAR(100) NOT NULL,
    dict_value VARCHAR(100) NOT NULL,
    sort       INT          NOT NULL DEFAULT 0,
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '0=disabled, 1=enabled',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_dict_data_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_sys_user_role_role_id (role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    KEY idx_sys_role_menu_menu_id (menu_id),
    CONSTRAINT fk_sys_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_sys_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_job (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    job_name   VARCHAR(64)  NOT NULL,
    job_group  VARCHAR(64)  NOT NULL DEFAULT 'DEFAULT',
    bean_name  VARCHAR(128) NOT NULL,
    cron       VARCHAR(128) NOT NULL,
    params     VARCHAR(512)          DEFAULT NULL,
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '0=paused, 1=enabled',
    remark     VARCHAR(255)          DEFAULT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_job_name_group (job_name, job_group)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_job_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    job_id       BIGINT                DEFAULT NULL,
    job_name     VARCHAR(64)           DEFAULT NULL,
    status       TINYINT      NOT NULL COMMENT '0=failure, 1=success',
    message      VARCHAR(500)          DEFAULT NULL,
    started_at   DATETIME     NOT NULL,
    finished_at  DATETIME              DEFAULT NULL,
    duration_ms  BIGINT                DEFAULT NULL,
    trigger_type VARCHAR(16)  NOT NULL DEFAULT 'cron' COMMENT 'cron | manual',
    PRIMARY KEY (id),
    KEY idx_sys_job_log_job_started (job_id, started_at),
    KEY idx_sys_job_log_status_started (status, started_at),
    KEY idx_sys_job_log_started (started_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_login_log (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(64)           DEFAULT NULL,
    ip         VARCHAR(128)          DEFAULT NULL,
    user_agent VARCHAR(512)          DEFAULT NULL,
    status     TINYINT      NOT NULL COMMENT '0=failure, 1=success',
    message    VARCHAR(255)          DEFAULT NULL,
    login_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_login_log_username (username),
    KEY idx_sys_login_log_login_at (login_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(128) NOT NULL,
    module       VARCHAR(64)           DEFAULT NULL,
    http_method  VARCHAR(16)           DEFAULT NULL,
    request_uri  VARCHAR(255)          DEFAULT NULL,
    params       TEXT,
    `operator`   VARCHAR(64)           DEFAULT NULL,
    ip           VARCHAR(128)          DEFAULT NULL,
    status       TINYINT      NOT NULL COMMENT '0=failure, 1=success',
    error_msg    VARCHAR(500)          DEFAULT NULL,
    duration_ms  BIGINT                DEFAULT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sys_oper_log_operator (`operator`),
    KEY idx_sys_oper_log_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Built-in sample jobs; paused by default so they produce no log noise until enabled.
INSERT INTO sys_job (job_name, job_group, bean_name, cron, params, status, remark)
VALUES ('demoHeartbeatJob', 'DEFAULT', 'demoHeartbeatJob', '0/30 * * * * ?', NULL, 0, '心跳示例任务，每次执行写入 heartbeat'),
       ('jobLogCleanupJob', 'DEFAULT', 'jobLogCleanupJob', '0 0 3 * * ?', NULL, 0, '执行日志清理，保留天数取 sys.job.log.retention-days'),
       ('auditLogCleanupJob', 'DEFAULT', 'auditLogCleanupJob', '0 30 3 * * ?', NULL, 0, '审计日志清理，保留天数取 sys.log.retention-days')
ON DUPLICATE KEY UPDATE remark = VALUES(remark);

-- Seed roles (users and password hashes are created by BootstrapDataInitializer).
INSERT INTO sys_role (id, name, code, status, remark)
VALUES (1, '管理员', 'admin', 1, '系统管理员，拥有全部菜单权限'),
       (2, '普通用户', 'guest', 1, '访客用户，仅拥有用户管理查看权限')
ON DUPLICATE KEY UPDATE name = VALUES(name), remark = VALUES(remark);

-- Runtime parameters; every value is a string interpreted by its consumer.
INSERT INTO sys_config (config_key, config_value, remark)
VALUES ('sys.site.title', 'Octopus 管理平台', '站点标题，展示在顶部品牌与浏览器标签页'),
       ('sys.log.retention-days', '30', '登录与操作日志保留天数，auditLogCleanupJob 读取，非法值回退 30')
ON DUPLICATE KEY UPDATE remark = VALUES(remark);

-- Seed menus: catalog -> menus -> button permissions.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, icon, perms, sort, visible, status)
VALUES (1, 0, '系统管理', 'catalog', '/system', NULL, 'Setting', NULL, 1, 1, 1),
       (5, 0, '系统配置', 'catalog', '/config', NULL, 'Tools', NULL, 2, 1, 1),
       (6, 5, '参数设置', 'menu', '/config/params', 'config/params/index', 'Document', 'sys:config:list', 1, 1, 1),
       (2, 1, '用户管理', 'menu', '/system/users', 'system/users/index', 'User', 'sys:user:list', 1, 1, 1),
       (3, 1, '角色管理', 'menu', '/system/roles', 'system/roles/index', 'UserFilled', 'sys:role:list', 2, 1, 1),
       (4, 1, '菜单管理', 'menu', '/system/menus', 'system/menus/index', 'Menu', 'sys:menu:list', 3, 1, 1),
       (11, 2, '用户新增', 'button', NULL, NULL, NULL, 'sys:user:add', 1, 1, 1),
       (12, 2, '用户删除', 'button', NULL, NULL, NULL, 'sys:user:delete', 2, 1, 1),
       (13, 2, '用户修改', 'button', NULL, NULL, NULL, 'sys:user:update', 3, 1, 1),
       (21, 3, '角色新增', 'button', NULL, NULL, NULL, 'sys:role:add', 1, 1, 1),
       (22, 3, '角色删除', 'button', NULL, NULL, NULL, 'sys:role:delete', 2, 1, 1),
       (23, 3, '角色修改', 'button', NULL, NULL, NULL, 'sys:role:update', 3, 1, 1),
       (31, 4, '菜单新增', 'button', NULL, NULL, NULL, 'sys:menu:add', 1, 1, 1),
       (32, 4, '菜单删除', 'button', NULL, NULL, NULL, 'sys:menu:delete', 2, 1, 1),
       (33, 4, '菜单修改', 'button', NULL, NULL, NULL, 'sys:menu:update', 3, 1, 1),
       (61, 6, '参数新增', 'button', NULL, NULL, NULL, 'sys:config:add', 1, 1, 1),
       (62, 6, '参数删除', 'button', NULL, NULL, NULL, 'sys:config:delete', 2, 1, 1),
       (63, 6, '参数修改', 'button', NULL, NULL, NULL, 'sys:config:edit', 3, 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), path = VALUES(path), component = VALUES(component),
                        icon = VALUES(icon), perms = VALUES(perms), sort = VALUES(sort);

-- Data dictionary menu under the 系统配置 catalog, with its button permissions.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, icon, perms, sort, visible, status)
VALUES (7, 5, '数据字典', 'menu', '/config/dicts', 'config/dicts/index', 'Notebook', 'sys:dict:list', 2, 1, 1),
       (51, 7, '字典新增', 'button', NULL, NULL, NULL, 'sys:dict:add', 1, 1, 1),
       (52, 7, '字典删除', 'button', NULL, NULL, NULL, 'sys:dict:delete', 2, 1, 1),
       (53, 7, '字典修改', 'button', NULL, NULL, NULL, 'sys:dict:edit', 3, 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), path = VALUES(path), component = VALUES(component),
                        icon = VALUES(icon), perms = VALUES(perms), sort = VALUES(sort);

-- System monitor catalog holding the scheduled job page and its buttons.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, icon, perms, sort, visible, status)
VALUES (8, 0, '系统监控', 'catalog', '/monitor', NULL, 'Monitor', NULL, 3, 1, 1),
       (9, 8, '定时任务', 'menu', '/monitor/jobs', 'monitor/jobs/index', 'Timer', 'sys:job:list', 1, 1, 1),
       (91, 9, '任务新增', 'button', NULL, NULL, NULL, 'sys:job:add', 1, 1, 1),
       (92, 9, '任务删除', 'button', NULL, NULL, NULL, 'sys:job:delete', 2, 1, 1),
       (93, 9, '任务修改', 'button', NULL, NULL, NULL, 'sys:job:edit', 3, 1, 1),
       (94, 9, '立即执行', 'button', NULL, NULL, NULL, 'sys:job:run', 4, 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), path = VALUES(path), component = VALUES(component),
                        icon = VALUES(icon), perms = VALUES(perms), sort = VALUES(sort);

-- Operation log page under the 系统监控 catalog.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, icon, perms, sort, visible, status)
VALUES (102, 8, '操作日志', 'menu', '/monitor/oper-logs', 'monitor/oper-logs/index', 'Tickets', 'sys:operlog:list', 3, 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), path = VALUES(path), component = VALUES(component),
                        icon = VALUES(icon), perms = VALUES(perms), sort = VALUES(sort);

-- Login audit log page under the system monitor catalog.
INSERT INTO sys_menu (id, parent_id, name, type, path, component, icon, perms, sort, visible, status)
VALUES (101, 8, '登录日志', 'menu', '/monitor/login-logs', 'monitor/login-logs/index', 'Document', 'sys:loginlog:list', 2, 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), path = VALUES(path), component = VALUES(component),
                        icon = VALUES(icon), perms = VALUES(perms), sort = VALUES(sort);

-- Seed dictionary: user sex (男/女/未知).
INSERT INTO sys_dict_type (id, dict_type, dict_name, status, remark)
VALUES (1, 'sys_user_sex', '用户性别', 1, '用户性别列表')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), remark = VALUES(remark);

INSERT INTO sys_dict_data (id, dict_type, dict_label, dict_value, sort, status)
VALUES (1, 'sys_user_sex', '男', '1', 1, 1),
       (2, 'sys_user_sex', '女', '2', 2, 1),
       (3, 'sys_user_sex', '未知', '0', 3, 1)
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label), dict_value = VALUES(dict_value),
                        sort = VALUES(sort), status = VALUES(status);

-- admin: all menus and buttons. guest: 系统管理 catalog + 用户管理.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
       (1, 11), (1, 12), (1, 13), (1, 21), (1, 22), (1, 23), (1, 31), (1, 32), (1, 33),
       (1, 51), (1, 52), (1, 53), (1, 61), (1, 62), (1, 63),
       (1, 91), (1, 92), (1, 93), (1, 94), (1, 101), (1, 102),
       (2, 1), (2, 2);

-- ---------------------------------------------------------------------------
-- Quartz 2.3 JDBC JobStore tables (official tables_mysql_innodb.sql).
-- Kept in this same script so containers and Testcontainers share one DDL.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS QRTZ_JOB_DETAILS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(190) NOT NULL,
    JOB_GROUP         VARCHAR(190) NOT NULL,
    DESCRIPTION       VARCHAR(250)          DEFAULT NULL,
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        VARCHAR(1)   NOT NULL,
    IS_NONCONCURRENT  VARCHAR(1)   NOT NULL,
    IS_UPDATE_DATA    VARCHAR(1)   NOT NULL,
    REQUESTS_RECOVERY VARCHAR(1)   NOT NULL,
    JOB_DATA          BLOB                  DEFAULT NULL,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_TRIGGERS (
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(190) NOT NULL,
    TRIGGER_GROUP  VARCHAR(190) NOT NULL,
    JOB_NAME       VARCHAR(190) NOT NULL,
    JOB_GROUP      VARCHAR(190) NOT NULL,
    DESCRIPTION    VARCHAR(250)          DEFAULT NULL,
    NEXT_FIRE_TIME BIGINT(13)            DEFAULT NULL,
    PREV_FIRE_TIME BIGINT(13)            DEFAULT NULL,
    PRIORITY       INT                   DEFAULT NULL,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT(13)   NOT NULL,
    END_TIME       BIGINT(13)            DEFAULT NULL,
    CALENDAR_NAME  VARCHAR(190)          DEFAULT NULL,
    MISFIRE_INSTR  SMALLINT(2)           DEFAULT NULL,
    JOB_DATA       BLOB                  DEFAULT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_triggers_job FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
        REFERENCES QRTZ_JOB_DETAILS (SCHED_NAME, JOB_NAME, JOB_GROUP)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(190) NOT NULL,
    TRIGGER_GROUP   VARCHAR(190) NOT NULL,
    REPEAT_COUNT    BIGINT(7)    NOT NULL,
    REPEAT_INTERVAL BIGINT(12)   NOT NULL,
    TIMES_TRIGGERED BIGINT(10)   NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_simple_triggers FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_CRON_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(190) NOT NULL,
    TRIGGER_GROUP   VARCHAR(190) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80)           DEFAULT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_cron_triggers FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_SIMPROP_TRIGGERS (
    SCHED_NAME    VARCHAR(120)   NOT NULL,
    TRIGGER_NAME  VARCHAR(190)   NOT NULL,
    TRIGGER_GROUP VARCHAR(190)   NOT NULL,
    STR_PROP_1    VARCHAR(512)            DEFAULT NULL,
    STR_PROP_2    VARCHAR(512)            DEFAULT NULL,
    STR_PROP_3    VARCHAR(512)            DEFAULT NULL,
    INT_PROP_1    INT                     DEFAULT NULL,
    INT_PROP_2    INT                     DEFAULT NULL,
    LONG_PROP_1   BIGINT                  DEFAULT NULL,
    LONG_PROP_2   BIGINT                  DEFAULT NULL,
    DEC_PROP_1    NUMERIC(13, 4)          DEFAULT NULL,
    DEC_PROP_2    NUMERIC(13, 4)          DEFAULT NULL,
    BOOL_PROP_1   VARCHAR(1)              DEFAULT NULL,
    BOOL_PROP_2   VARCHAR(1)              DEFAULT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_simprop_triggers FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_BLOB_TRIGGERS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(190) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    BLOB_DATA     BLOB                  DEFAULT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    INDEX (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    CONSTRAINT fk_qrtz_blob_triggers FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_CALENDARS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(190) NOT NULL,
    CALENDAR      BLOB         NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(190) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_FIRED_TRIGGERS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(190) NOT NULL,
    TRIGGER_GROUP     VARCHAR(190) NOT NULL,
    INSTANCE_NAME     VARCHAR(190) NOT NULL,
    FIRED_TIME        BIGINT(13)   NOT NULL,
    SCHED_TIME        BIGINT(13)   NOT NULL,
    PRIORITY          INT          NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(190)          DEFAULT NULL,
    JOB_GROUP         VARCHAR(190)          DEFAULT NULL,
    IS_NONCONCURRENT  VARCHAR(1)            DEFAULT NULL,
    REQUESTS_RECOVERY VARCHAR(1)            DEFAULT NULL,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_SCHEDULER_STATE (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(190) NOT NULL,
    LAST_CHECKIN_TIME BIGINT(13)   NOT NULL,
    CHECKIN_INTERVAL  BIGINT(13)   NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
) ENGINE = InnoDB;

INSERT IGNORE INTO QRTZ_LOCKS (SCHED_NAME, LOCK_NAME)
VALUES ('quartzScheduler', 'TRIGGER_ACCESS'),
       ('quartzScheduler', 'JOB_ACCESS'),
       ('quartzScheduler', 'CALENDAR_ACCESS'),
       ('quartzScheduler', 'STATE_ACCESS'),
       ('quartzScheduler', 'MISFIRE_ACCESS');
