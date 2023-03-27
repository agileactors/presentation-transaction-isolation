package com.aa.isolation;

import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class SampleService {

  @Autowired
  JdbcTemplate jdbc;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void queryByIdAndUpdateIndexedColumn(Integer id) {
    jdbc.queryForRowSet("select * from ser_sample where id=?", id);
    jdbc.execute("update ser_sample set indexed_column = '" + new Random().nextInt() + "' where id = '" + id + "'");
    log.info("Queried id {}, updated indexed_column", id);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void queryByNonIndexedAndUpdate(Integer id) {
    jdbc.queryForRowSet("select * from ser_sample where non_indexed_column=?", "notindexed" + id);
    jdbc.execute("update ser_sample set indexed_column = '" + new Random().nextInt() + "' where id = '" + id + "'");
    log.info("Queried non-indexed column {}, updated indexed_column", "notindexed" + id);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @SneakyThrows
  public ReadDelayedInfo queryWaitUpdate(long waitMillis, boolean update) {
    String query = "select indexed_column from ser_sample where id = 1";
    // both threads select the same value
    String initialValue = jdbc.queryForObject(query, String.class);
    Thread.sleep(waitMillis);
    if (update) {
      // one of the threads updates the value while the other waits a bit
      String updatedValue = initialValue + "-updated";
      jdbc.execute("update ser_sample set indexed_column = '" + updatedValue + "' where id = 1");
    }
    // on read committed, we'll see the new value, whereas serializable does not allow it
    String delayedValue = jdbc.queryForObject(query, String.class);
    return ReadDelayedInfo.builder().initialValue(initialValue).delayedValue(delayedValue).build();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @SneakyThrows
  public ReadDelayedInfo queryWaitInsert(long waitMillis, boolean insert) {
    String query = "select count(indexed_column) from ser_sample where indexed_column = 'indexed1'";
    // both threads select the same value
    Integer initialValue = jdbc.queryForObject(query, Integer.class);
    Thread.sleep(waitMillis);
    if (insert) {
      // one of the threads inserts a new entry while the other waits a bit
      jdbc.execute("INSERT INTO ser_sample (id,indexed_column,non_indexed_column,marble_colour) VALUES ( '" + Integer.MAX_VALUE + "', 'indexed1', 'notindexed1', 'whateva-colour')");
    }
    // on read committed, we'll see the new value, whereas serializable does not allow it
    Integer delayedValue = jdbc.queryForObject(query, Integer.class);
    return ReadDelayedInfo.builder().initialValue(initialValue).delayedValue(delayedValue).build();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void queryByIdAndUpdateNonIndexedColumn(Integer id) {
    jdbc.queryForRowSet("select * from ser_sample where id=?", id);
    jdbc.execute("update ser_sample set non_indexed_column = '" + new Random().nextInt() + "' where id = '" + id + "'");
    log.info("Queried id {}, updated non_indexed_column", id);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @SneakyThrows
  public void updateColumnWhere(String columnName, String value, String whereValue) {
    jdbc.execute("update ser_sample set " + columnName + " = '" + value + "' where " + columnName + " = '" + whereValue + "'");
    Thread.sleep(20);
    log.info("Updated column {}, set {} where {}", columnName, value, whereValue);
  }

}
