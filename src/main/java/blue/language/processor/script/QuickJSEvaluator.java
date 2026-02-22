package blue.language.processor.script;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class QuickJSEvaluator implements AutoCloseable {

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

    private final ScriptRuntime runtime;

    public QuickJSEvaluator() {
        this(new QuickJsSidecarRuntime());
    }

    public QuickJSEvaluator(ScriptRuntime runtime) {
        this.runtime = runtime;
    }

    public ScriptRuntimeResult evaluate(String code, Map<String, Object> bindings, BigInteger wasmGasLimit) {
        Map<String, Object> safeBindings = normalizeBindings(bindings);
        validateBindings(safeBindings);
        try {
            return runtime.evaluate(new ScriptRuntimeRequest(withRuntimePrelude(code), safeBindings, wasmGasLimit));
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

    private Map<String, Object> normalizeBindings(Map<String, Object> bindings) {
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
        return normalized;
    }
}
