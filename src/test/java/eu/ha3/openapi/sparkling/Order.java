package eu.ha3.openapi.sparkling;

import java.time.OffsetDateTime;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
public class Order {
    public long id;
    public long petId;
    public int quantity;
    public OffsetDateTime shipDate;
    public String status;
    public boolean complete;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Order{");
        sb.append("id=").append(id);
        sb.append(", petId=").append(petId);
        sb.append(", quantity=").append(quantity);
        sb.append(", shipDate=").append(shipDate);
        sb.append(", status='").append(status).append('\'');
        sb.append(", complete=").append(complete);
        sb.append('}');
        return sb.toString();
    }
}
