package com.subby.stats;

import com.subby.exchange.MoneyConverter;
import com.subby.exchange.RateTable;
import com.subby.subscription.Subscription;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 실제로 발생하는(또는 발생할) 결제 1건과 원화 환산액 */
public record Payment(Subscription subscription, LocalDate date, long amountKrw) {

    public static List<Payment> between(List<Subscription> subscriptions, LocalDate from, LocalDate to,
                                        LocalDate today, RateTable rates) {
        List<Payment> payments = new ArrayList<>();
        for (Subscription subscription : subscriptions) {
            for (LocalDate date : subscription.paymentDatesBetween(from, to)) {
                long krw = MoneyConverter.toKrw(subscription.getAmount(), subscription.getCurrency(),
                        rates.rateForPayment(date, today));
                payments.add(new Payment(subscription, date, krw));
            }
        }
        payments.sort(Comparator.comparing(Payment::date)
                .thenComparing(payment -> payment.subscription().getName()));
        return payments;
    }

    public static long sum(List<Payment> payments) {
        return payments.stream().mapToLong(Payment::amountKrw).sum();
    }
}
