package mstan.service;

import java.util.concurrent.atomic.AtomicInteger;

import mstan.domain.UserTagEvent;

public class Stats {
    private AtomicInteger count = new AtomicInteger(0);
    private AtomicInteger sumPrice = new AtomicInteger(0);

    public AtomicInteger getCount() { return count; }

    public AtomicInteger getSumPrice() { return sumPrice; }

    public void updateWith(int price) {
        count.incrementAndGet();
        sumPrice.addAndGet(price);
    }
}
