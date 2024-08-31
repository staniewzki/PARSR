package mstan.service;

import java.util.ArrayList;
import java.util.List;

import mstan.domain.Action;

public class Filters {
    private Action action;
    private String origin;
    private String brandId;
    private String categoryId;

    public Filters(Action action, String origin, String brandId, String categoryId) {
        this.action = action;
        this.origin = origin;
        this.brandId = brandId;
        this.categoryId = categoryId;
    }

    public Action getAction() {
        return action;
    }

    public String value() {
        String result = (origin == null ? "<null>" : origin) + "|" +
                        (brandId == null ? "<null>" : brandId) + "|" +
                        (categoryId == null ? "<null>" : categoryId);
        return result;
    }

    public boolean isFull() {
        return origin != null && brandId != null && categoryId != null;
    }

    public static List<Filters> allFilters(Action action, String origin, String brandId, String categoryId) {
        List<Filters> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    result.add(new Filters(
                        action,
                        i == 0 ? origin : null,
                        j == 0 ? brandId : null,
                        k == 0 ? categoryId : null));
                }
            }
        }
        return result;
    }

    private static boolean cmp(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static int hsh(String s) {
        if (s == null) return 0;
        return s.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Filters that = (Filters) obj;
        return this.action == that.action &&
               cmp(this.origin, that.origin) &&
               cmp(this.brandId, that.brandId) &&
               cmp(this.categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return action.hashCode() + 2 * hsh(origin) + 64 * hsh(brandId) + 2048 * hsh(categoryId);
    }
};
