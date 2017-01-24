package com.workday.prometheus.test;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.Gauge.Timer;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

public class Main {
    private static final int ITERATIONS = 10000;

    public static void main(String[] args) throws IOException {
        DefaultExports.initialize();

        // warmup run
        runPerformantTextFormat();
        Gauge throwawayGauge = new Gauge.Builder().name("throwaway").help("time performant code").create();
        Timer throwawayTimer = throwawayGauge.startTimer();
        for (int i = 0; i < ITERATIONS; i++) {
            runPerformantTextFormat();
        }
        throwawayTimer.close();

        // test new 'performant' code
        runPerformantTextFormat();
        Gauge perfGauge = new Gauge.Builder().name("performant").help("time performant code").create();
        Timer perfTimer = perfGauge.startTimer();
        for (int i = 0; i < ITERATIONS; i++) {
            runPerformantTextFormat();
        }
        perfTimer.close();
        printStats(perfGauge.collect());

        // test new 'interpolation' code
        runInterpolationTextFormat();
        Gauge interGauge = new Gauge.Builder().name("interpolation").help("time interpolation code").create();
        Timer interTimer = interGauge.startTimer();
        for (int i = 0; i < ITERATIONS; i++) {
            runInterpolationTextFormat();
        }
        interTimer.close();
        printStats(interGauge.collect());

        // test new 'stringbuilder' code
        runStringBuilderTextFormat();
        Gauge sbGauge = new Gauge.Builder().name("stringbuilder").help("time stringbuilder code").create();
        Timer sbTimer = sbGauge.startTimer();
        for (int i = 0; i < ITERATIONS; i++) {
            runStringBuilderTextFormat();
        }
        sbTimer.close();
        printStats(sbGauge.collect());

        // test 0.0.19 release code
        runLegacyTextFormat();
        Gauge legacyGauge = new Gauge.Builder().name("legacy").help("time legacy code").create();
        Timer legacyTimer = legacyGauge.startTimer();
        for (int i = 0; i < ITERATIONS; i++) {
            runLegacyTextFormat();
        }
        legacyTimer.close();
        printStats(legacyGauge.collect());
    }

    static void runLegacyTextFormat() throws IOException {
        StringWriter sw = new StringWriter(10240);
        TextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
        sw.close();
    }

    static void runPerformantTextFormat() throws IOException {
        StringWriter sw = new StringWriter(10240);
        PerformantTextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
        sw.close();
    }

    static void runInterpolationTextFormat() throws IOException {
        StringWriter sw = new StringWriter(10240);
        InterpolationTextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
        sw.close();
    }

    static void runStringBuilderTextFormat() throws IOException {
        StringWriter sw = new StringWriter(10240);
        StringBuilderTextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
        sw.close();
    }

    static void printStats(List<Collector.MetricFamilySamples> samples) throws IOException {
        StringWriter sw = new StringWriter(10240);
        TextFormat.write004(sw, Collections.enumeration(samples));
        sw.close();
        System.out.println(sw.toString());
    }

}
