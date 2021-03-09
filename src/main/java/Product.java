public class Product {
    private String title;
    private String url;
    private String rating;
    private String sold;
    private String minPrice;
    private String maxPrice;

    public String getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(String minPrice) {
        this.minPrice = minPrice;
    }

    public String getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(String maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getSold() {
        return sold;
    }

    public void setSold(String sold) {
        this.sold = sold;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Product{" +
                "title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", rating='" + rating + '\'' +
                ", sold='" + sold + '\'' +
                ", minPrice='" + minPrice + '\'' +
                ", maxPrice='" + maxPrice + '\'' +
                '}';
    }
}
