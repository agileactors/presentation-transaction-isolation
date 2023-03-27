package com.aa.isolation.test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
//@ActiveProfiles({"serializable", "crdb"})
public class IsolationSerializableCommonFailureTest extends AbstractTest {

  @Test
  public void shouldFailToUpdateNonSerializableTransactions() {
    // This will fail because there's a possibility that the final db state can't be the result of
    // a specific serial execution of the transactions
    Exception exception = assertThrows(DataAccessException.class, () ->
            callParallelUpdate(Arrays.asList(
                    UpdateInfo.builder().column("marble_colour").setValue("colour-a").whereValue("colour-z").build(),
                    UpdateInfo.builder().column("marble_colour").setValue("colour-z").whereValue("colour-a").build()
            ))
    );

    log.error(exception.getMessage(), exception);

    assertTrue(
          // psql serialization error
          stacktraceContains(exception, "ERROR: could not serialize access due to read/write dependencies among transactions") ||
          // or crdb serialization error
          stacktraceContains(exception, "ERROR: restart transaction: TransactionRetryWithProtoRefreshError")
    );
  }

  @Test
  public void shouldFailDueToTableScanAndAnyUpdate() {
    Exception exception = assertThrows(DataAccessException.class, () -> callParallelQueryOnNonIndexAndUpdate(Arrays.asList(1, 2)));

    log.error(exception.getMessage(), exception);

    assertTrue(
            // psql serialization error
            stacktraceContains(exception, "ERROR: could not serialize access due to read/write dependencies among transactions") ||
            // or crdb serialization error
            stacktraceContains(exception, "ERROR: restart transaction: TransactionRetryWithProtoRefreshError")
    );
  }

}
