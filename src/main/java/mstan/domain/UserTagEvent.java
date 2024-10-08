package mstan.domain;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;


// @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
// We have to use @JsonProperty annotations instead the above due to this bug:
// https://github.com/FasterXML/jackson-databind/issues/3102
public class UserTagEvent {

    private Instant time;
    private String cookie;

    private String country;
    private Device device;
    private Action action;
    private String origin;
    @JsonProperty("product_info")
    private Product productInfo;

    public UserTagEvent() {}

    public UserTagEvent(Instant time, String cookie, String country, Device device, Action action, String origin, Product productInfo) {
        this.time = time;
        this.cookie = cookie;
        this.country = country;
        this.device = device;
        this.action = action;
        this.origin = origin;
        this.productInfo = productInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserTagEvent that = (UserTagEvent) obj;
        return this.time.equals(that.time) &&
               this.cookie.equals(that.cookie) &&
               this.country.equals(that.country) &&
               this.device == that.device &&
               this.action == that.action &&
               this.origin.equals(that.origin) &&
               this.productInfo.equals(that.productInfo);
    }

    @Override
    public String toString() {
        return "UserTagEvent{" +
                "time=" + time +
                ", cookie='" + cookie + '\'' +
                ", country='" + country + '\'' +
                ", device=" + device.name() +
                ", action=" + action.name() +
                ", origin='" + origin + '\'' +
                ", productInfo=" + productInfo +
                '}';
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Product getProductInfo() {
        return productInfo;
    }

    public void setProductInfo(Product productInfo) {
        this.productInfo = productInfo;
    }
}
