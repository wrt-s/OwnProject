-- Love App in-app notifications (no FCM). Safe to re-run.
CREATE TABLE IF NOT EXISTS app_device (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  device_id VARCHAR(64) NOT NULL,
  platform VARCHAR(32) DEFAULT 'android',
  app_version VARCHAR(32) NULL,
  last_seen_at DATETIME NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  UNIQUE KEY uk_app_device_user_device (user_id, device_id),
  KEY idx_app_device_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS app_notification (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  body VARCHAR(500) NULL,
  type VARCHAR(32) NULL,
  order_no VARCHAR(64) NULL,
  read_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NULL,
  KEY idx_app_notif_user_unread (user_id, read_flag, id),
  KEY idx_app_notif_user_created (user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
