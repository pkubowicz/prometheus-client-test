# prometheus-client-test

Performance test of Prometheus Java client text formatting.

## Measure time

    ./gradlew shadowJar && java -jar build/libs/prometheus-client-test-all.jar

## Measure memory efficiency

    ./gradlew shadowJar && java -XX:+PrintGC -jar build/libs/prometheus-client-test-all.jar PerformantTextFormat

Manually copy GC output between lines `Starting` and `Finished` to a GC analyser like [GCViewer](https://github.com/chewiebug/GCViewer).
