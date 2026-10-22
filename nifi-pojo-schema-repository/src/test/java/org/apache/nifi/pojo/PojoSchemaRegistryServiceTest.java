package org.apache.nifi.pojo;

import org.apache.nifi.schema.access.SchemaNotFoundException;
import org.apache.nifi.serialization.record.RecordField;
import org.apache.nifi.serialization.record.RecordSchema;
import org.apache.nifi.serialization.record.SchemaIdentifier;
import org.apache.nifi.serialization.record.StandardSchemaIdentifier;
import org.apache.nifi.util.NoOpProcessor;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PojoSchemaRegistryServiceTest {
    private TestRunner testRunner;
    private PojoSchemaRegistryService service;

    @BeforeEach
    public void setup() throws Exception {
        testRunner = TestRunners.newTestRunner(NoOpProcessor.class);
        service = new PojoSchemaRegistryService();
        testRunner.addControllerService("service", service);
        testRunner.setProperty(service, PojoSchemaRegistryService.EXTENSION_JARS, "target/lib/test-pojos-2.12.0.jar");
    }

    @Test
    public void testAvroGeneratedObject() {
        testRunner.setProperty(service, "message", "org.apache.nifi.pojo.MessageRecord");
        testRunner.enableControllerService(service);

        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name("message")
                .build();
        AtomicReference<RecordSchema> schema = new AtomicReference<>();
        assertDoesNotThrow(() -> schema.set(service.retrieveSchema(identifier)));
        RecordSchema result = schema.get();
        assertEquals(2, result.getFieldNames().size());
        assertNotNull(result.getField("from"));
        assertNotNull(result.getField("message"));
    }

    @Test
    public void testJacksonGeneratedSchema() {
        testRunner.setProperty(service, "user", "org.apache.nifi.pojo.User");
        testRunner.enableControllerService(service);

        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name("user")
                .build();
        AtomicReference<RecordSchema> schema = new AtomicReference<>();
        assertDoesNotThrow(() -> schema.set(service.retrieveSchema(identifier)));
        RecordSchema result = schema.get();
        assertEquals(3, result.getFieldNames().size());
        assertNotNull(result.getField("username"));
        assertNotNull(result.getField("password"));
        assertNotNull(result.getField("failedLoginCount"));
    }

    @Test
    public void testAnnotatedPojoSchema() {
        testRunner.setProperty(service, "object", "org.apache.nifi.pojo.AnnotatedObject");
        testRunner.enableControllerService(service);

        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name("object")
                .build();
        AtomicReference<RecordSchema> schema = new AtomicReference<>();
        assertDoesNotThrow(() -> schema.set(service.retrieveSchema(identifier)));
        RecordSchema result = schema.get();
        assertEquals(3, result.getFieldNames().size());
        Optional<RecordField> aliasedField = result.getField("multipleAliases");
        Optional<RecordField> nullable = result.getField("nullable");
        Optional<RecordField> renamed = result.getField("renamed");

        assertTrue(aliasedField.isPresent());
        assertTrue(nullable.isPresent());
        assertTrue(renamed.isPresent());
    }

    @Test
    public void testUnknownSchemaNameThrows() {
        testRunner.setProperty(service, "user", "org.apache.nifi.pojo.User");
        testRunner.enableControllerService(service);

        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name("nope")
                .build();
        assertThrows(SchemaNotFoundException.class, () -> service.retrieveSchema(identifier));
    }

    @Test
    public void testMultipleExtensionJars() {
        testRunner.setProperty(service, PojoSchemaRegistryService.EXTENSION_JARS,
                "target/lib/test-pojos-2.12.0.jar, target/lib/avro-1.12.2.jar");
        testRunner.setProperty(service, "user", "org.apache.nifi.pojo.User");
        testRunner.assertValid(service);
        testRunner.enableControllerService(service);

        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name("user")
                .build();
        assertDoesNotThrow(() -> service.retrieveSchema(identifier));
    }

    @Test
    public void testUnknownClassIsInvalid() {
        testRunner.setProperty(service, "missing", "com.example.DoesNotExist");
        testRunner.assertNotValid(service);
    }

    @Test
    public void testInstanceSchemaFieldUsesGeneratedSchema() {
        testRunner.setProperty(service, "holder", "org.apache.nifi.pojo.CachedSchemaHolder");
        testRunner.enableControllerService(service);

        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name("holder")
                .build();
        AtomicReference<RecordSchema> schema = new AtomicReference<>();
        assertDoesNotThrow(() -> schema.set(service.retrieveSchema(identifier)));
        assertTrue(schema.get().getField("name").isPresent());
    }

    @Test
    public void testDisableClearsSchemas() {
        testRunner.setProperty(service, "user", "org.apache.nifi.pojo.User");
        testRunner.enableControllerService(service);
        testRunner.disableControllerService(service);

        SchemaIdentifier identifier = new StandardSchemaIdentifier.Builder()
                .name("user")
                .build();
        assertThrows(SchemaNotFoundException.class, () -> service.retrieveSchema(identifier));
    }
}
