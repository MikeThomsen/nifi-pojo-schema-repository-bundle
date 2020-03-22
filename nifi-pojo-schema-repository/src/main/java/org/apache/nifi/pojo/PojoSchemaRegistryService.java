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
    public static final PropertyDescriptor ENABLE_AVRO_POJOS = new PropertyDescriptor.Builder()
        .name("pojo-schema-reg-enable-avro")
        .displayName("Enable Avro-Generated POJOs")
        .description("If enabled, this will have the schema registry scan selected packages for POJOs that were generated " +
                "using the Avro Maven plugin.")
        .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
        .allowableValues("true", "false")
        .defaultValue("true")
        .required(true)
        .build();
    public static final PropertyDescriptor PACKAGES = new PropertyDescriptor.Builder()
        .name("pojo-schema-reg-packages")
        .displayName("Packages")
        .description("A comma-separated list of packages to scan and generate schemas from.")
        .addValidator(StandardValidators.NON_EMPTY_EL_VALIDATOR)
        .required(true)
        .build();

    public static final List<PropertyDescriptor> DESCRIPTOR_LIST = Collections.unmodifiableList(Arrays.asList(
        EXTENSION_JARS, ENABLE_AVRO_POJOS, PACKAGES
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
        return null;
    }
}
