package eu.ha3.openapi.sparkling.vo;

import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.ParameterLocation;

import java.util.Objects;

/**
 * (Default template)
 * Created on 2017-09-23
 *
 * @author Ha3
 */
public class SparklingParameter {
    private final String name;
    private final ParameterLocation location;
    private final ArrayType arrayType;
    private final DeserializeInto type;
    private final SparklingRequirement requirement;

    public SparklingParameter(String name, ParameterLocation location, ArrayType arrayType, DeserializeInto type, SparklingRequirement requirement) {
        this.name = name;
        this.location = location;
        this.arrayType = arrayType;
        this.type = type;
        this.requirement = requirement;
    }

    public String getName() {
        return name;
    }

    public ParameterLocation getLocation() {
        return location;
    }

    public ArrayType getArrayType() {
        return arrayType;
    }

    public DeserializeInto getType() {
        return type;
    }

    public SparklingRequirement getRequirement() {
        return requirement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SparklingParameter that = (SparklingParameter) o;
        return Objects.equals(name, that.name) &&
                location == that.location &&
                arrayType == that.arrayType &&
                type == that.type &&
                requirement == that.requirement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, location, arrayType, type, requirement);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SparklingParameter{");
        sb.append("name='").append(name).append('\'');
        sb.append(", location=").append(location);
        sb.append(", arrayType=").append(arrayType);
        sb.append(", type=").append(type);
        sb.append(", requirement=").append(requirement);
        sb.append('}');
        return sb.toString();
    }
}
