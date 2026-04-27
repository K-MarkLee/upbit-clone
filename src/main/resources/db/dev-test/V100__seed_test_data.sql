INSERT INTO users (email, user_name, status, password_hash, created_at, updated_at)
VALUES
    ('seller@test.com', 'seller', 'ACTIVE', 'dev-password', NOW(3), NOW(3)),
    ('buyer@test.com', 'buyer', 'ACTIVE', 'dev-password', NOW(3), NOW(3));

INSERT INTO asset (symbol, name, decimals, status, created_at, updated_at)
VALUES
    ('KRW', 'Korean Won', 0, 'ACTIVE', NOW(3), NOW(3)),
    ('BTC', 'Bitcoin', 8, 'ACTIVE', NOW(3), NOW(3));

INSERT INTO market (
    base_asset_id,
    quote_asset_id,
    market_code,
    status,
    min_order_quote,
    tick_size,
    created_at,
    updated_at
)
SELECT
    btc.asset_id,
    krw.asset_id,
    'KRW-BTC',
    'ACTIVE',
    5000.00000000,
    1000.00000000,
    NOW(3),
    NOW(3)
FROM asset btc
         JOIN asset krw
WHERE btc.symbol = 'BTC'
  AND krw.symbol = 'KRW';


INSERT INTO wallet (user_id, asset_id, available_balance, locked_balance, version, created_at, updated_at)
SELECT u.user_id, a.asset_id,
       CASE
           WHEN u.email = 'seller@test.com' AND a.symbol = 'BTC' THEN 1.00000000
           WHEN u.email = 'buyer@test.com' AND a.symbol = 'KRW' THEN 10000000.00000000
           ELSE 0.00000000
           END,
       0.00000000,
       0,
       NOW(3),
       NOW(3)
FROM users u
         JOIN asset a
WHERE u.email IN ('seller@test.com', 'buyer@test.com')
  AND a.symbol IN ('KRW', 'BTC');
