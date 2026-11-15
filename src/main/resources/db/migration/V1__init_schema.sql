-- =====================================================
-- Upbit Clone Lite MVP - Final DDL (MySQL 8.0+)
-- =====================================================

-- [1] users
CREATE TABLE `users` (
                         `user_id` BIGINT NOT NULL AUTO_INCREMENT,
                         `email` VARCHAR(100) NOT NULL,
                         `user_name` VARCHAR(100) NOT NULL,
                         `status` ENUM('ACTIVE','INACTIVE','DEPRECATED') NOT NULL DEFAULT 'ACTIVE',
                         `password_hash` VARCHAR(255) NOT NULL,
                         `created_at` TIMESTAMP(3) NOT NULL,
                         `updated_at` TIMESTAMP(3) NOT NULL,
                         PRIMARY KEY (`user_id`),
                         UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [2] asset
CREATE TABLE `asset` (
                         `asset_id` BIGINT NOT NULL AUTO_INCREMENT,
                         `symbol` VARCHAR(10) NOT NULL,
                         `name` VARCHAR(100) NOT NULL,
                         `decimals` TINYINT NOT NULL,
                         `status` ENUM('ACTIVE','INACTIVE','DEPRECATED') NOT NULL DEFAULT 'ACTIVE',
                         `created_at` TIMESTAMP(3) NOT NULL,
                         `updated_at` TIMESTAMP(3) NOT NULL,
                         PRIMARY KEY (`asset_id`),
                         UNIQUE KEY `uk_asset_symbol` (`symbol`),
                         CONSTRAINT `chk_asset_decimals` CHECK (`decimals` >= 0 AND `decimals` <= 8)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [3] market (fixed tick)
CREATE TABLE `market` (
                          `market_id` BIGINT NOT NULL AUTO_INCREMENT,
                          `base_asset_id` BIGINT NOT NULL,
                          `quote_asset_id` BIGINT NOT NULL,
                          `market_code` VARCHAR(21) NOT NULL,
                          `status` ENUM('ACTIVE','INACTIVE','DEPRECATED') NOT NULL DEFAULT 'ACTIVE',
                          `min_order_quote` DECIMAL(30,8) NOT NULL,
                          `tick_size` DECIMAL(30,8) NOT NULL,
                          `created_at` TIMESTAMP(3) NOT NULL,
                          `updated_at` TIMESTAMP(3) NOT NULL,
                          PRIMARY KEY (`market_id`),
                          UNIQUE KEY `uk_market_code` (`market_code`),
                          CONSTRAINT `fk_market_base` FOREIGN KEY (`base_asset_id`) REFERENCES `asset`(`asset_id`),
                          CONSTRAINT `fk_market_quote` FOREIGN KEY (`quote_asset_id`) REFERENCES `asset`(`asset_id`),
                          CONSTRAINT `chk_market_diff_assets` CHECK (`base_asset_id` <> `quote_asset_id`),
                          CONSTRAINT `chk_market_positive` CHECK (`min_order_quote` >= 0 AND `tick_size` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [4] wallet (optimistic lock)
CREATE TABLE `wallet` (
                          `wallet_id` BIGINT NOT NULL AUTO_INCREMENT,
                          `user_id` BIGINT NOT NULL,
                          `asset_id` BIGINT NOT NULL,
                          `available_balance` DECIMAL(30,8) NOT NULL DEFAULT 0,
                          `locked_balance` DECIMAL(30,8) NOT NULL DEFAULT 0,
                          `version` BIGINT NOT NULL DEFAULT 0,
                          `created_at` TIMESTAMP(3) NOT NULL,
                          `updated_at` TIMESTAMP(3) NOT NULL,
                          PRIMARY KEY (`wallet_id`),
                          UNIQUE KEY `uk_wallet_user_asset` (`user_id`,`asset_id`),
                          CONSTRAINT `fk_wallet_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`),
                          CONSTRAINT `fk_wallet_asset` FOREIGN KEY (`asset_id`) REFERENCES `asset`(`asset_id`),
                          CONSTRAINT `chk_wallet_available_nonneg` CHECK (`available_balance` >= 0),
                          CONSTRAINT `chk_wallet_locked_nonneg` CHECK (`locked_balance` >= 0),
                          CONSTRAINT `chk_wallet_version` CHECK (`version` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [5] orders (no remaining stored)
CREATE TABLE `orders` (
                          `order_id` BIGINT NOT NULL AUTO_INCREMENT,
                          `market_id` BIGINT NOT NULL,
                          `user_id` BIGINT NOT NULL,
                          `client_order_id` VARCHAR(150) NOT NULL,
                          `order_key` VARCHAR(64) NOT NULL,
                          `order_side` ENUM('BID','ASK') NOT NULL,
                          `order_type` ENUM('LIMIT','MARKET') NOT NULL,
                          `time_in_force` ENUM('GTC','IOC','FOK') NOT NULL DEFAULT 'GTC',
                          `price` DECIMAL(30,8) NULL,
                          `quantity` DECIMAL(30,8) NULL,
                          `quote_amount` DECIMAL(30,8) NULL,
                          `executed_quantity` DECIMAL(30,8) NOT NULL DEFAULT 0,
                          `executed_quote_amount` DECIMAL(30,8) NOT NULL DEFAULT 0,
                          `status` ENUM('PENDING','OPEN','FILLED','CANCELED') NOT NULL DEFAULT 'PENDING',
                          `cancel_reason` VARCHAR(50) NULL,
                          `created_at` TIMESTAMP(3) NOT NULL,
                          `updated_at` TIMESTAMP(3) NOT NULL,
                          PRIMARY KEY (`order_id`),
                          UNIQUE KEY `uk_orders_client` (`user_id`,`client_order_id`),
                          UNIQUE KEY `uk_orders_order_key` (`order_key`),
                          CONSTRAINT `fk_orders_market` FOREIGN KEY (`market_id`) REFERENCES `market`(`market_id`),
                          CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`),
                          CONSTRAINT `chk_orders_input_pos`
                              CHECK (
                                  (`price` IS NULL OR `price` > 0) AND
                                  (`quantity` IS NULL OR `quantity` > 0) AND
                                  (`quote_amount` IS NULL OR `quote_amount` > 0)
                                  ),
                          CONSTRAINT `chk_orders_exec_nonneg` CHECK (`executed_quantity` >= 0 AND `executed_quote_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [6] trade (immutable)
CREATE TABLE `trade` (
                         `trade_id` BIGINT NOT NULL AUTO_INCREMENT,
                         `market_id` BIGINT NOT NULL,
                         `buy_order_id` BIGINT NOT NULL,
                         `sell_order_id` BIGINT NOT NULL,
                         `trade_key` VARCHAR(128) NOT NULL,
                         `maker_order_side` ENUM('BID','ASK') NOT NULL,
                         `price` DECIMAL(30,8) NOT NULL,
                         `quantity` DECIMAL(30,8) NOT NULL,
                         `quote_amount` DECIMAL(30,8) NOT NULL,
                         `fee_rate` DECIMAL(10,8) NOT NULL DEFAULT 0,
                         `buy_fee_amount` DECIMAL(30,8) NOT NULL DEFAULT 0,
                         `sell_fee_amount` DECIMAL(30,8) NOT NULL DEFAULT 0,
                         `executed_at` TIMESTAMP(3) NOT NULL,
                         PRIMARY KEY (`trade_id`),
                         UNIQUE KEY `uk_trade_trade_key` (`trade_key`),
                         CONSTRAINT `fk_trade_market` FOREIGN KEY (`market_id`) REFERENCES `market`(`market_id`),
                         CONSTRAINT `fk_trade_buy` FOREIGN KEY (`buy_order_id`) REFERENCES `orders`(`order_id`),
                         CONSTRAINT `fk_trade_sell` FOREIGN KEY (`sell_order_id`) REFERENCES `orders`(`order_id`),
                         CONSTRAINT `chk_trade_no_self` CHECK (`buy_order_id` <> `sell_order_id`),
                         CONSTRAINT `chk_trade_positive` CHECK (`price` > 0 AND `quantity` > 0 AND `quote_amount` > 0),
                         CONSTRAINT `chk_trade_fee_nonneg` CHECK (`fee_rate` >= 0 AND `buy_fee_amount` >= 0 AND `sell_fee_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [7] ledger (immutable + idempotency)
CREATE TABLE `ledger` (
                          `ledger_id` BIGINT NOT NULL AUTO_INCREMENT,
                          `wallet_id` BIGINT NOT NULL,
                          `asset_id` BIGINT NOT NULL,
                          `ledger_type` ENUM('ORDER_LOCK','ORDER_UNLOCK','TRADE','CANCEL','DEPOSIT','WITHDRAW','FEE') NOT NULL,
                          `change_type` ENUM('INCREASE','DECREASE') NOT NULL,
                          `amount` DECIMAL(30,8) NOT NULL,
                          `available_before` DECIMAL(30,8) NOT NULL,
                          `available_after` DECIMAL(30,8) NOT NULL,
                          `locked_before` DECIMAL(30,8) NOT NULL,
                          `locked_after` DECIMAL(30,8) NOT NULL,
                          `reference_type` ENUM('ORDER','TRADE','DEPOSIT','WITHDRAW','SYSTEM') NOT NULL,
                          `reference_id` BIGINT NULL,
                          `description` VARCHAR(255) NULL,
                          `idempotency_key` VARCHAR(150) NOT NULL,
                          `created_at` TIMESTAMP(3) NOT NULL,
                          PRIMARY KEY (`ledger_id`),
                          UNIQUE KEY `uk_ledger_idempotency` (`idempotency_key`),
                          CONSTRAINT `fk_ledger_wallet` FOREIGN KEY (`wallet_id`) REFERENCES `wallet`(`wallet_id`),
                          CONSTRAINT `fk_ledger_asset` FOREIGN KEY (`asset_id`) REFERENCES `asset`(`asset_id`),
                          CONSTRAINT `chk_ledger_amount_pos` CHECK (`amount` > 0),
                          CONSTRAINT `chk_ledger_available_nonneg` CHECK (`available_before` >= 0 AND `available_after` >= 0),
                          CONSTRAINT `chk_ledger_locked_nonneg` CHECK (`locked_before` >= 0 AND `locked_after` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [8] command_log (append-only)
CREATE TABLE `command_log` (
                               `command_log_id` BIGINT NOT NULL AUTO_INCREMENT,
                               `command_id` VARCHAR(64) NOT NULL,
                               `command_type` ENUM('PLACE_ORDER','CANCEL_ORDER') NOT NULL,
                               `market_id` BIGINT NOT NULL,
                               `user_id` BIGINT NULL,
                               `client_order_id` VARCHAR(150) NULL,
                               `payload` JSON NOT NULL,
                               `request_hash` VARCHAR(64) NOT NULL,
                               `created_at` TIMESTAMP(3) NOT NULL,
                               PRIMARY KEY (`command_log_id`),
                               UNIQUE KEY `uk_command_log_command_id` (`command_id`),
                               UNIQUE KEY `uk_command_log_user_client_type` (`user_id`, `client_order_id`, `command_type`),
                               KEY `idx_command_log_market_seq` (`market_id`, `command_log_id`),
                               KEY `idx_command_log_user_client` (`user_id`, `client_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [9] event_log (append-only)
CREATE TABLE `event_log` (
                             `event_log_id` BIGINT NOT NULL AUTO_INCREMENT,
                             `command_log_id` BIGINT NOT NULL,
                             `event_id` VARCHAR(64) NOT NULL,
                             `event_type` VARCHAR(32) NOT NULL,
                             `market_id` BIGINT NOT NULL,
                             `order_id` BIGINT NULL,
                             `payload` JSON NOT NULL,
                             `created_at` TIMESTAMP(3) NOT NULL,
                             PRIMARY KEY (`event_log_id`),
                             UNIQUE KEY `uk_event_log_event_id` (`event_id`),
                             KEY `idx_event_log_market_seq` (`market_id`, `event_log_id`),
                             KEY `idx_event_log_command` (`command_log_id`),
                             CONSTRAINT `fk_event_log_command_log` FOREIGN KEY (`command_log_id`) REFERENCES `command_log`(`command_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [10] order_book_projection (read model)
CREATE TABLE `order_book_projection` (
                                         `market_id` BIGINT NOT NULL,
                                         `side` ENUM('BID','ASK') NOT NULL,
                                         `price` DECIMAL(30,8) NOT NULL,
                                         `total_qty` DECIMAL(30,8) NOT NULL,
                                         `order_count` INT NOT NULL,
                                         `updated_at` TIMESTAMP(3) NOT NULL,
                                         PRIMARY KEY (`market_id`, `side`, `price`),
                                         CONSTRAINT `fk_order_book_projection_market` FOREIGN KEY (`market_id`) REFERENCES `market`(`market_id`),
                                         CONSTRAINT `chk_order_book_projection_nonneg` CHECK (`total_qty` >= 0 AND `order_count` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [11] consumer_offset (checkpoint)
CREATE TABLE `consumer_offset` (
                                   `log_type` ENUM('COMMAND','EVENT') NOT NULL,
                                   `consumer_name` VARCHAR(100) NOT NULL,
                                   `partition_key` VARCHAR(64) NOT NULL,
                                   `last_offset` BIGINT NOT NULL DEFAULT 0,
                                   `updated_at` TIMESTAMP(3) NOT NULL,
                                   PRIMARY KEY (`log_type`, `consumer_name`, `partition_key`),
                                   CONSTRAINT `chk_consumer_offset_nonneg` CHECK (`last_offset` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
