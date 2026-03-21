package com.example.maven.plugins.oracle2h2;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.junit.Assert.fail;

/**
 * Unit tests for {@link ReplicateSchemasMojo}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReplicateSchemasMojoTest {

    private ReplicateSchemasMojo mojo;

    @Before
    public void setUp() throws Exception {
        mojo = new ReplicateSchemasMojo();
        set(mojo, "oracleJdbcUrl", "jdbc:oracle:thin:@//localhost:1521/dummyschema");
        set(mojo, "oracleUser", "u");
        set(mojo, "oraclePassword", "p");
        set(mojo, "schemas", "SCOTT");
        set(mojo, "h2JdbcUrl", "jdbc:h2:mem:mojo_test;MODE=Oracle;DB_CLOSE_DELAY=-1");
        set(mojo, "skip", true);
    }

    @Test
    public void executeWithSkipTrueCompletesWithoutException() throws Exception {
        mojo.execute();
        // no exception = success
    }

    @Test(expected = MojoFailureException.class)
    public void executeWithEmptySchemasThrowsMojoFailureException() throws Exception {
        set(mojo, "skip", false);
        set(mojo, "schemas", "");
        set(mojo, "schemaList", null);
        mojo.execute();
        fail("expected MojoFailureException for empty schemas");
    }

    private static void set(Object target, String fieldName, Object value) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
