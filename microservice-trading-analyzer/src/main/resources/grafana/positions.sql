SELECT
    ts AS time,
    open,
    high,
    low,
    close,
    feature_adx_14_4h,
    feature_rsi_14_4h
FROM
    features
WHERE
    tf = '4h'
  AND contract_hash = '29d46bbc0cf396221255dde2ce1304efdc0e34022ffcd12d696a0675c3b49c3b'
ORDER BY
    ts