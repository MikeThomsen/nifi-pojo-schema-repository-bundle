package org.apache.nifi.pojo;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.Validator;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.schema.access.SchemaField;
import org.apache.nifi.schema.access.SchemaNotFoundException;
import org.apache.nifi.schemaregistry.services.SchemaRegistry;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.SchemaIdentifier;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PojoSchemaRegistryService extends AbstractControllerService implements SchemaRegistry {
    public static final PropertyDescriptor EXTENSION_JARS = new PropertyDescriptor.Builder()
        .name("pojo-schema-reg-extension-jars")
        .displayName("Extension JARs")
        .description("A comma-separated list of Java JAR files to be loaded for schema generation.")
        .dynamicallyModifiesClasspath(true)
        .defaultValue("")
        .addValidator(Validator.VALID)
        .required(false)
        .build();

    public static final List<PropertyDescriptor> DESCRIPTOR_LIST = Collections.unmodifiableList(Arrays.asList(
        EXTENSION_JARS
    ));

    @Override
    public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return DESCRIPTOR_LIST;
    }

    @Override
    public RecordSchema retrieveSchema(SchemaIdentifier schemaIdentifier) throws IOException, SchemaNotFoundException {
        return null;
    }

    @Override
    public Set<SchemaField> getSuppliedSchemaFields() {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            SchemaField.SCHEMA_NAME
        )));
    }
}
