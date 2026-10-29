-- 赚币任务
CREATE TABLE IF NOT EXISTS reward_task (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  family_id BIGINT NOT NULL DEFAULT 1,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  coin_reward INT NOT NULL DEFAULT 10,
  enabled TINYINT NOT NULL DEFAULT 1,
  require_confirm TINYINT NOT NULL DEFAULT 0,
  daily_limit INT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 100,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_family (family_id),
  KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reward_task_claim (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  family_id BIGINT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  coins_awarded INT NOT NULL DEFAULT 0,
  remark VARCHAR(500) NULL,
  confirmed_by BIGINT NULL,
  confirmed_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_task_user_day (task_id, user_id, created_at),
  KEY idx_family_status (family_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
