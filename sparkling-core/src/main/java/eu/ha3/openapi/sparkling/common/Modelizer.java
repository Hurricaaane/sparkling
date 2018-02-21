package eu.ha3.openapi.sparkling.common;

import eu.ha3.openapi.sparkling.routing.RequestConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * (Default template)
 * Created on 2017-10-11
 *
 * @author Ha3
 */
public class Modelizer {
    private final RequestConverter requestConverter;

    public Modelizer(RequestConverter requestConverter) {
        this.requestConverter = requestConverter;
    }

    Object modelize(Object item, Type reflectedType) {
        if (item instanceof List) {
            return modelizeListSource(item, reflectedType, (List) item);

        } else if (item instanceof String) {
            return modelizeStringSource(item, reflectedType);

        } else {
            return item;
        }
    }

    private Object modelizeStringSource(Object item, Type reflectedType) {
        try {
            if (item instanceof String && reflectedType != String.class) {
                // reflectedType can be either a model, or a list of models, it will work the same
                return requestConverter.convertRequest((String) item, reflectedType);

            } else {
                return item;
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Object modelizeListSource(Object item, Type reflectedType, List list) {
        if (list.size() > 0
                && list.get(0) instanceof String
                && reflectedType instanceof ParameterizedType
                && ((ParameterizedType)reflectedType).getActualTypeArguments()[0] != String.class) {
            return ((List<String>) item).stream()
                            .map(o -> {
                                try {
                                    return requestConverter.convertRequest((String) o, reflectedType);
                                } catch (Exception e) {
                                    throw new IllegalStateException(e);
                                }
                            })
                            .collect(Collectors.toList());

        } else {
            return item;
        }
    }
}
