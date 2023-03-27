package com.aa.isolation.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
@ActiveProfiles({"read-committed", "pgdb"})
public class IsolationReadCommittedTest extends AbstractTest {

  @Test
  public void shouldBeOkToUpdateNonSerializableTransactions() {
    assertDoesNotThrow(() ->
        callParallelUpdate(Arrays.asList(
            UpdateInfo.builder().column("marble_colour").setValue("colour-a").whereValue("colour-z").build(),
            UpdateInfo.builder().column("marble_colour").setValue("colour-z").whereValue("colour-a").build()
        ))
    );

    assertEquals(1, jdbc.queryForObject("select count(0) from ser_sample where marble_colour = 'colour-a'", Integer.class));
    assertEquals(1, jdbc.queryForObject("select count(0) from ser_sample where marble_colour = 'colour-z'", Integer.class));
  }

  @Test
  public void phantomReadTest() {
    phantomRead(o -> Assertions.assertNotEquals(o.getInitialValue(), o.getDelayedValue()));
  }

  @Test
  public void nonRepeatableReadTest() {
    nonRepeatableRead(o -> Assertions.assertNotEquals(o.getInitialValue(), o.getDelayedValue()));
  }

}
