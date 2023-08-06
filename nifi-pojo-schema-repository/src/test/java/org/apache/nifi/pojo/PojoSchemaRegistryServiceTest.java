package org.apache.nifi.pojo;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PojoSchemaRegistryServiceTest {
    private TestRunner testRunner;
    private PojoSchemaRegistryService service;

    @BeforeEach
    public void setup() throws Exception {
        testRunner = TestRunners.newTestRunner(NoOpProcessor.class);
        service = new PojoSchemaRegistryService();
        testRunner.addControllerService("service", service);
        testRunner.setProperty(service, PojoSchemaRegistryService.EXTENSION_JARS, "target/lib/test-pojos-1.0.0");
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
}
