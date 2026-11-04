package com.subby.subscription;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 결제일 계산 규칙.
 *
 * <ul>
 *   <li>n번째 결제일은 항상 첫 결제일에서 n주기를 더해 구한다. 직전 결제일에서 더하지 않는다.
 *       그래서 1월 31일 구독은 2월 28일 → 3월 31일로 돌아오고, 28일로 굳지 않는다.</li>
 *   <li>그 달에 같은 날짜가 없으면 말일이다 (LocalDate.plusMonths의 동작).</li>
 *   <li>2월 29일 연간 구독은 평년에 2월 28일이다 (LocalDate.plusYears의 동작).</li>
 *   <li>해지한 구독은 해지일 당일과 그 이후 결제를 계산하지 않는다.</li>
 * </ul>
 */
public final class BillingSchedule {

    private BillingSchedule() {
    }

    public static LocalDate occurrence(LocalDate startDate, BillingCycle cycle, long index) {
        return switch (cycle) {
            case WEEKLY -> startDate.plusWeeks(index);
            case MONTHLY -> startDate.plusMonths(index);
            case YEARLY -> startDate.plusYears(index);
        };
    }

    /**
     * from ~ to(양 끝 포함) 사이의 결제일 목록.
     *
     * @param canceledAt 해지일. null이면 해지하지 않은 구독
     */
    public static List<LocalDate> between(LocalDate startDate, BillingCycle cycle, LocalDate canceledAt,
                                          LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        if (to.isBefore(from) || to.isBefore(startDate)) {
            return dates;
        }
        long index = firstIndexOnOrAfter(startDate, cycle, from);
        while (true) {
            LocalDate date = occurrence(startDate, cycle, index);
            if (date.isAfter(to) || isCanceledBy(date, canceledAt)) {
                return dates;
            }
            dates.add(date);
            index++;
        }
    }

    /** date 이후(당일 포함) 가장 가까운 결제일. 해지돼서 더 이상 결제가 없으면 empty */
    public static Optional<LocalDate> nextOnOrAfter(LocalDate startDate, BillingCycle cycle, LocalDate canceledAt,
                                                    LocalDate date) {
        LocalDate next = occurrence(startDate, cycle, firstIndexOnOrAfter(startDate, cycle, date));
        if (isCanceledBy(next, canceledAt)) {
            return Optional.empty();
        }
        return Optional.of(next);
    }

    /** date 이후(당일 포함) 결제일을 count개 */
    public static List<LocalDate> upcoming(LocalDate startDate, BillingCycle cycle, LocalDate canceledAt,
                                           LocalDate date, int count) {
        List<LocalDate> dates = new ArrayList<>();
        long index = firstIndexOnOrAfter(startDate, cycle, date);
        while (dates.size() < count) {
            LocalDate next = occurrence(startDate, cycle, index++);
            if (isCanceledBy(next, canceledAt)) {
                break;
            }
            dates.add(next);
        }
        return dates;
    }

    private static long firstIndexOnOrAfter(LocalDate startDate, BillingCycle cycle, LocalDate date) {
        if (!date.isAfter(startDate)) {
            return 0;
        }
        ChronoUnit unit = switch (cycle) {
            case WEEKLY -> ChronoUnit.WEEKS;
            case MONTHLY -> ChronoUnit.MONTHS;
            case YEARLY -> ChronoUnit.YEARS;
        };
        // 대략적인 위치에서 한 칸 앞부터 확인해서, 말일 보정으로 생기는 오차를 흡수한다.
        long index = Math.max(0, unit.between(startDate, date) - 1);
        while (occurrence(startDate, cycle, index).isBefore(date)) {
            index++;
        }
        return index;
    }

    private static boolean isCanceledBy(LocalDate paymentDate, LocalDate canceledAt) {
        return canceledAt != null && !paymentDate.isBefore(canceledAt);
    }
}
