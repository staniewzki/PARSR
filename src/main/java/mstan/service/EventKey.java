package mstan.service;

import mstan.domain.Action;

public class EventKey {
    private String cookie;
    private Action action;

    public EventKey(String cookie, Action action) {
        this.cookie = cookie;
        this.action = action;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EventKey that = (EventKey) obj;
        return this.cookie.equals(that.cookie) && this.action.equals(that.action);
    }

    @Override
    public int hashCode() {
        return cookie.hashCode() * 1024 + action.hashCode();
    }
};
