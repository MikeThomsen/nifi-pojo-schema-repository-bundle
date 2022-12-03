package org.apache.nifi.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.avro.AvroFactory;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fasterxml.jackson.dataformat.avro.schema.AvroSchemaGenerator;
import org.apache.avro.Schema;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.schema.access.SchemaField;
import org.apache.nifi.schema.access.SchemaNotFoundException;
import org.apache.nifi.schemaregistry.services.SchemaRegistry;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.SchemaIdentifier;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

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

    private Map<String, RecordSchema> SCHEMA_CACHE;

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) {
        Map<String, RecordSchema> temp = new ConcurrentHashMap<>();
        context.getProperties().keySet().stream().filter(prop -> prop.isDynamic())
            .forEach(prop -> {
                try {
                    Class clz = Class.forName(context.getProperty(prop).evaluateAttributeExpressions().getValue());
                    Schema schema = convertSchema(clz);
                    temp.put(prop.getName(), AvroTypeUtil.createSchema(schema));
                } catch (Exception ex) {
                    throw new ProcessException(ex);
                }
            });
        SCHEMA_CACHE = temp;
    }

    protected Schema convertSchema(Class clz) throws Exception {
        Optional<Field> existing = Stream.of(clz.getDeclaredFields())
                .filter(field -> field.getType() == Schema.class).findFirst();
        if (existing.isPresent()) {
            Field field = existing.get();
            Object obj  = field.get(null);

            return (Schema)obj;
        }

        ObjectMapper mapper = new ObjectMapper(new AvroFactory());
        AvroSchemaGenerator gen = new AvroSchemaGenerator();
        mapper.acceptJsonFormatVisitor(clz, gen);
        AvroSchema schemaWrapper = gen.getGeneratedSchema();

        return schemaWrapper.getAvroSchema();
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(String name) {
        return new PropertyDescriptor.Builder()
                .name(name)
                .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
                .addValidator((subject, input, context) -> {
                    ValidationResult result;
                    try {
                        Class.forName(input);
                        result = new ValidationResult.Builder()
                                .valid(true)
                                .build();
                    } catch (Exception ex) {
                        result = new ValidationResult.Builder()
                                .input(input)
                                .subject(subject)
                                .explanation(ex.getMessage())
                                .valid(false)
                                .build();
                    }
                    return result;
                })
                .build();
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
