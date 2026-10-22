package org.apache.nifi.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.avro.AvroFactory;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fasterxml.jackson.dataformat.avro.schema.AvroSchemaGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.avro.Schema;
import org.apache.nifi.annotation.behavior.Restricted;
import org.apache.nifi.annotation.behavior.Restriction;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.components.RequiredPermission;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.components.resource.ResourceCardinality;
import org.apache.nifi.components.resource.ResourceType;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.schema.access.SchemaField;
import org.apache.nifi.schema.access.SchemaNotFoundException;
import org.apache.nifi.schemaregistry.services.SchemaRegistry;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.SchemaIdentifier;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Restricted(restrictions = {
        @Restriction(
                requiredPermission = RequiredPermission.EXECUTE_CODE,
                explanation = "Loads and initializes classes from the configured JAR files, which can run arbitrary code."),
        @Restriction(
                requiredPermission = RequiredPermission.REFERENCE_REMOTE_RESOURCES,
                explanation = "Reads JAR files from any location on the NiFi host.")
})
public class PojoSchemaRegistryService extends AbstractControllerService implements SchemaRegistry {
    public static final PropertyDescriptor EXTENSION_JARS = new PropertyDescriptor.Builder()
        .name("pojo-schema-reg-extension-jars")
        .displayName("Extension JARs")
        .description("A comma-separated list of Java JAR files, or directories of JAR files, to be loaded for schema generation.")
        .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
        .dynamicallyModifiesClasspath(true)
        .identifiesExternalResource(ResourceCardinality.MULTIPLE, ResourceType.FILE, ResourceType.DIRECTORY)
        .required(true)
        .build();

    public static final List<PropertyDescriptor> DESCRIPTOR_LIST = List.of(EXTENSION_JARS);

    private static final Set<SchemaField> SUPPLIED_FIELDS = Set.of(SchemaField.SCHEMA_NAME);

    private volatile Map<String, RecordSchema> schemas = Map.of();

    @Override
    public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return DESCRIPTOR_LIST;
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(String name) {
        return new PropertyDescriptor.Builder()
                .name(name)
                .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
                .dynamic(true)
                .addValidator(Validator.VALID)
                .build();
    }

    @Override
    protected Collection<ValidationResult> customValidate(ValidationContext context) {
        List<ValidationResult> results = new ArrayList<>();
        List<PropertyDescriptor> dynamic = context.getProperties().keySet().stream()
                .filter(PropertyDescriptor::isDynamic)
                .toList();
        if (dynamic.isEmpty()) {
            return results;
        }

        try (URLClassLoader loader = createClassLoader(context.getProperty(EXTENSION_JARS))) {
            for (PropertyDescriptor prop : dynamic) {
                String className = context.getProperty(prop).evaluateAttributeExpressions().getValue();
                try {
                    Class.forName(className, false, loader);
                } catch (ClassNotFoundException | LinkageError ex) {
                    results.add(new ValidationResult.Builder()
                            .subject(prop.getName())
                            .input(className)
                            .valid(false)
                            .explanation(String.format("class %s could not be loaded from the configured Extension JARs: %s", className, ex))
                            .build());
                }
            }
        } catch (IOException ex) {
            results.add(new ValidationResult.Builder()
                    .subject(EXTENSION_JARS.getDisplayName())
                    .valid(false)
                    .explanation("could not read the configured Extension JARs: " + ex.getMessage())
                    .build());
        }

        return results;
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) {
        Map<String, RecordSchema> temp = new HashMap<>();
        try (URLClassLoader loader = createClassLoader(context.getProperty(EXTENSION_JARS))) {
            for (PropertyDescriptor prop : context.getProperties().keySet()) {
                if (!prop.isDynamic()) {
                    continue;
                }

                String className = context.getProperty(prop).evaluateAttributeExpressions().getValue();
                try {
                    Class<?> clz = Class.forName(className, false, loader);
                    temp.put(prop.getName(), AvroTypeUtil.createSchema(convertSchema(clz)));
                } catch (Exception | LinkageError ex) {
                    throw new ProcessException(String.format("Could not generate schema %s from class %s", prop.getName(), className), ex);
                }
            }
        } catch (IOException ex) {
            throw new ProcessException("Could not read the configured Extension JARs", ex);
        }
        schemas = Map.copyOf(temp);
    }

    @OnDisabled
    public void onDisabled() {
        schemas = Map.of();
    }

    private URLClassLoader createClassLoader(PropertyValue jars) {
        URL[] urls = jars.evaluateAttributeExpressions().asResources().flatten().asURLs().toArray(new URL[0]);
        return new URLClassLoader(urls, getClass().getClassLoader());
    }

    protected Schema convertSchema(Class<?> clz) throws Exception {
        Schema existing = getGeneratedSchema(clz);
        if (existing != null) {
            return existing;
        }

        ObjectMapper mapper = new ObjectMapper(new AvroFactory());
        mapper.registerModule(new JavaTimeModule());
        AvroSchemaGenerator gen = new AvroSchemaGenerator();
        mapper.acceptJsonFormatVisitor(clz, gen);
        AvroSchema schemaWrapper = gen.getGeneratedSchema();

        Schema retVal = schemaWrapper.getAvroSchema();

        getLogger().debug("Generated this for {}:\n\n{}\n\n", clz.getName(), retVal.toString(true));

        return retVal;
    }

    /*
     * Classes generated by the Avro compiler carry their schema in a public static SCHEMA$ field.
     */
    private Schema getGeneratedSchema(Class<?> clz) throws IllegalAccessException {
        Field field;
        try {
            field = clz.getField("SCHEMA$");
        } catch (NoSuchFieldException ex) {
            return null;
        }

        if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Schema.class) {
            return null;
        }

        return (Schema) field.get(null);
    }

    @Override
    public RecordSchema retrieveSchema(SchemaIdentifier schemaIdentifier) throws SchemaNotFoundException {
        if (!schemaIdentifier.getName().isPresent()) {
            throw new SchemaNotFoundException("Missing schema name");
        }

        String name = schemaIdentifier.getName().get();
        RecordSchema schema = schemas.get(name);
        if (schema == null) {
            throw new SchemaNotFoundException(String.format("No schema named %s is configured", name));
        }

        return schema;
    }

    @Override
    public Set<SchemaField> getSuppliedSchemaFields() {
        return SUPPLIED_FIELDS;
    }
}
