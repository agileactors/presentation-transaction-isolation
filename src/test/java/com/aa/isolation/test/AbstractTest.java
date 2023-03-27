package com.aa.isolation.test;

import com.aa.isolation.ReadDelayedInfo;
import com.aa.isolation.SampleService;
import java.util.List;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class AbstractTest {
  protected static final Scheduler SCHEDULER = Schedulers.newBoundedElastic(4, Integer.MAX_VALUE, "async-scheduler");

  @Autowired
  SampleService sampleService;

  @Autowired
  JdbcTemplate jdbc;

  void callParallelUpdate(List<UpdateInfo> info) {
    Flux.fromIterable(info)
        .flatMap(i -> Mono.fromRunnable(
            () -> sampleService.updateColumnWhere(i.getColumn(), i.getSetValue(), i.getWhereValue())
          ).subscribeOn(SCHEDULER)
        )
        .blockLast();
  }

  void callParallelQueryOnIdAndUpdate(List<Integer> ids, boolean isUpdateOnIndexedColumn) {
    Flux.fromIterable(ids)
        .flatMap(i -> Mono.fromRunnable(() -> {
                  if (isUpdateOnIndexedColumn) {
                    sampleService.queryByIdAndUpdateIndexedColumn(i);
                  } else {
                    sampleService.queryByIdAndUpdateNonIndexedColumn(i);
                  }
                })
                .subscribeOn(SCHEDULER)
        )
        .blockLast();
  }

  void callParallelQueryOnNonIndexAndUpdate(List<Integer> ids) {
    Flux.fromIterable(ids)
        .flatMap(i -> Mono.fromRunnable(() -> sampleService.queryByNonIndexedAndUpdate(i))
                .subscribeOn(SCHEDULER)
        )
        .blockLast();
  }

  void nonRepeatableRead(Integer id, boolean isUpdateOnIndexedColumn) {
    Flux.just(0, 1)
        .flatMap(i -> Mono.fromRunnable(() -> {
                  if (i == 0) {
                    sampleService.queryByIdAndUpdateIndexedColumn(i);
                  } else {
                    sampleService.queryByIdAndUpdateNonIndexedColumn(i);
                  }
                })
                .subscribeOn(SCHEDULER)
        )
        .blockLast();
  }

  public void phantomRead(Consumer<ReadDelayedInfo> function) {
    Flux.just(0, 1)
      .flatMap(i -> Mono.fromRunnable(() -> {
            // both threads will query
            if (i == 0) {
              // but the first will update immediately the value
              sampleService.queryWaitInsert(0, true);
            } else {
              // whereas the second one will:
              // read ->
              // wait a bit so that the update goes through ->
              // re-select the entity to verify if it can read the phantom entry
              ReadDelayedInfo info = sampleService.queryWaitInsert(200, false);
              function.accept(info);
            }
          })
        .subscribeOn(SCHEDULER)
      )
      .blockLast();
  }

  public void nonRepeatableRead(Consumer<ReadDelayedInfo> function) {
    Flux.just(0, 1)
      .flatMap(i -> Mono.fromRunnable(() -> {
            // both threads will query
            if (i == 0) {
              // but the first will update immediately the value
              sampleService.queryWaitUpdate(0, true);
            } else {
              // whereas the second one will:
              // read ->
              // wait a bit so that the update goes through ->
              // re-select the entity to verify if it can read the phantom entry
              ReadDelayedInfo info = sampleService.queryWaitUpdate(200, false);
              function.accept(info);
            }
          })
        .subscribeOn(SCHEDULER)
      )
      .blockLast();
  }

  @SneakyThrows
  void sleep(Long millis) {
    Thread.sleep(millis);
  }

  protected boolean stacktraceContains(Throwable throwable, String error) {
    if (throwable.getMessage().contains(error)) {
      return true;
    } else {
      return throwable.getCause() != null && stacktraceContains(throwable.getCause(), error);
    }
  }
}
