package se.skltpnext.experiment001.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import se.skltpnext.experiment001.ExperimentConfig;
import se.skltpnext.experiment001.evidence.JsonSupport;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TelemetryRecorder implements AutoCloseable {
    private static final Set<String> DECISION_FIELDS = Set.of(
            "runId", "scenarioId", "variantId", "releaseId", "component", "checkpoint",
            "category", "result", "reason", "policyVersion", "endpointId");
    private final Path runtimeRoot;
    private final String runId;
    private final String scenarioId;
    private final String variantId;
    private final String component;
    private final InMemorySpanExporter exporter;
    private final SdkTracerProvider tracerProvider;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public TelemetryRecorder(Path runtimeRoot, String runId, String scenarioId,
                             String variantId, String component) {
        this.runtimeRoot = runtimeRoot;
        this.runId = runId;
        this.scenarioId = scenarioId;
        this.variantId = variantId;
        this.component = component;
        exporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(io.opentelemetry.context.propagation.ContextPropagators.create(
                        W3CTraceContextPropagator.getInstance()))
                .build();
        tracer = openTelemetry.getTracer("se.skltpnext.experiment001", "1.0.0");
    }

    public Span startConsumerSpan(String traceparent) {
        Map<String, String> carrier = Map.of("traceparent", traceparent);
        Context extracted = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), carrier, MAP_GETTER);
        return tracer.spanBuilder("consumer.direct-read")
                .setParent(extracted)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("experiment.run_id", runId)
                .setAttribute("experiment.scenario_id", scenarioId)
                .setAttribute("experiment.variant_id", variantId)
                .setAttribute("experiment.release_id", ExperimentConfig.RELEASE_ID)
                .setAttribute("experiment.component", component)
                .startSpan();
    }

    public Span startServerSpan(Map<String, List<String>> headers) {
        Map<String, String> carrier = new HashMap<>();
        headers.forEach((key, values) -> {
            if (!values.isEmpty()) {
                carrier.put(key.toLowerCase(), values.getFirst());
            }
        });
        Context extracted = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), carrier, MAP_GETTER);
        return tracer.spanBuilder("producer.synthetic-read")
                .setParent(extracted)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("experiment.run_id", runId)
                .setAttribute("experiment.scenario_id", scenarioId)
                .setAttribute("experiment.variant_id", variantId)
                .setAttribute("experiment.release_id", ExperimentConfig.RELEASE_ID)
                .setAttribute("experiment.component", component)
                .startSpan();
    }

    public void inject(Context context, Map<String, String> headers) {
        openTelemetry.getPropagators().getTextMapPropagator().inject(context, headers, MAP_SETTER);
    }

    public void endAndExport(Span span, boolean success) {
        span.setStatus(success ? StatusCode.OK : StatusCode.ERROR);
        span.end();
        tracerProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);
        for (SpanData data : exporter.getFinishedSpanItems()) {
            Map<String, Object> safe = new java.util.LinkedHashMap<>();
            safe.put("runId", runId);
            safe.put("scenarioId", scenarioId);
            safe.put("variantId", variantId);
            safe.put("releaseId", ExperimentConfig.RELEASE_ID);
            safe.put("component", component);
            safe.put("name", data.getName());
            safe.put("traceId", data.getTraceId());
            safe.put("spanId", data.getSpanId());
            safe.put("parentSpanId", data.getParentSpanId());
            safe.put("status", data.getStatus().getStatusCode().name().toLowerCase());
            JsonSupport.appendJsonLine(runtimeRoot.resolve("events/telemetry/spans.jsonl"), safe);
        }
        exporter.reset();
    }

    public void warmUpWithoutEvidence() {
        Span span = startServerSpan(Map.of(
                "traceparent", List.of("00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-bbbbbbbbbbbbbbbb-01")));
        span.end();
        tracerProvider.forceFlush().join(5, java.util.concurrent.TimeUnit.SECONDS);
        exporter.reset();
    }

    public void decision(String checkpoint, String category, String result, String reason) {
        Map<String, Object> event = new java.util.LinkedHashMap<>();
        event.put("runId", runId);
        event.put("scenarioId", scenarioId);
        event.put("variantId", variantId);
        event.put("releaseId", ExperimentConfig.RELEASE_ID);
        event.put("component", component);
        event.put("checkpoint", checkpoint);
        event.put("category", category);
        event.put("result", result);
        event.put("reason", reason);
        if ("authorization".equals(category)) {
            event.put("policyVersion", ExperimentConfig.POLICY_VERSION);
        }
        writeAllowlisted(runtimeRoot.resolve("events/telemetry/decisions.jsonl"), event, DECISION_FIELDS);
    }

    public void contract(String role, String phase, String result) {
        JsonSupport.appendJsonLine(runtimeRoot.resolve("events/contract/validations.jsonl"), Map.of(
                "runId", runId,
                "scenarioId", scenarioId,
                "variantId", variantId,
                "role", role,
                "phase", phase,
                "contractId", ExperimentConfig.CONTRACT_ID,
                "contractVersion", ExperimentConfig.CONTRACT_VERSION,
                "validator", "kappa-2.0.5",
                "result", result));
    }

    public void dependency(String dependency, String result, long durationMillis) {
        JsonSupport.appendJsonLine(runtimeRoot.resolve("events/telemetry/dependencies.jsonl"), Map.of(
                "runId", runId,
                "scenarioId", scenarioId,
                "variantId", variantId,
                "dependency", dependency,
                "attempts", 1,
                "result", result,
                "durationBucket", durationMillis < 100 ? "under-100ms" : "100ms-or-more"));
    }

    public void network(String sender, String receiver, String listenerId,
                        String method, String pathTemplate, boolean apiDataReceived) {
        JsonSupport.appendJsonLine(runtimeRoot.resolve("events/network/payload-call-ledger.jsonl"), Map.of(
                "runId", runId,
                "scenarioId", scenarioId,
                "variantId", variantId,
                "sender", sender,
                "receiver", receiver,
                "listenerId", listenerId,
                "method", method,
                "pathTemplate", pathTemplate,
                "apiDataReceived", apiDataReceived));
    }

    public String audit(String checkpoint, String result, String reason) {
        String auditId = "AUDIT-" + UUID.randomUUID();
        Map<String, Object> record = new java.util.LinkedHashMap<>();
        record.put("auditRecordId", auditId);
        record.put("runId", runId);
        record.put("scenarioId", scenarioId);
        record.put("variantId", variantId);
        record.put("decidingParty", "producer-b");
        record.put("organizationRef", "ORG_A");
        record.put("systemRef", "SYSTEM_A");
        record.put("clientRef", "CLIENT_A");
        record.put("releaseVersion", ExperimentConfig.RELEASE_VERSION);
        record.put("policyVersion", ExperimentConfig.POLICY_VERSION);
        record.put("checkpoint", checkpoint);
        record.put("result", result);
        record.put("reason", reason);
        JsonSupport.appendJsonLine(runtimeRoot.resolve("events/audit/records.jsonl"), record);
        return auditId;
    }

    private static void writeAllowlisted(Path target, Map<String, Object> value, Set<String> allowlist) {
        if (!allowlist.containsAll(value.keySet())) {
            throw new IllegalArgumentException("Telemetry contains a non-allowlisted field");
        }
        JsonSupport.appendJsonLine(target, value);
    }

    @Override
    public void close() {
        tracerProvider.close();
    }

    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key.toLowerCase());
        }
    };

    private static final TextMapSetter<Map<String, String>> MAP_SETTER = Map::put;
}
