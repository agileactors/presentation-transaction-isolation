package com.aa.isolation;

import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class SampleInitializr {

  @Autowired
  JdbcTemplate jdbc;

  @EventListener(ApplicationReadyEvent.class)
  public void doSomethingAfterStartup() {
    jdbc.execute("delete from ser_sample");
    StringBuilder builder = new StringBuilder("INSERT INTO ser_sample (id,indexed_column,non_indexed_column,marble_colour) VALUES ");
    for (int i = 1; i < 10000; i++) {
      builder.append(String.format("(%s,'indexed%s','notindexed%s','%s'),", i, i, i, marbleColour(i)));
    }
    builder.append("(10000,'indexed10000','notindexed10000','black')");
    jdbc.execute(builder.toString());
  }

  private String marbleColour(int index) {
    if (index == 1) {
      return "colour-a";
    } else if (index == 9999) {
      return "colour-z";
    } else {
      return "colour-b" + new Random().nextInt(1000);
    }
  }
}
