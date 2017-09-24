package eu.ha3.openapi.sparkling;

import eu.ha3.openapi.sparkling.routing.SparklingResponseContext;

/**
 * (Default template)
 * Created on 2017-09-24
 *
 * @author Ha3
 */
public class StoreController {
    public SparklingResponseContext getOrderById(long orderId) {
        return new SparklingResponseContext().status(200).entity("hello " + orderId);
    }
}
