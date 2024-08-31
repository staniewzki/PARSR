package mstan.service;

public class Stats {
    private int count = 0;
    private int sumPrice = 0;

    public int getCount() { return count; }
    public int getSumPrice() { return sumPrice; }

    public void updateWith(int price) {
        count += 1;
        sumPrice += price;
    }
}
