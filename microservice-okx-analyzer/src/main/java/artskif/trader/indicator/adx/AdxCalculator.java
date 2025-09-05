package artskif.trader.indicator.adx;

import artskif.trader.dto.CandlestickDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdxCalculator {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int SCALE = 10;
    private static final int OUT_SCALE = 2;

    public static Optional<AdxPoint> computeLastAdx(int period, Map<Instant, CandlestickDto> history, boolean onlyConfirmed) {
        int n = period;
        if (history == null || history.isEmpty()) return Optional.empty();

        // 1) Создаем список
        List<Map.Entry<Instant, CandlestickDto>> rows = new ArrayList<>(history.entrySet());

        // 2) Готовим TR и сырые +DM/-DM
        List<BigDecimal> trList = new ArrayList<>();
        List<BigDecimal> plusDmRawList = new ArrayList<>();
        List<BigDecimal> minusDmRawList = new ArrayList<>();
        List<Instant> tsList = new ArrayList<>();

        BigDecimal prevClose = null, prevHigh = null, prevLow = null;

        for (Map.Entry<Instant, CandlestickDto> e : rows) {
            CandlestickDto c = e.getValue();
            if (onlyConfirmed && (c.getConfirmed() == null || !c.getConfirmed())) continue;

            BigDecimal h = nz(c.getHigh());
            BigDecimal l = nz(c.getLow());
            BigDecimal cl = nz(c.getClose());

            if (prevClose == null) {
                trList.add(BigDecimal.ZERO);
                plusDmRawList.add(BigDecimal.ZERO);
                minusDmRawList.add(BigDecimal.ZERO);
            } else {
                BigDecimal tr = max3(
                        h.subtract(l, MC).abs(MC),
                        h.subtract(prevClose, MC).abs(MC),
                        l.subtract(prevClose, MC).abs(MC)
                );

                BigDecimal upMove = h.subtract(prevHigh, MC);
                BigDecimal downMove = prevLow.subtract(l, MC);

                BigDecimal plusDmRaw  = (gt(upMove, downMove) && gt(upMove,  BigDecimal.ZERO)) ? upMove  : BigDecimal.ZERO;
                BigDecimal minusDmRaw = (gt(downMove, upMove) && gt(downMove, BigDecimal.ZERO)) ? downMove : BigDecimal.ZERO;

                trList.add(scale(tr));
                plusDmRawList.add(scale(plusDmRaw));
                minusDmRawList.add(scale(minusDmRaw));
            }
            tsList.add(e.getKey());
            prevClose = cl; prevHigh = h; prevLow = l;
        }

        int m = tsList.size();
        // Для ADX нужен минимум 2*n баров (n на первичную RMA + n DX для старта ADX)
        if (m < 2 * n) return Optional.empty();

        BigDecimal nBD = new BigDecimal(n, MC);

        // 3) Инициализация RMA за первые n баров (используем i=1..n)
        BigDecimal trR = BigDecimal.ZERO, pR = BigDecimal.ZERO, mR = BigDecimal.ZERO;
        for (int i = 1; i <= n; i++) {
            trR = trR.add(trList.get(i), MC);
            pR  = pR.add(plusDmRawList.get(i), MC);
            mR  = mR.add(minusDmRawList.get(i), MC);
        }

        // 4) Идём от i=n до конца, на КАЖДОМ шаге обновляем RMA и считаем DX
        List<BigDecimal> dxList = new ArrayList<>(m - n);
        BigDecimal lastPlusDI = BigDecimal.ZERO;
        BigDecimal lastMinusDI = BigDecimal.ZERO;
        BigDecimal lastDX = BigDecimal.ZERO;

        for (int i = n; i < m; i++) {
            if (i > n) {
                trR = trR.subtract(trR.divide(nBD, MC), MC).add(trList.get(i), MC);
                pR  = pR.subtract(pR.divide(nBD, MC), MC).add(plusDmRawList.get(i), MC);
                mR  = mR.subtract(mR.divide(nBD, MC), MC).add(minusDmRawList.get(i), MC);
            }
            // +DI/-DI для текущего бара i
            BigDecimal pdi = isZero(trR) ? BigDecimal.ZERO : pR.divide(trR, MC).multiply(hundred(), MC);
            BigDecimal mdi = isZero(trR) ? BigDecimal.ZERO : mR.divide(trR, MC).multiply(hundred(), MC);

            BigDecimal denom = pdi.add(mdi, MC);
            BigDecimal dxi = isZero(denom)
                    ? BigDecimal.ZERO
                    : pdi.subtract(mdi, MC).abs(MC).divide(denom, MC).multiply(hundred(), MC);

            dxList.add(dxi);

            if (i == m - 1) {
                lastPlusDI = pdi;
                lastMinusDI = mdi;
                lastDX = dxi;
            }
        }

        // 5) ADX = RMA(DX, n): сначала среднее первых n DX, затем рекурсия
        if (dxList.size() < n) return Optional.empty();
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) sum = sum.add(dxList.get(i), MC);
        BigDecimal adx = sum.divide(nBD, MC); // стартовое значение ADX на баре i = n+(n-1)

        for (int i = n; i < dxList.size(); i++) {
            adx = adx.add( dxList.get(i).subtract(adx, MC).divide(nBD, MC), MC );
        }

        // 6) Возвращаем последнюю точку
        Instant ts = tsList.get(m - 1);
        return Optional.of(new AdxPoint(
                ts,
                out2(lastPlusDI),
                out2(lastMinusDI),
                out2(lastDX),
                out2(adx)
        ));
    }

    // helpers
    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal hundred() {
        return new BigDecimal("100");
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static boolean isZero(BigDecimal v) {
        return v == null || v.compareTo(BigDecimal.ZERO) == 0;
    }

    private static boolean gt(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0;
    }

    private static BigDecimal max3(BigDecimal a, BigDecimal b, BigDecimal c) {
        return a.max(b).max(c);
    }

    private static BigDecimal out2(BigDecimal v) { return v.setScale(OUT_SCALE, RoundingMode.HALF_UP); }


}
