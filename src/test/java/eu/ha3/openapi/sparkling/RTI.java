package eu.ha3.openapi.sparkling;

import com.google.gson.Gson;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * (Default template)
 * Created on 2017-10-09
 *
 * @author Ha3
 */
public class RTI {
    public static void main(String[] args) {
        RTI rti = new RTI();
        rti.invoke("doCatStuff", "{\"someCatAttribute\":\"majestic\",\"soundName\":\"meow\",\"someCatAttribute\":\"cat\",\"someCowAttribute\":\"cow\",\"anotherCowAttribute\":50}");
        rti.invoke("doCowStuff", "{\"someCatAttribute\":\"majestic\",\"soundName\":\"moo\",\"someCatAttribute\":\"cat\",\"someCowAttribute\":\"cow\",\"anotherCowAttribute\":50}");

        rti.invoke("doMultiCatStuff", "[{\"someCatAttribute\":\"majestic\",\"soundName\":\"meow\",\"someCatAttribute\":\"cat\",\"someCowAttribute\":\"cow\",\"anotherCowAttribute\":50},{\"someCatAttribute\":\"fantastic\",\"soundName\":\"meowww\",\"someCatAttribute\":\"kitty\",\"someCowAttribute\":\"cow\",\"anotherCowAttribute\":50}]");
    }

    /**
     * Invoke the single-parameter method.
     * The json string will be deserialized using the available runtime type information.
     * If the method parameter is a subclass of Collection, it will try to deserialize into the expected generic parameter.
     *
     * @param methodName
     * @param json
     */
    private void invoke(String methodName, String json) {
        Arrays.stream(this.getClass().getMethods())
                .filter(method -> methodName.equals(method.getName()))
                .findFirst()
                .ifPresent(method -> {
                            try {
                                Class<?> parameterType = method.getParameterTypes()[0];
                                if (Collection.class.isAssignableFrom(parameterType)) {
                                    // We need information about the runtime generic parameter in order to deserialize it.

                                    ParameterizedType collectionType = (ParameterizedType) method.getGenericParameterTypes()[0];
                                    Type genericParameterType = collectionType.getActualTypeArguments()[0];
                                    System.out.println("...Expecting type Collection<" + genericParameterType + ">");
                                }

                                Object o = new Gson().fromJson(json, method.getGenericParameterTypes()[0]);
                                method.invoke(this, o);

                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                );
    }

    public void doCatStuff(Cat cat) {
        System.out.println(cat);
    }

    public void doCowStuff(Cow cow) {
        System.out.println(cow);
    }

    public void doMultiCatStuff(List<Cat> cat) {
        System.out.println(cat.getClass().getSimpleName() + " of " + cat.get(0).getClass().getSimpleName() + " -> " + cat);
    }

    private class Cat {
        private String soundName;
        private String someCatAttribute;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Cat{");
            sb.append("soundName='").append(soundName).append('\'');
            sb.append(", someCatAttribute='").append(someCatAttribute).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    private class Cow {
        private String soundName;
        private String someCowAttribute;
        private int anotherCowAttribute;

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Cow{");
            sb.append("soundName='").append(soundName).append('\'');
            sb.append(", someCowAttribute='").append(someCowAttribute).append('\'');
            sb.append(", anotherCowAttribute=").append(anotherCowAttribute);
            sb.append('}');
            return sb.toString();
        }
    }
}
