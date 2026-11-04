package com.subby.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillingScheduleTest {

    @Test
    @DisplayName("31일 매월 결제는 말일로 보정되고, 다음 달에 다시 31일로 돌아온다")
    void monthlyEndOfMonth() {
        assertThat(BillingSchedule.between(LocalDate.of(2026, 1, 31), BillingCycle.MONTHLY, null,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 5, 31)))
                .containsExactly(
                        LocalDate.of(2026, 2, 28),
                        LocalDate.of(2026, 3, 31),
                        LocalDate.of(2026, 4, 30),
                        LocalDate.of(2026, 5, 31));
    }

    @Test
    @DisplayName("2월 29일 연간 결제는 평년에 2월 28일, 윤년에 2월 29일")
    void yearlyLeapDay() {
        assertThat(BillingSchedule.between(LocalDate.of(2024, 2, 29), BillingCycle.YEARLY, null,
                LocalDate.of(2025, 1, 1), LocalDate.of(2028, 12, 31)))
                .containsExactly(
                        LocalDate.of(2025, 2, 28),
                        LocalDate.of(2026, 2, 28),
                        LocalDate.of(2027, 2, 28),
                        LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("매주 결제는 첫 결제일부터 7일 간격")
    void weekly() {
        assertThat(BillingSchedule.between(LocalDate.of(2026, 11, 2), BillingCycle.WEEKLY, null,
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 23)))
                .containsExactly(
                        LocalDate.of(2026, 11, 9),
                        LocalDate.of(2026, 11, 16),
                        LocalDate.of(2026, 11, 23));
    }

    @Test
    @DisplayName("오늘이 결제일이면 오늘이 다음 결제일")
    void todayIsPaymentDay() {
        assertThat(BillingSchedule.nextOnOrAfter(LocalDate.of(2026, 3, 15), BillingCycle.MONTHLY, null,
                LocalDate.of(2026, 5, 15)))
                .contains(LocalDate.of(2026, 5, 15));
    }

    @Test
    @DisplayName("첫 결제일이 미래면 첫 결제일이 다음 결제일")
    void futureStart() {
        assertThat(BillingSchedule.nextOnOrAfter(LocalDate.of(2026, 12, 1), BillingCycle.MONTHLY, null,
                LocalDate.of(2026, 11, 3)))
                .contains(LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("해지일 당일과 그 이후 결제는 계산하지 않는다")
    void canceled() {
        LocalDate start = LocalDate.of(2026, 1, 10);
        LocalDate canceledAt = LocalDate.of(2026, 3, 10);

        assertThat(BillingSchedule.between(start, BillingCycle.MONTHLY, canceledAt,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .containsExactly(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10));
        assertThat(BillingSchedule.nextOnOrAfter(start, BillingCycle.MONTHLY, canceledAt, LocalDate.of(2026, 3, 1)))
                .isEmpty();
    }

    @Test
    @DisplayName("첫 결제일 이전 기간에는 결제가 없다")
    void beforeStart() {
        assertThat(BillingSchedule.between(LocalDate.of(2026, 6, 1), BillingCycle.MONTHLY, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31)))
                .isEmpty();
    }

    @Test
    @DisplayName("다가오는 결제일 3개")
    void upcoming() {
        assertThat(BillingSchedule.upcoming(LocalDate.of(2026, 1, 31), BillingCycle.MONTHLY, null,
                LocalDate.of(2026, 2, 1), 3))
                .containsExactly(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30));
    }
}
