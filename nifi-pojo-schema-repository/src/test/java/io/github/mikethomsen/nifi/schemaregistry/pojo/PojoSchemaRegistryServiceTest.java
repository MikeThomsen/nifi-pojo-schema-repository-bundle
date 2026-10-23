package io.github.mikethomsen.nifi.schemaregistry.pojo;

import org.apache.nifi.annotation.behavior.Restricted;
import org.apache.nifi.annotation.behavior.Restriction;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.components.RequiredPermission;
import org.apache.nifi.schema.access.SchemaNotFoundException;
import org.apache.nifi.serialization.record.DataType;
import org.apache.nifi.serialization.record.RecordField;
import org.apache.nifi.serialization.record.RecordFieldType;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.SchemaIdentifier;
import org.apache.nifi.serialization.record.StandardSchemaIdentifier;
import org.apache.nifi.serialization.record.type.RecordDataType;
import org.apache.nifi.util.NoOpProcessor;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PojoSchemaRegistryServiceTest {
    // Set by Surefire; see nifi-pojo-schema-repository/pom.xml
    private static final String TEST_POJOS_JAR = System.getProperty("test.pojos.jar");
    private static final String AVRO_JAR = System.getProperty("avro.jar");

    private TestRunner testRunner;
    private PojoSchemaRegistryService service;

    @BeforeEach
    public void setup() throws Exception {
        testRunner = TestRunners.newTestRunner(NoOpProcessor.class);
        service = new PojoSchemaRegistryService();
        testRunner.addControllerService("service", service);
        testRunner.setProperty(service, PojoSchemaRegistryService.EXTENSION_JARS, TEST_POJOS_JAR);
    }

    private RecordSchema retrieve(String name) {
        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name(name)
                .build();
        AtomicReference<RecordSchema> schema = new AtomicReference<>();
        assertDoesNotThrow(() -> schema.set(service.retrieveSchema(identifier)));
        return schema.get();
    }

    private static RecordFieldType typeOf(RecordSchema schema, String field) {
        return schema.getField(field).orElseThrow().getDataType().getFieldType();
    }

    private static RecordSchema childSchema(RecordSchema schema, String field) {
        DataType type = schema.getField(field).orElseThrow().getDataType();
        return ((RecordDataType) type).getChildSchema();
    }

    @Test
    public void testAvroGeneratedObject() {
        testRunner.setProperty(service, "message", "io.github.mikethomsen.nifi.schemaregistry.pojo.MessageRecord");
        testRunner.enableControllerService(service);

        RecordSchema result = retrieve("message");
        assertEquals(2, result.getFieldNames().size());
        assertNotNull(result.getField("from"));
        assertNotNull(result.getField("message"));
    }

    @Test
    public void testJacksonGeneratedSchema() {
        testRunner.setProperty(service, "user", "io.github.mikethomsen.nifi.schemaregistry.pojo.User");
        testRunner.enableControllerService(service);

        RecordSchema result = retrieve("user");
        assertEquals(3, result.getFieldNames().size());
        assertEquals(RecordFieldType.STRING, typeOf(result, "username"));
        assertEquals(RecordFieldType.STRING, typeOf(result, "password"));
        assertEquals(RecordFieldType.INT, typeOf(result, "failedLoginCount"));
    }

    @Test
    public void testAnnotatedPojoSchema() {
        testRunner.setProperty(service, "object", "io.github.mikethomsen.nifi.schemaregistry.pojo.AnnotatedObject");
        testRunner.enableControllerService(service);

        RecordSchema result = retrieve("object");
        assertEquals(3, result.getFieldNames().size());
        Optional<RecordField> aliasedField = result.getField("multipleAliases");
        Optional<RecordField> nullable = result.getField("nullable");
        Optional<RecordField> renamed = result.getField("renamed");

        assertTrue(aliasedField.isPresent());
        assertTrue(nullable.isPresent());
        assertTrue(renamed.isPresent());
    }

    @Test
    public void testComplexModelWithJackson() {
        testRunner.setProperty(service, "transaction", "io.github.mikethomsen.nifi.schemaregistry.pojo.complex.FinancialTransaction");
        testRunner.enableControllerService(service);

        RecordSchema result = retrieve("transaction");
        assertEquals(Set.of("sender", "receiver", "amount", "sendDate", "receiveDate", "paymentService"),
                Set.copyOf(result.getFieldNames()));
        assertEquals(RecordFieldType.DOUBLE, typeOf(result, "amount"));
        assertEquals(RecordFieldType.TIMESTAMP, typeOf(result, "sendDate"));
        assertEquals(RecordFieldType.TIMESTAMP, typeOf(result, "receiveDate"));

        RecordSchema sender = childSchema(result, "sender");
        assertEquals(RecordFieldType.TIMESTAMP, typeOf(sender, "dateOfBirth"));
        assertEquals(RecordFieldType.STRING, typeOf(childSchema(sender, "mailingAddress"), "zipCode"));
        assertEquals(RecordFieldType.RECORD, typeOf(result, "receiver"));
        assertEquals(RecordFieldType.STRING,
                typeOf(childSchema(childSchema(result, "paymentService"), "serviceLocation"), "city"));
    }

    @Test
    public void testComplexModelWithAvroReflection() {
        testRunner.setProperty(service, PojoSchemaRegistryService.SCHEMA_STRATEGY, PojoSchemaRegistryService.STRATEGY_AVRO_REFLECTION);
        testRunner.setProperty(service, "transaction", "io.github.mikethomsen.nifi.schemaregistry.pojo.complex.FinancialTransaction");
        testRunner.enableControllerService(service);

        RecordSchema result = retrieve("transaction");
        assertEquals(RecordFieldType.TIMESTAMP, typeOf(result, "sendDate"));
        assertEquals(RecordFieldType.TIMESTAMP, typeOf(childSchema(result, "sender"), "dateOfBirth"));
        assertEquals(RecordFieldType.RECORD, typeOf(result, "paymentService"));
    }

    @Test
    public void testAvroReflectionHonorsAvroAnnotations() {
        testRunner.setProperty(service, PojoSchemaRegistryService.SCHEMA_STRATEGY, PojoSchemaRegistryService.STRATEGY_AVRO_REFLECTION);
        testRunner.setProperty(service, "object", "io.github.mikethomsen.nifi.schemaregistry.pojo.AnnotatedObject");
        testRunner.enableControllerService(service);

        RecordSchema result = retrieve("object");
        assertEquals(Set.of("multipleAliases", "nullable", "renamed"), Set.copyOf(result.getFieldNames()));
        assertEquals(Set.of("aliasTest1", "aliasTest2"), Set.copyOf(result.getField("multipleAliases").orElseThrow().getAliases()));
        assertTrue(result.getField("nullable").orElseThrow().isNullable());
        assertFalse(result.getField("renamed").orElseThrow().isNullable());
    }

    @Test
    public void testUnknownSchemaNameThrows() {
        testRunner.setProperty(service, "user", "io.github.mikethomsen.nifi.schemaregistry.pojo.User");
        testRunner.enableControllerService(service);

        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name("nope")
                .build();
        assertThrows(SchemaNotFoundException.class, () -> service.retrieveSchema(identifier));
    }

    @Test
    public void testMultipleExtensionJars() {
        testRunner.setProperty(service, PojoSchemaRegistryService.EXTENSION_JARS, TEST_POJOS_JAR + ", " + AVRO_JAR);
        testRunner.setProperty(service, "user", "io.github.mikethomsen.nifi.schemaregistry.pojo.User");
        testRunner.assertValid(service);
        testRunner.enableControllerService(service);

        assertNotNull(retrieve("user"));
    }

    @Test
    public void testUnknownClassIsInvalid() {
        testRunner.setProperty(service, "missing", "com.example.DoesNotExist");
        testRunner.assertNotValid(service);
    }

    @Test
    public void testInstanceSchemaFieldUsesGeneratedSchema() {
        testRunner.setProperty(service, "holder", "io.github.mikethomsen.nifi.schemaregistry.pojo.CachedSchemaHolder");
        testRunner.enableControllerService(service);

        assertTrue(retrieve("holder").getField("name").isPresent());
    }

    @Test
    public void testDisableClearsSchemas() {
        testRunner.setProperty(service, "user", "io.github.mikethomsen.nifi.schemaregistry.pojo.User");
        testRunner.enableControllerService(service);
        testRunner.disableControllerService(service);

        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name("user")
                .build();
        assertThrows(SchemaNotFoundException.class, () -> service.retrieveSchema(identifier));
    }

    @Test
    public void testClassLoadersAreClosed() {
        List<TrackingClassLoader> loaders = new ArrayList<>();
        PojoSchemaRegistryService tracking = new PojoSchemaRegistryService() {
            @Override
            protected URLClassLoader createClassLoader(PropertyValue jars) {
                URL[] urls;
                try (URLClassLoader real = super.createClassLoader(jars)) {
                    urls = real.getURLs();
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                TrackingClassLoader loader = new TrackingClassLoader(urls, getClass().getClassLoader());
                loaders.add(loader);
                return loader;
            }
        };

        TestRunner runner = TestRunners.newTestRunner(NoOpProcessor.class);
        assertDoesNotThrow(() -> runner.addControllerService("tracking", tracking));
        runner.setProperty(tracking, PojoSchemaRegistryService.EXTENSION_JARS, TEST_POJOS_JAR);
        runner.setProperty(tracking, "user", "io.github.mikethomsen.nifi.schemaregistry.pojo.User");
        runner.setProperty(tracking, "object", "io.github.mikethomsen.nifi.schemaregistry.pojo.AnnotatedObject");
        runner.enableControllerService(tracking);

        assertFalse(loaders.isEmpty());
        assertTrue(loaders.stream().allMatch(loader -> loader.closed), "every class loader should be closed");
    }

    @Test
    public void testServiceIsRestricted() {
        Restricted restricted = PojoSchemaRegistryService.class.getAnnotation(Restricted.class);
        assertNotNull(restricted);
        Set<RequiredPermission> permissions = Arrays.stream(restricted.restrictions())
                .map(Restriction::requiredPermission)
                .collect(Collectors.toSet());
        assertEquals(Set.of(RequiredPermission.EXECUTE_CODE, RequiredPermission.REFERENCE_REMOTE_RESOURCES), permissions);
    }

    private static class TrackingClassLoader extends URLClassLoader {
        private boolean closed;

        TrackingClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
