package io.github.mikethomsen.nifi.schemaregistry.pojo;

import org.apache.avro.Schema;

/*
 * A plain POJO with an instance field of type Schema; it must not be mistaken for an Avro-generated class.
 */
public class CachedSchemaHolder {
    private String name;
    private transient Schema cached;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
