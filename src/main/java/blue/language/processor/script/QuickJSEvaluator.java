package blue.language.processor.script;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuickJSEvaluator implements AutoCloseable {

    public interface DocumentBinding {
        Object get(String pointer);

        Object getCanonical(String pointer);
    }

    private static final Set<String> SUPPORTED_BINDINGS = Collections.unmodifiableSet(
            new java.util.LinkedHashSet<String>(Arrays.asList(
                    "event",
                    "eventCanonical",
                    "steps",
                    "document",
                    "emit",
                    "currentContract",
                    "currentContractCanonical",
                    "__documentData",
                    "__documentDataSimple",
                    "__documentDataCanonical",
                    "__scopePath")));
    private static final Pattern DOCUMENT_CALL_PATTERN = Pattern.compile("document\\((['\"])(.*?)\\1\\)");
    private static final Pattern DOCUMENT_GET_PATTERN = Pattern.compile("document\\.get\\((['\"])(.*?)\\1\\)");
    private static final Pattern DOCUMENT_CANONICAL_PATTERN = Pattern.compile("document\\.canonical\\((['\"])(.*?)\\1\\)");
    private static final Pattern DOCUMENT_GET_CANONICAL_PATTERN = Pattern.compile("document\\.getCanonical\\((['\"])(.*?)\\1\\)");

    private final ScriptRuntime runtime;

    public QuickJSEvaluator() {
        this(new QuickJsSidecarRuntime());
    }

    public QuickJSEvaluator(ScriptRuntime runtime) {
        this.runtime = runtime;
    }

    public ScriptRuntimeResult evaluate(String code, Map<String, Object> bindings, BigInteger wasmGasLimit) {
        Map<String, Object> safeBindings = normalizeBindings(code, bindings);
        Consumer<Object> emitCallback = extractEmitCallback(safeBindings);
        validateBindings(safeBindings);
        try {
            ScriptRuntimeResult runtimeResult = runtime.evaluate(
                    new ScriptRuntimeRequest(withRuntimePrelude(code), safeBindings, wasmGasLimit));
            return applyEmitCallback(runtimeResult, emitCallback);
        } catch (ScriptRuntimeException ex) {
            throw new CodeBlockEvaluationError(code, ex);
        }
    }

    private String withRuntimePrelude(String code) {
        StringBuilder prelude = new StringBuilder();
        prelude.append("const canon = {");
        prelude.append("at: function(value, pointer){");
        prelude.append("if (value === undefined || value === null) return null;");
        prelude.append("if (pointer === undefined || pointer === null || pointer === '' || pointer === '/') return value;");
        prelude.append("if (typeof pointer !== 'string') throw new TypeError('canon.at() expects a string pointer');");
        prelude.append("let current = value;");
        prelude.append("const normalized = pointer.startsWith('/') ? pointer : '/' + pointer;");
        prelude.append("const segments = normalized === '/' ? [] : normalized.substring(1).split('/').map(function(s){return s.replace(/~1/g,'/').replace(/~0/g,'~');});");
        prelude.append("for (const segment of segments) {");
        prelude.append("if (current === undefined || current === null) return null;");
        prelude.append("if (Array.isArray(current)) {");
        prelude.append("if (!/^\\d+$/.test(segment)) return null;");
        prelude.append("current = current[Number(segment)];");
        prelude.append("continue;");
        prelude.append("}");
        prelude.append("current = current[segment];");
        prelude.append("}");
        prelude.append("if (current === undefined) return null;");
        prelude.append("return current;");
        prelude.append("},");
        prelude.append("unwrap: function(value, deep){");
        prelude.append("const deepMode = deep !== false;");
        prelude.append("const unwrapInternal = function(input, recursive){");
        prelude.append("if (input === undefined || input === null) return input;");
        prelude.append("if (Array.isArray(input)) {");
        prelude.append("if (!recursive) return input;");
        prelude.append("return input.map(function(item){ return unwrapInternal(item, true); });");
        prelude.append("}");
        prelude.append("if (typeof input !== 'object') return input;");
        prelude.append("if (Object.prototype.hasOwnProperty.call(input,'value')) {");
        prelude.append("const wrappedValue = input.value;");
        prelude.append("return recursive ? unwrapInternal(wrappedValue, true) : wrappedValue;");
        prelude.append("}");
        prelude.append("if (Object.prototype.hasOwnProperty.call(input,'items') && Array.isArray(input.items)) {");
        prelude.append("if (!recursive) return input.items;");
        prelude.append("return input.items.map(function(item){ return unwrapInternal(item, true); });");
        prelude.append("}");
        prelude.append("if (!recursive) return input;");
        prelude.append("const out = {};");
        prelude.append("for (const key of Object.keys(input)) {");
        prelude.append("out[key] = unwrapInternal(input[key], true);");
        prelude.append("}");
        prelude.append("return out;");
        prelude.append("};");
        prelude.append("return unwrapInternal(value, deepMode);");
        prelude.append("}");
        prelude.append("};");
        prelude.append("const document = (function(){");
        prelude.append("const __simpleSource = (typeof __documentDataSimple !== 'undefined') ? __documentDataSimple : ((typeof __documentData !== 'undefined') ? __documentData : undefined);");
        prelude.append("const __canonicalSource = (typeof __documentDataCanonical !== 'undefined') ? __documentDataCanonical : __simpleSource;");
        prelude.append("const __scopePathValue = (typeof __scopePath !== 'undefined' && typeof __scopePath === 'string' && __scopePath.length > 0) ? __scopePath : '/';");
        prelude.append("const RAW_VALUE_SEGMENTS = new Set(['blueId','name','description','value']);");
        prelude.append("const __normalize = function(pointer){");
        prelude.append("if (pointer === undefined || pointer === null || pointer === '') return '/';");
        prelude.append("if (typeof pointer !== 'string') throw new TypeError('document() expects a string pointer');");
        prelude.append("const absolute = pointer.startsWith('/') ? pointer : (__scopePathValue === '/' ? '/' + pointer : __scopePathValue + '/' + pointer);");
        prelude.append("const compact = absolute.replace(/\\/{2,}/g,'/');");
        prelude.append("if (compact.length > 1 && compact.endsWith('/')) return compact.substring(0, compact.length - 1);");
        prelude.append("return compact || '/';");
        prelude.append("};");
        prelude.append("const __segments = function(pointer){");
        prelude.append("if (pointer === '/') return [];");
        prelude.append("return pointer.substring(1).split('/').map(function(s){return s.replace(/~1/g,'/').replace(/~0/g,'~');});");
        prelude.append("};");
        prelude.append("const __unwrapPotentialNode = function(current){");
        prelude.append("if (current && typeof current === 'object' && !Array.isArray(current) && Object.prototype.hasOwnProperty.call(current,'value')) {");
        prelude.append("const keys = Object.keys(current);");
        prelude.append("if (keys.length <= 4 && keys.every(function(k){ return k === 'value' || k === 'type' || k === 'name' || k === 'description'; })) {");
        prelude.append("return current.value;");
        prelude.append("}");
        prelude.append("}");
        prelude.append("return current;");
        prelude.append("};");
        prelude.append("const __isRawValuePointer = function(pointer){");
        prelude.append("if (pointer === '/') return false;");
        prelude.append("const idx = pointer.lastIndexOf('/');");
        prelude.append("const segment = idx >= 0 ? pointer.substring(idx + 1) : pointer;");
        prelude.append("return RAW_VALUE_SEGMENTS.has(segment);");
        prelude.append("};");
        prelude.append("const __isTypeMetadataPointer = function(pointer){");
        prelude.append("return pointer === '/type' || pointer.indexOf('/type/') >= 0;");
        prelude.append("};");
        prelude.append("const __readInternal = function(source, pointer, unwrapScalar){");
        prelude.append("if (source === undefined) return undefined;");
        prelude.append("const normalized = __normalize(pointer);");
        prelude.append("let current = source;");
        prelude.append("for (const segment of __segments(normalized)) {");
        prelude.append("if (current === undefined || current === null) return undefined;");
        prelude.append("if (Array.isArray(current)) {");
        prelude.append("if (!/^\\d+$/.test(segment)) return undefined;");
        prelude.append("const idx = Number(segment);");
        prelude.append("current = current[idx];");
        prelude.append("continue;");
        prelude.append("}");
        prelude.append("current = current[segment];");
        prelude.append("}");
        prelude.append("if (current === undefined) {");
        prelude.append("if (source === __simpleSource && __isRawValuePointer(normalized)) {");
        prelude.append("const canonicalValue = __readInternal(__canonicalSource, pointer, false);");
        prelude.append("if (canonicalValue !== undefined) return __unwrapPotentialNode(canonicalValue);");
        prelude.append("}");
        prelude.append("if (source === __simpleSource && __isTypeMetadataPointer(normalized)) {");
        prelude.append("const canonicalTypeValue = __readInternal(__canonicalSource, pointer, false);");
        prelude.append("if (canonicalTypeValue !== undefined) return __unwrapPotentialNode(canonicalTypeValue);");
        prelude.append("}");
        prelude.append("return undefined;");
        prelude.append("}");
        prelude.append("if (__isRawValuePointer(normalized)) return __unwrapPotentialNode(current);");
        prelude.append("if (unwrapScalar) return __unwrapPotentialNode(current);");
        prelude.append("return current;");
        prelude.append("};");
        prelude.append("const fn = function(pointer){ return __readInternal(__simpleSource, pointer, true); };");
        prelude.append("fn.get = function(pointer){ return __readInternal(__simpleSource, pointer, true); };");
        prelude.append("fn.canonical = function(pointer){ return __readInternal(__canonicalSource, pointer, false); };");
        prelude.append("fn.getCanonical = function(pointer){ return __readInternal(__canonicalSource, pointer, false); };");
        prelude.append("return fn;");
        prelude.append("})();");
        prelude.append("\n");
        prelude.append(code);
        return prelude.toString();
    }

    @Override
    public void close() {
        runtime.close();
    }

    private void validateBindings(Map<String, Object> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        for (String key : bindings.keySet()) {
            if (!SUPPORTED_BINDINGS.contains(key)) {
                throw new IllegalArgumentException("Unsupported QuickJS binding: \"" + key + "\"");
            }
            Object value = bindings.get(key);
            if ("document".equals(key) && value != null) {
                throw new IllegalArgumentException("QuickJS document binding must be a function");
            }
            if ("emit".equals(key) && value != null) {
                throw new IllegalArgumentException("QuickJS emit binding must be a function");
            }
        }
    }

    private Map<String, Object> normalizeBindings(String code, Map<String, Object> bindings) {
        Map<String, Object> normalized = bindings == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(bindings);
        Object event = normalized.get("event");
        if (!normalized.containsKey("event")) {
            normalized.put("event", null);
            event = null;
        }
        if (!normalized.containsKey("eventCanonical") || normalized.get("eventCanonical") == null) {
            normalized.put("eventCanonical", event);
        }
        if (!normalized.containsKey("steps") || normalized.get("steps") == null) {
            normalized.put("steps", Collections.emptyList());
        }
        Object currentContract = normalized.get("currentContract");
        if (!normalized.containsKey("currentContract")) {
            normalized.put("currentContract", null);
            currentContract = null;
        }
        if (!normalized.containsKey("currentContractCanonical")
                || normalized.get("currentContractCanonical") == null) {
            normalized.put("currentContractCanonical", currentContract);
        }
        normalizeDocumentBinding(code, normalized);
        return normalized;
    }

    private Consumer<Object> extractEmitCallback(Map<String, Object> bindings) {
        if (bindings == null || !bindings.containsKey("emit")) {
            return null;
        }
        Object emit = bindings.get("emit");
        if (emit == null) {
            return null;
        }
        if (emit instanceof Consumer) {
            bindings.remove("emit");
            @SuppressWarnings("unchecked")
            Consumer<Object> callback = (Consumer<Object>) emit;
            return callback;
        }
        throw new IllegalArgumentException("QuickJS emit binding must be a function");
    }

    private ScriptRuntimeResult applyEmitCallback(ScriptRuntimeResult runtimeResult, Consumer<Object> emitCallback) {
        if (emitCallback == null || runtimeResult == null || !(runtimeResult.value() instanceof Map)) {
            return runtimeResult;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> valueMap = (Map<String, Object>) runtimeResult.value();
        Object events = valueMap.get("events");
        if (!(events instanceof List)) {
            return runtimeResult;
        }
        @SuppressWarnings("unchecked")
        List<Object> emittedEvents = (List<Object>) events;
        for (Object emitted : emittedEvents) {
            emitCallback.accept(emitted);
        }
        if (valueMap.containsKey("__result")) {
            return new ScriptRuntimeResult(
                    valueMap.get("__result"),
                    runtimeResult.wasmGasUsed(),
                    runtimeResult.wasmGasRemaining());
        }
        Map<String, Object> stripped = new LinkedHashMap<String, Object>(valueMap);
        stripped.remove("events");
        return new ScriptRuntimeResult(stripped, runtimeResult.wasmGasUsed(), runtimeResult.wasmGasRemaining());
    }

    @SuppressWarnings("unchecked")
    private void normalizeDocumentBinding(String code, Map<String, Object> bindings) {
        if (bindings == null || !bindings.containsKey("document")) {
            return;
        }
        Object value = bindings.get("document");
        if (value == null) {
            return;
        }

        Function<String, Object> simpleReader = null;
        Function<String, Object> canonicalReader = null;
        if (value instanceof DocumentBinding) {
            final DocumentBinding binding = (DocumentBinding) value;
            simpleReader = new Function<String, Object>() {
                @Override
                public Object apply(String pointer) {
                    return binding.get(pointer);
                }
            };
            canonicalReader = new Function<String, Object>() {
                @Override
                public Object apply(String pointer) {
                    return binding.getCanonical(pointer);
                }
            };
        } else if (value instanceof Function) {
            final Function<Object, Object> binding = (Function<Object, Object>) value;
            simpleReader = new Function<String, Object>() {
                @Override
                public Object apply(String pointer) {
                    return binding.apply(pointer);
                }
            };
            canonicalReader = simpleReader;
        } else {
            throw new IllegalArgumentException("QuickJS document binding must be a function");
        }

        bindings.remove("document");
        if (bindings.get("__documentDataSimple") == null) {
            Object simpleSnapshot = buildSnapshotFromCodePointers(code, simpleReader, false);
            if (simpleSnapshot != null) {
                bindings.put("__documentDataSimple", simpleSnapshot);
            }
        }
        if (bindings.get("__documentDataCanonical") == null) {
            Object canonicalSnapshot = buildSnapshotFromCodePointers(code, canonicalReader, true);
            if (canonicalSnapshot != null) {
                bindings.put("__documentDataCanonical", canonicalSnapshot);
            }
        }
    }

    private Object buildSnapshotFromCodePointers(String code, Function<String, Object> reader, boolean canonical) {
        if (reader == null) {
            return null;
        }
        Map<String, Object> pointerValues = new LinkedHashMap<String, Object>();
        if (referencesRootDocument(code, canonical)) {
            Object rootValue = safeRead(reader, "/");
            if (rootValue != null) {
                pointerValues.put("/", rootValue);
            }
        }
        collectPointerValues(pointerValues, code, canonical ? DOCUMENT_CANONICAL_PATTERN : DOCUMENT_CALL_PATTERN, reader);
        collectPointerValues(pointerValues, code, canonical ? DOCUMENT_GET_CANONICAL_PATTERN : DOCUMENT_GET_PATTERN, reader);
        if (pointerValues.isEmpty()) {
            return null;
        }
        return buildSnapshotTree(pointerValues);
    }

    private void collectPointerValues(Map<String, Object> pointerValues,
                                      String code,
                                      Pattern pattern,
                                      Function<String, Object> reader) {
        Matcher matcher = pattern.matcher(code == null ? "" : code);
        while (matcher.find()) {
            String pointer = matcher.group(2);
            if (pointer == null || pointer.trim().isEmpty()) {
                pointer = "/";
            }
            String normalized = normalizePointer(pointer);
            if (pointerValues.containsKey(normalized)) {
                continue;
            }
            Object value = safeRead(reader, normalized);
            if (value != null) {
                pointerValues.put(normalized, value);
            }
        }
    }

    private boolean referencesRootDocument(String code, boolean canonical) {
        String source = code == null ? "" : code;
        if (canonical) {
            return source.contains("document.canonical()") || source.contains("document.getCanonical()");
        }
        return source.contains("document()") || source.contains("document.get()");
    }

    private Object safeRead(Function<String, Object> reader, String pointer) {
        try {
            return reader.apply(pointer);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Object buildSnapshotTree(Map<String, Object> pointerValues) {
        Object root = new LinkedHashMap<String, Object>();
        Object rootValue = pointerValues.get("/");
        if (rootValue instanceof Map || rootValue instanceof List) {
            root = rootValue;
        } else if (rootValue != null) {
            return rootValue;
        }

        for (Map.Entry<String, Object> entry : pointerValues.entrySet()) {
            String pointer = entry.getKey();
            if ("/".equals(pointer)) {
                continue;
            }
            List<String> segments = decodePointerSegments(pointer);
            root = insertAtPointer(root, segments, entry.getValue());
        }
        return root;
    }

    private Object insertAtPointer(Object root, List<String> segments, Object value) {
        Object current = root;
        Object parent = null;
        String parentSegment = null;
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            boolean last = i == segments.size() - 1;
            boolean numeric = isNumeric(segment);
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                if (last) {
                    map.put(segment, value);
                    return root;
                }
                Object next = map.get(segment);
                if (next == null) {
                    next = numericSegmentAhead(segments, i + 1) ? new java.util.ArrayList<Object>() : new LinkedHashMap<String, Object>();
                    map.put(segment, next);
                }
                parent = map;
                parentSegment = segment;
                current = next;
                continue;
            }
            if (current instanceof List && numeric) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) current;
                int index = Integer.parseInt(segment);
                while (list.size() <= index) {
                    list.add(null);
                }
                if (last) {
                    list.set(index, value);
                    return root;
                }
                Object next = list.get(index);
                if (next == null) {
                    next = numericSegmentAhead(segments, i + 1) ? new java.util.ArrayList<Object>() : new LinkedHashMap<String, Object>();
                    list.set(index, next);
                }
                parent = list;
                parentSegment = segment;
                current = next;
                continue;
            }

            Object replacement = numeric ? new java.util.ArrayList<Object>() : new LinkedHashMap<String, Object>();
            if (parent == null) {
                root = replacement;
            } else if (parent instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapParent = (Map<String, Object>) parent;
                mapParent.put(parentSegment, replacement);
            } else if (parent instanceof List && isNumeric(parentSegment)) {
                @SuppressWarnings("unchecked")
                List<Object> listParent = (List<Object>) parent;
                listParent.set(Integer.parseInt(parentSegment), replacement);
            }
            current = replacement;
            i--;
        }
        return root;
    }

    private boolean numericSegmentAhead(List<String> segments, int index) {
        if (segments == null || index < 0 || index >= segments.size()) {
            return false;
        }
        return isNumeric(segments.get(index));
    }

    private boolean isNumeric(String segment) {
        if (segment == null || segment.isEmpty()) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            if (!Character.isDigit(segment.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private List<String> decodePointerSegments(String pointer) {
        String normalized = normalizePointer(pointer);
        if ("/".equals(normalized)) {
            return Collections.emptyList();
        }
        String[] rawSegments = normalized.substring(1).split("/");
        java.util.ArrayList<String> decoded = new java.util.ArrayList<String>(rawSegments.length);
        for (String raw : rawSegments) {
            decoded.add(raw.replace("~1", "/").replace("~0", "~"));
        }
        return decoded;
    }

    private String normalizePointer(String pointer) {
        if (pointer == null || pointer.trim().isEmpty()) {
            return "/";
        }
        String trimmed = pointer.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}
