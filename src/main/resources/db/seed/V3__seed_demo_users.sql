-- V3__seed_demo_users.sql

INSERT INTO users (email, user_name, status, password_hash, created_at, updated_at)
VALUES
    ('buyer@demo.local', '돈 많은 테스트 유저', 'ACTIVE', 'demo-password-hash', NOW(3), NOW(3)),
    ('seller@demo.local', '코인 많은 테스트 유저', 'ACTIVE', 'demo-password-hash', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
                     user_name = VALUES(user_name),
                     status = VALUES(status),
                     updated_at = NOW(3);

SET @buyer_id = (SELECT user_id FROM users WHERE email = 'buyer@demo.local');
SET @seller_id = (SELECT user_id FROM users WHERE email = 'seller@demo.local');
SET @krw_id = (SELECT asset_id FROM asset WHERE symbol = 'KRW');
SET @btc_id = (SELECT asset_id FROM asset WHERE symbol = 'BTC');

INSERT INTO wallet (
    user_id,
    asset_id,
    available_balance,
    locked_balance,
    version,
    created_at,
    updated_at
)
VALUES
    (@buyer_id, @krw_id, 1000000000.00000000, 0.00000000, 0, NOW(3), NOW(3)),
    (@buyer_id, @btc_id, 0.00000000, 0.00000000, 0, NOW(3), NOW(3)),
    (@seller_id, @krw_id, 0.00000000, 0.00000000, 0, NOW(3), NOW(3)),
    (@seller_id, @btc_id, 10.00000000, 0.00000000, 0, NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
                     available_balance = VALUES(available_balance),
                     locked_balance = VALUES(locked_balance),
                     updated_at = NOW(3);
