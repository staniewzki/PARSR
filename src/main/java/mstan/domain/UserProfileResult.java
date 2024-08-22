package mstan.domain;

import java.util.ArrayList;
import java.util.List;

public class UserProfileResult {

    private String cookie;
    private List<UserTagEvent> views = new ArrayList<>();
    private List<UserTagEvent> buys = new ArrayList<>();

    public UserProfileResult() {}

    public UserProfileResult(String cookie, List<UserTagEvent> views, List<UserTagEvent> buys) {
        this.cookie = cookie;
        this.views = views;
        this.buys = buys;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserProfileResult that = (UserProfileResult) obj;
        return this.cookie.equals(that.cookie) &&
               this.views.equals(that.views) &&
               this.buys.equals(that.buys);
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public List<UserTagEvent> getViews() {
        return views;
    }

    public void setViews(List<UserTagEvent> views) {
        this.views = views;
    }

    public List<UserTagEvent> getBuys() {
        return buys;
    }

    public void setBuys(List<UserTagEvent> buys) {
        this.buys = buys;
    }
}
