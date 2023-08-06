package org.apache.nifi.pojo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.avro.AvroFactory;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.fasterxml.jackson.dataformat.avro.schema.AvroSchemaGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.avro.Schema;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.avro.AvroTypeUtil;
import org.apache.nifi.components.PropertyDescriptor;
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
import org.apache.nifi.util.file.classloader.ClassLoaderUtils;

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
        .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
        .dynamicallyModifiesClasspath(true)
        .defaultValue("")
        .addValidator(StandardValidators.FILE_EXISTS_VALIDATOR)
        .required(true)
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
        String modulePath = context.getProperty(EXTENSION_JARS).evaluateAttributeExpressions().getValue();
        Map<String, RecordSchema> temp = new ConcurrentHashMap<>();
        context.getProperties().keySet().stream().filter(prop -> prop.isDynamic())
            .forEach(prop -> {
                try {
                    ClassLoader loader = ClassLoaderUtils.getCustomClassLoader(modulePath,
                            this.getClass().getClassLoader(), null);

                            //Thread.currentThread().getContextClassLoader();
                    String name = context.getProperty(prop).evaluateAttributeExpressions().getValue();
                    Class clz = Class.forName(name, true, loader);
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
        mapper.registerModule(new JavaTimeModule());
        AvroSchemaGenerator gen = new AvroSchemaGenerator();
        mapper.acceptJsonFormatVisitor(clz, gen);
        AvroSchema schemaWrapper = gen.getGeneratedSchema();

        Schema retVal = schemaWrapper.getAvroSchema();

        getLogger().debug("Generated this for {}:\n\n{}\n\n", clz.getName(), retVal.toString(true));

        return retVal;
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(String name) {
        return new PropertyDescriptor.Builder()
                .name(name)
                .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
                .dynamic(true)
                .addValidator(Validator.VALID)
//                .addValidator((subject, input, context) -> {
//                    ValidationResult result;
//                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
//
//                    try {
//                        Class.forName(input, true, loader);
//                        result = new ValidationResult.Builder()
//                                .valid(true)
//                                .build();
//                    } catch (Exception ex) {
//                        result = new ValidationResult.Builder()
//                                .input(input)
//                                .subject(subject)
//                                .explanation(ex.getMessage())
//                                .valid(false)
//                                .build();
//                    }
//                    return result;
//                })
                .build();
    }

    @Override
    public RecordSchema retrieveSchema(SchemaIdentifier schemaIdentifier) throws SchemaNotFoundException {
        if (!schemaIdentifier.getName().isPresent()) {
            throw new SchemaNotFoundException("Missing schema name");
        }

        String name = schemaIdentifier.getName().get();

        return SCHEMA_CACHE.get(name);
    }

    @Override
    public Set<SchemaField> getSuppliedSchemaFields() {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            SchemaField.SCHEMA_NAME
        )));
    }
}
