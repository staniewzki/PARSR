package mstan.service;

public class Stats {
    private long count = 0;
    private long sumPrice = 0;

    public long getCount() { return count; }
    public long getSumPrice() { return sumPrice; }

    public void updateWith(long price) {
        count += 1;
        sumPrice += price;
    }
}
