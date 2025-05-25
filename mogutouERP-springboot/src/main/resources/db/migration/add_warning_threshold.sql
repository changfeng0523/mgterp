-- 为inventory表添加预警阈值字段
ALTER TABLE inventory ADD COLUMN warning_threshold INT DEFAULT 5 COMMENT '库存预警阈值，当库存数量低于此值时显示预警';

-- 为现有记录设置默认预警阈值
UPDATE inventory SET warning_threshold = 5 WHERE warning_threshold IS NULL;
