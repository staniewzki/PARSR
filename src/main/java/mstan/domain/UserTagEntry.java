package mstan.domain;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "UserTag")
public class UserTagEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Instant time;
    private String cookie;

    private String country;
    private Device device;
    private Action action;
    private String origin;

    private int productId;
    private String productBrandId;
    private String productCategoryId;
    private int productPrice;

    public void fromEvent(UserTagEvent event) {
        this.time = event.getTime();
        this.cookie = event.getCookie();
        this.country = event.getCountry();
        this.device = event.getDevice();
        this.action = event.getAction();
        this.origin = event.getOrigin();
        this.productId = event.getProductInfo().getProductId();
        this.productBrandId = event.getProductInfo().getBrandId();
        this.productCategoryId = event.getProductInfo().getCategoryId();
        this.productPrice = event.getProductInfo().getPrice();
    }

    public UserTagEvent intoEvent() {
        Product product = new Product();
        product.setProductId(productId);
        product.setBrandId(productBrandId);
        product.setCategoryId(productCategoryId);
        product.setPrice(productPrice);
        return new UserTagEvent(time, cookie, country, device, action, origin, product);
    }

    public int getId() {
        return id;
    }

    public String getCookie() {
        return cookie;
    }

    public Instant getTime() {
        return time;
    }

    public String getCountry() {
        return country;
    }

    public Device getDevice() {
        return device;
    }

    public Action getAction() {
        return action;
    }

    public String getOrigin() {
        return origin;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductBrandId() {
        return productBrandId;
    }

    public String productCategoryId() {
        return productCategoryId;
    }

    public int getProductPrice() {
        return productPrice;
    }
}
