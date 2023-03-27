package com.aa.isolation.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.util.PSQLException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
//@ActiveProfiles({"serializable", "pgdb"})
public class IsolationSerializableTest extends AbstractTest {

  @Test
  @DisabledIfEnvironmentVariable(named = "SPRING_PROFILES_ACTIVE", matches = ".*crdb.*")
  public void shouldFailDueToSIReadLockOnSameIndexPageAndUpdateOnIndexedColumn() {
    /* ========== Scenario: False positive on PostgreSQL ========== */
    Exception exception = assertThrows(JpaSystemException.class, () -> callParallelQueryOnIdAndUpdate(Arrays.asList(1, 2), true));

    assertEquals(exception.getCause().getCause().getClass(), PSQLException.class);
    assertEquals("ERROR: could not serialize access due to read/write dependencies among transactions", exception.getCause().getCause().getMessage());
  }

  @Test
  public void shouldSucceedBecauseSIReadLockOnDifferentIndexPageAndUpdateOnIndexedColumn() {
    // We are querying index entries NOT in the same page (id 1 and 10000, different index pages)
    // No error when updating an indexed or non-indexed column
    assertDoesNotThrow(() -> callParallelQueryOnIdAndUpdate(Arrays.asList(1, 10000), true));
    assertDoesNotThrow(() -> callParallelQueryOnIdAndUpdate(Arrays.asList(1, 10000), false));
  }

  @Test
  public void phantomReadTest() {
    phantomRead(o -> Assertions.assertEquals(o.getInitialValue(), o.getDelayedValue()));
  }

  @Test
  public void nonRepeatableReadTest() {
    nonRepeatableRead(o -> Assertions.assertEquals(o.getInitialValue(), o.getDelayedValue()));
  }

  @Test
  @Disabled("This may yield a serializable error occasionally! It is random! (due to sync on PostgreSQL probably)")
  public void shouldSucceedForUpdatesOnNonIndexedColumn() {
    // We are querying side-by-side index entries (id 1 and 2, same index page)
    // This leads to SIRead lock from both threads on the same index page
    // Most times this does not fail, because the column is not indexed, but it may randomly
    // fail, like the indexed column update does all the time
    assertDoesNotThrow(() -> callParallelQueryOnIdAndUpdate(Arrays.asList(1, 2), false));
  }
}
