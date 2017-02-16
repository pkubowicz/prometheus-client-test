package com.workday.prometheus.test;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
  private static final int WARM_UP_ITERATIONS = 2_000;
  private static final int ITERATIONS = 10_000;

  private static class Implementation {
    private final String description;
    private final BiConsumer<StringWriter, CollectorRegistry> formatImplementation;

    Implementation(String description, BiConsumer<StringWriter, CollectorRegistry> formatImplementation) {
      this.description = description;
      this.formatImplementation = formatImplementation;
    }
  }

  private static final List<Implementation> IMPLEMENTATIONS = Arrays.asList(
      new Implementation("Performant2TextFormat", Main::runPerformant2TextFormat),
      new Implementation("PerformantTextFormat", Main::runPerformantTextFormat),
//      new Implementation("InterpolationTextFormat", Main::runInterpolationTextFormat),
      new Implementation("StringBuilderTextFormat", Main::runStringBuilderTextFormat),
      new Implementation("LegacyTextFormat", Main::runLegacyTextFormat)
  );

  public static void main(String[] args) throws IOException {
    populateCollectorRegistry();
    String result = runOnce(IMPLEMENTATIONS.get(0));
    System.out.printf("%d warm-up iterations, %d iterators with metrics string of length %d\n",
        WARM_UP_ITERATIONS, ITERATIONS, result.length());

    List<Implementation> implementations = getImplementations(args);

    System.out.println("Warming up");
    for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
      implementations.forEach(Main::runOnce);
    }
    System.gc();
    System.out.println("Warmed up");

    implementations.forEach(Main::runAndReport);
  }

  private static void populateCollectorRegistry() {
    IntStream.rangeClosed(1, 100).forEach(i ->
        new Counter.Builder().name("simple_counter" + i).help("short help")
            .register().inc(i)
    );
    IntStream.rangeClosed(1, 40).forEach(i ->
        new Counter.Builder().name("long_help_counter" + i).help("this help is a bit longer than help text for other elements")
            .register().inc(i * 100)
    );
    IntStream.rangeClosed(1, 20).forEach(i -> {
      Gauge gauge = new Gauge.Builder().name("throwaway" + i).help("long\\help\nwith\\escaping").register();
      gauge.set(1.0d / 3);
      Counter counter = new Counter.Builder().name("with_escaped" + i).help("long\\help\nwith\\escaping")
          .labelNames("escaped_label")
          .register();
      counter.labels("slash\\quote\\newline\nlabel1").inc();
      counter.labels("slash\\quote\\newline\nlabel2").inc();
      Counter manyLabelsCounter = new Counter.Builder().name("many_labels" + i).help("short help")
          .labelNames("l1")
          .register();
      IntStream.rangeClosed(1, 15).forEach(j -> manyLabelsCounter.labels("lv" + j).inc(j));
    });
  }

  private static void runAndReport(Implementation implementation) {
    runOnce(implementation);
    System.out.printf("   Starting %25s\n", implementation.description);
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < ITERATIONS; i++) {
      runOnce(implementation);
    }
    long durationMillis = System.currentTimeMillis() - startTime;
    System.gc();
    System.out.printf("   Finished %25s in %5d ms\n", implementation.description, durationMillis);
  }

  private static String runOnce(Implementation implementation) {
    try (StringWriter sw = new StringWriter(10240)) {
      implementation.formatImplementation.accept(sw, CollectorRegistry.defaultRegistry);
      return sw.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void runLegacyTextFormat(StringWriter sw, CollectorRegistry registry) {
    try {
      TextFormat.write004(sw, registry.metricFamilySamples());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void runPerformantTextFormat(StringWriter sw, CollectorRegistry registry) {
    try {
      PerformantTextFormat.write004(sw, registry.metricFamilySamples());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void runPerformant2TextFormat(StringWriter sw, CollectorRegistry registry) {
    try {
      PerformantTextFormat2.write004(sw, registry.metricFamilySamples());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void runInterpolationTextFormat(StringWriter sw, CollectorRegistry registry) {
    try {
      InterpolationTextFormat.write004(sw, registry.metricFamilySamples());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void runStringBuilderTextFormat(StringWriter sw, CollectorRegistry registry) {
    try {
      StringBuilderTextFormat.write004(sw, registry.metricFamilySamples());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<Implementation> getImplementations(String[] args) {
    final List<Implementation> implementations;
    if (args.length > 0) {
      implementations = IMPLEMENTATIONS.stream().filter(implementation -> implementation.description.equals(args[0]))
          .collect(Collectors.toList());
      if (implementations.isEmpty()) {
        System.err.println("Unknown implementation: " + args[0] + "; pass any of " +
            IMPLEMENTATIONS.stream().map(i -> i.description).sorted().collect(Collectors.joining(",")));
        System.exit(1);
      }
    } else {
      implementations = new ArrayList<>(IMPLEMENTATIONS);
      Collections.shuffle(implementations);
    }
    return Collections.unmodifiableList(implementations);
  }
}
