package eu.ha3.openapi.sparkling.common;

import com.google.gson.Gson;

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
    private final Gson gson;

    public Modelizer(Gson gson) {
        this.gson = gson;
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
        if (item instanceof String && reflectedType != String.class) {
            // reflectedType can be either a model, or a list of models, it will work the same
            return gson.fromJson((String) item, reflectedType);

        } else {
            return item;
        }
    }

    private Object modelizeListSource(Object item, Type reflectedType, List list) {
        if (list.size() > 0
                && list.get(0) instanceof String
                && reflectedType instanceof ParameterizedType
                && ((ParameterizedType)reflectedType).getActualTypeArguments()[0] != String.class) {
            return ((List<String>) item).stream()
                            .map(o -> gson.fromJson((String) o, reflectedType))
                            .collect(Collectors.toList());

        } else {
            return item;
        }
    }
}
