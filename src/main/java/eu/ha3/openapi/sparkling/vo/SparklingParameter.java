package eu.ha3.openapi.sparkling.vo;

import eu.ha3.openapi.sparkling.enums.DeserializeInto;
import eu.ha3.openapi.sparkling.enums.ArrayType;
import eu.ha3.openapi.sparkling.enums.ParameterLocation;

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
}
