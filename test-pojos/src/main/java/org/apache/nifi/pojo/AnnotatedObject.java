package org.apache.nifi.pojo;

import org.apache.avro.reflect.AvroAlias;
import org.apache.avro.reflect.AvroAliases;
import org.apache.avro.reflect.AvroName;
import org.apache.avro.reflect.Nullable;

public class AnnotatedObject {
    @AvroAliases({ @AvroAlias(alias = "aliasTest1"), @AvroAlias(alias = "aliasTest2")})
    private String multipleAliases;

    @Nullable
    private Long nullable;

    public AnnotatedObject(String multipleAliases, Long nullable) {
        this.multipleAliases = multipleAliases;
        this.nullable = nullable;
    }

    public String getMultipleAliases() {
        return multipleAliases;
    }

    public Long getNullable() {
        return nullable;
    }
}
