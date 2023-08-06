package org.apache.nifi.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.avro.reflect.AvroAlias;
import org.apache.avro.reflect.AvroAliases;
import org.apache.avro.reflect.AvroName;
import org.apache.avro.reflect.Nullable;

public class AnnotatedObject {
    @AvroAliases({ @AvroAlias(alias = "aliasTest1"), @AvroAlias(alias = "aliasTest2")})
    private String multipleAliases;

    @Nullable
    private Long nullable;

    /*
     * This doesn't appear to be honored by the generator
     */
    @AvroName("renamed")
    private String testField;

    public AnnotatedObject(String multipleAliases, Long nullable, String testField) {
        this.multipleAliases = multipleAliases;
        this.nullable = nullable;
        this.testField = testField;
    }

    public String getMultipleAliases() {
        return multipleAliases;
    }

    public Long getNullable() {
        return nullable;
    }

    @JsonProperty("renamed")
    public String getTestField() {
        return testField;
    }
}
