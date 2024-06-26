/*
 * Copyright 2019 Aiven Oy and jdbc-connector-for-apache-kafka project contributors
 * Copyright 2015 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.aiven.connect.jdbc.source;

import javax.sql.rowset.serial.SerialBlob;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.kafka.connect.data.Date;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Schema.Type;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Time;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.source.SourceRecord;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Tests conversion of data types and schemas. These use the types supported by Derby, which
// might not cover everything in the SQL standards and definitely doesn't cover any non-standard
// types, but should cover most of the JDBC types which is all we see anyway
public class JdbcSourceTaskConversionTest extends JdbcSourceTaskTestBase {

    public boolean extendedMapping = true;

    @BeforeEach
    public void setup() throws Exception {
        super.setup();
        final Map<String, String> taskConfig = singleTableConfig(extendedMapping);
        taskConfig.put(JdbcSourceConnectorConfig.POLL_INTERVAL_MS_CONFIG, "1");
        task.start(taskConfig);
    }

    @AfterEach
    public void tearDown() throws Exception {
        task.stop();
        super.tearDown();
    }

    @Test
    public void testBoolean() throws Exception {
        typeConversion("BOOLEAN", false, false, Schema.BOOLEAN_SCHEMA, false);
    }

    @Test
    public void testNullableBoolean() throws Exception {
        typeConversion("BOOLEAN", true, false, Schema.OPTIONAL_BOOLEAN_SCHEMA, false);
        typeConversion("BOOLEAN", true, null, Schema.OPTIONAL_BOOLEAN_SCHEMA, null);
    }

    @Test
    public void testSmallInt() throws Exception {
        typeConversion("SMALLINT", false, 1, Schema.INT16_SCHEMA, (short) 1);
    }

    @Test
    public void testNullableSmallInt() throws Exception {
        typeConversion("SMALLINT", true, 1, Schema.OPTIONAL_INT16_SCHEMA, (short) 1);
        typeConversion("SMALLINT", true, null, Schema.OPTIONAL_INT16_SCHEMA, null);
    }

    @Test
    public void testInt() throws Exception {
        typeConversion("INTEGER", false, 1, Schema.INT32_SCHEMA, 1);
    }

    @Test
    public void testNullableInt() throws Exception {
        typeConversion("INTEGER", true, 1, Schema.OPTIONAL_INT32_SCHEMA, 1);
        typeConversion("INTEGER", true, null, Schema.OPTIONAL_INT32_SCHEMA, null);
    }

    @Test
    public void testBigInt() throws Exception {
        typeConversion("BIGINT", false, Long.MAX_VALUE, Schema.INT64_SCHEMA, Long.MAX_VALUE);
    }

    @Test
    public void testNullableBigInt() throws Exception {
        typeConversion("BIGINT", true, Long.MAX_VALUE, Schema.OPTIONAL_INT64_SCHEMA, Long.MAX_VALUE);
        typeConversion("BIGINT", true, null, Schema.OPTIONAL_INT64_SCHEMA, null);
    }

    @Test
    public void testReal() throws Exception {
        typeConversion("REAL", false, 1, Schema.FLOAT32_SCHEMA, 1.f);
    }

    @Test
    public void testNullableReal() throws Exception {
        typeConversion("REAL", true, 1, Schema.OPTIONAL_FLOAT32_SCHEMA, 1.f);
        typeConversion("REAL", true, null, Schema.OPTIONAL_FLOAT32_SCHEMA, null);
    }

    @Test
    public void testDouble() throws Exception {
        typeConversion("DOUBLE", false, 1, Schema.FLOAT64_SCHEMA, 1.0);
    }

    @Test
    public void testNullableDouble() throws Exception {
        typeConversion("DOUBLE", true, 1, Schema.OPTIONAL_FLOAT64_SCHEMA, 1.0);
        typeConversion("DOUBLE", true, null, Schema.OPTIONAL_FLOAT64_SCHEMA, null);
    }

    @Test
    public void testChar() throws Exception {
        // Converted to string, so fixed size not checked
        typeConversion("CHAR(5)", false, "a", Schema.STRING_SCHEMA, "a    ");
    }

    @Test
    public void testNullableChar() throws Exception {
        // Converted to string, so fixed size not checked
        typeConversion("CHAR(5)", true, "a", Schema.OPTIONAL_STRING_SCHEMA, "a    ");
        typeConversion("CHAR(5)", true, null, Schema.OPTIONAL_STRING_SCHEMA, null);
    }

    @Test
    public void testVarChar() throws Exception {
        // Converted to string, so fixed size not checked
        typeConversion("VARCHAR(5)", false, "a", Schema.STRING_SCHEMA, "a");
    }

    @Test
    public void testNullableVarChar() throws Exception {
        // Converted to string, so fixed size not checked
        typeConversion("VARCHAR(5)", true, "a", Schema.OPTIONAL_STRING_SCHEMA, "a");
        typeConversion("VARCHAR(5)", true, null, Schema.OPTIONAL_STRING_SCHEMA, null);
    }

    @Test
    public void testBlob() throws Exception {
        // BLOB is varying size but can specify a max size so we specify that size in the spec but
        // expect BYTES not FIXED back.
        typeConversion("BLOB(5)", false, new SerialBlob("a".getBytes()), Schema.BYTES_SCHEMA,
            "a".getBytes());
    }

    @Test
    public void testNullableBlob() throws Exception {
        typeConversion("BLOB(5)", true, new SerialBlob("a".getBytes()), Schema.OPTIONAL_BYTES_SCHEMA,
            "a".getBytes());
        typeConversion("BLOB(5)", true, null, Schema.OPTIONAL_BYTES_SCHEMA, null);
    }

    @Test
    public void testClob() throws Exception {
        // CLOB is varying size but can specify a max size so we specify that size in the spec but
        // expect BYTES not FIXED back.
        typeConversion("CLOB(5)", false, "a", Schema.STRING_SCHEMA, "a");
    }

    @Test
    public void testNullableClob() throws Exception {
        typeConversion("CLOB(5)", true, "a", Schema.OPTIONAL_STRING_SCHEMA, "a");
        typeConversion("CLOB(5)", true, null, Schema.OPTIONAL_STRING_SCHEMA, null);
    }

    @Test
    public void testBinary() throws Exception {
        typeConversion("CHAR(5) FOR BIT DATA", false, "a".getBytes(), Schema.BYTES_SCHEMA,
            "a    ".getBytes());
    }

    @Test
    public void testNullableBinary() throws Exception {
        typeConversion("CHAR(5) FOR BIT DATA", true, "a".getBytes(), Schema.OPTIONAL_BYTES_SCHEMA,
            "a    ".getBytes());
        typeConversion("CHAR(5) FOR BIT DATA", true, null, Schema.OPTIONAL_BYTES_SCHEMA, null);
    }

    @Test
    public void testNumeric() throws Exception {
        typeConversion("NUMERIC(1)", false,
            new EmbeddedDerby.Literal("CAST (1 AS NUMERIC)"),
            Schema.INT8_SCHEMA, Byte.valueOf("1"));
        typeConversion("NUMERIC(3)", false,
            new EmbeddedDerby.Literal("CAST (123 AS NUMERIC)"),
            Schema.INT16_SCHEMA, Short.valueOf("123"));
        typeConversion("NUMERIC(5)", false,
            new EmbeddedDerby.Literal("CAST (12345 AS NUMERIC)"),
            Schema.INT32_SCHEMA, Integer.valueOf("12345"));
        typeConversion("NUMERIC(10)", false,
            new EmbeddedDerby.Literal("CAST (1234567890 AS NUMERIC(10))"),
            Schema.INT64_SCHEMA, Long.valueOf("1234567890"));
    }

    @Test
    public void testDecimal() throws Exception {
        typeConversion("DECIMAL(5,2)", false,
            new EmbeddedDerby.Literal("CAST (123.45 AS DECIMAL(5,2))"),
            Decimal.schema(2), new BigDecimal(new BigInteger("12345"), 2));
    }

    @Test
    public void testNullableDecimal() throws Exception {
        typeConversion("DECIMAL(5,2)", true,
            new EmbeddedDerby.Literal("CAST(123.45 AS DECIMAL(5,2))"),
            Decimal.builder(2).optional().build(),
            new BigDecimal(new BigInteger("12345"), 2));
        typeConversion("DECIMAL(5,2)", true, null, Decimal.builder(2).optional().build(),
            null);
    }

    @Test
    public void testDate() throws Exception {
        final GregorianCalendar expected = new GregorianCalendar(1977, Calendar.FEBRUARY, 13, 0, 0, 0);
        expected.setTimeZone(TimeZone.getTimeZone("UTC"));
        typeConversion("DATE", false, "1977-02-13",
            Date.builder().build(),
            expected.getTime());
    }

    @Test
    public void testNullableDate() throws Exception {
        final GregorianCalendar expected = new GregorianCalendar(1977, Calendar.FEBRUARY, 13, 0, 0, 0);
        expected.setTimeZone(TimeZone.getTimeZone("UTC"));
        typeConversion("DATE", true, "1977-02-13",
            Date.builder().optional().build(),
            expected.getTime());
        typeConversion("DATE", true, null,
            Date.builder().optional().build(),
            null);
    }

    @Test
    public void testTime() throws Exception {
        final GregorianCalendar expected = new GregorianCalendar(1970, Calendar.JANUARY, 1, 23, 3, 20);
        expected.setTimeZone(TimeZone.getTimeZone("UTC"));
        typeConversion("TIME", false, "23:03:20",
            Time.builder().build(),
            expected.getTime());
    }

    @Test
    public void testNullableTime() throws Exception {
        final GregorianCalendar expected = new GregorianCalendar(1970, Calendar.JANUARY, 1, 23, 3, 20);
        expected.setTimeZone(TimeZone.getTimeZone("UTC"));
        typeConversion("TIME", true, "23:03:20",
            Time.builder().optional().build(),
            expected.getTime());
        typeConversion("TIME", true, null,
            Time.builder().optional().build(),
            null);
    }

    @Test
    public void testTimestamp() throws Exception {
        final GregorianCalendar expected = new GregorianCalendar(1977, Calendar.FEBRUARY, 13, 23, 3, 20);
        expected.setTimeZone(TimeZone.getTimeZone("UTC"));
        typeConversion("TIMESTAMP", false, "1977-02-13 23:03:20",
            Timestamp.builder().build(),
            java.sql.Timestamp.from(expected.getTime().toInstant()));
    }

    @Test
    public void testNullableTimestamp() throws Exception {
        final GregorianCalendar expected = new GregorianCalendar(1977, Calendar.FEBRUARY, 13, 23, 3, 20);
        expected.setTimeZone(TimeZone.getTimeZone("UTC"));
        typeConversion("TIMESTAMP", true, "1977-02-13 23:03:20",
            Timestamp.builder().optional().build(),
            java.sql.Timestamp.from(expected.getTime().toInstant()));
        typeConversion("TIMESTAMP", true, null,
            Timestamp.builder().optional().build(),
            null);
    }

    // Derby has an XML type, but the JDBC driver doesn't implement any of the type bindings,
    // returning strings instead, so the XML type is not tested here

    private void typeConversion(final String sqlType, final boolean nullable,
                                final Object sqlValue, final Schema convertedSchema,
                                final Object convertedValue) throws Exception {
        String sqlColumnSpec = sqlType;
        if (!nullable) {
            sqlColumnSpec += " NOT NULL";
        }
        db.createTable(SINGLE_TABLE_NAME, "id", sqlColumnSpec);
        db.insert(SINGLE_TABLE_NAME, "id", sqlValue);
        List<SourceRecord> records = null;
        // May need to retry polling
        for (int retries = 0; retries < 10 && records == null; retries++) {
            records = task.poll();
        }
        validateRecords(records, convertedSchema, convertedValue);
        db.dropTable(SINGLE_TABLE_NAME);
    }

    /**
     * Validates schema and type of returned record data. Assumes single-field values since this is
     * only used for validating type information.
     */
    private void validateRecords(final List<SourceRecord> records, final Schema expectedFieldSchema,
                                 final Object expectedValue) {
        // Validate # of records and object type
        assertThat(records).hasSize(1);
        final Object objValue = records.get(0).value();
        assertThat(objValue).isInstanceOf(Struct.class);
        final Struct value = (Struct) objValue;

        // Validate schema
        final Schema schema = value.schema();
        assertThat(schema.type()).isEqualTo(Type.STRUCT);
        final List<Field> fields = schema.fields();

        assertThat(fields).hasSize(1);

        final Schema fieldSchema = fields.get(0).schema();
        assertThat(fieldSchema).isEqualTo(expectedFieldSchema);
        if (expectedValue instanceof byte[]) {
            assertThat(value.get(fields.get(0))).isInstanceOf(byte[].class);
            assertThat(ByteBuffer.wrap((byte[]) value.get(fields.get(0))))
                .isEqualTo(ByteBuffer.wrap((byte[]) expectedValue));
        } else {
            assertThat(value.get(fields.get(0))).isEqualTo(expectedValue);
        }
    }
}
