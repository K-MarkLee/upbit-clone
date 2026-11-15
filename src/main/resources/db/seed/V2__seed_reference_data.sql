-- V2__seed_reference_data.sql

INSERT INTO asset (symbol, name, decimals, status, created_at, updated_at)
VALUES
    ('KRW', 'Korean Won', 0, 'ACTIVE', NOW(3), NOW(3)),
    ('BTC', 'Bitcoin', 8, 'ACTIVE', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
                     name = VALUES(name),
                     decimals = VALUES(decimals),
                     status = VALUES(status),
                     updated_at = NOW(3);

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
    base.asset_id,
    quote.asset_id,
    'KRW-BTC',
    'ACTIVE',
    10.00000000,
    10.00000000,
    NOW(3),
    NOW(3)
FROM asset base
         JOIN asset quote
WHERE base.symbol = 'BTC'
  AND quote.symbol = 'KRW'
ON DUPLICATE KEY UPDATE
                     status = VALUES(status),
                     min_order_quote = VALUES(min_order_quote),
                     tick_size = VALUES(tick_size),
                     updated_at = NOW(3);
