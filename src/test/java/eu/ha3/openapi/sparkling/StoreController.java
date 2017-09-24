package eu.ha3.openapi.sparkling;

import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;
import spark.Request;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
public class StoreController {
    public SparklingResponseContext placeOrder(Request request, Order order) {
        return new SparklingResponseContext().status(200).entity("order " + order.toString());
    }

    public SparklingResponseContext getOrderById(Request request, long orderId) {
        return new SparklingResponseContext().status(200).entity("hello " + orderId);
    }
}
