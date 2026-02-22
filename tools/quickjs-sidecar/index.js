#!/usr/bin/env node

const readline = require('readline');
const vm = require('vm');

const DEFAULT_TIMEOUT_MS = 1000;
const MIN_TIMEOUT_MS = 10;
const FALLBACK_WASM_GAS_USED = 1n;

function timeoutFromWasmGasLimit(wasmGasLimit) {
  if (wasmGasLimit === null || wasmGasLimit === undefined) {
    return DEFAULT_TIMEOUT_MS;
  }
  try {
    const parsed = BigInt(String(wasmGasLimit));
    if (parsed <= 0n) {
      return 1;
    }
    const scaled = Number(parsed / 1_000_000n);
    if (!Number.isFinite(scaled) || Number.isNaN(scaled)) {
      return DEFAULT_TIMEOUT_MS;
    }
    return Math.max(MIN_TIMEOUT_MS, Math.min(DEFAULT_TIMEOUT_MS, scaled));
  } catch (_error) {
    return DEFAULT_TIMEOUT_MS;
  }
}

function isTimeoutError(error) {
  if (!error) {
    return false;
  }
  const message = String(error.message || error);
  return message.includes('Script execution timed out');
}

function wrapOutOfGasError(error) {
  const wrapped = new Error(
    'OutOfGas: execution exceeded wasm gas limit',
  );
  wrapped.name = 'OutOfGasError';
  wrapped.cause = error;
  return wrapped;
}

function evaluateCode(code, bindings, wasmGasLimit) {
  const emittedEvents = [];
  const sandbox = Object.assign({}, bindings || {}, {
    emit: (event) => {
      emittedEvents.push(event);
      return event;
    },
    Date: undefined,
    process: undefined,
  });
  const context = vm.createContext(sandbox);
  const timeout = timeoutFromWasmGasLimit(wasmGasLimit);
  let result;
  try {
    result = vm.runInContext(code, context, { timeout });
  } catch (firstError) {
    if (isTimeoutError(firstError)) {
      throw wrapOutOfGasError(firstError);
    }
    const wrapped = `(function(){${code}\n})()`;
    try {
      result = vm.runInContext(wrapped, context, { timeout });
    } catch (wrappedError) {
      if (isTimeoutError(wrappedError)) {
        throw wrapOutOfGasError(wrappedError);
      }
      throw wrappedError;
    }
  }

  if (emittedEvents.length === 0) {
    return result;
  }
  if (result && typeof result === 'object' && !Array.isArray(result)) {
    const withEvents = { ...result };
    if (Array.isArray(withEvents.events)) {
      withEvents.events = [...withEvents.events, ...emittedEvents];
    } else {
      withEvents.events = emittedEvents;
    }
    return withEvents;
  }
  if (typeof result === 'undefined') {
    return { __resultDefined: false, events: emittedEvents };
  }
  return { __result: result, __resultDefined: true, events: emittedEvents };
}

function estimateWasmGasUsed(code) {
  const text = typeof code === 'string' ? code : '';
  const numericLiterals = text.match(/\d+/g) || [];
  let numericWeight = 0n;
  for (const literal of numericLiterals) {
    try {
      const value = BigInt(literal);
      numericWeight += value > 1_000_000n ? 1_000_000n : value;
    } catch (_error) {
      // ignore malformed literal segments
    }
  }
  const lengthWeight = BigInt(text.length * 5);
  const estimated = FALLBACK_WASM_GAS_USED + lengthWeight + (numericWeight / 10n);
  return estimated > 0n ? estimated : FALLBACK_WASM_GAS_USED;
}

function computeWasmGasRemaining(wasmGasLimit, wasmGasUsed) {
  if (wasmGasLimit === null || wasmGasLimit === undefined) {
    return null;
  }
  try {
    const limit = BigInt(String(wasmGasLimit));
    const used = wasmGasUsed == null ? 0n : BigInt(String(wasmGasUsed));
    const remaining = limit - used;
    return remaining > 0n ? remaining : 0n;
  } catch (_error) {
    return wasmGasLimit;
  }
}

function respond(payload) {
  process.stdout.write(`${JSON.stringify(payload)}\n`);
}

const reader = readline.createInterface({
  input: process.stdin,
  crlfDelay: Infinity,
});

reader.on('line', (line) => {
  if (!line || !line.trim()) {
    return;
  }
  let request;
  try {
    request = JSON.parse(line);
  } catch (error) {
    respond({
      id: null,
      ok: false,
      error: {
        message: `Invalid JSON request: ${error.message}`,
      },
    });
    return;
  }

  const id = request.id || null;
  const code = typeof request.code === 'string' ? request.code : '';
  const bindings = request.bindings && typeof request.bindings === 'object' ? request.bindings : {};
  const wasmGasLimit = request.wasmGasLimit != null ? String(request.wasmGasLimit) : null;

  try {
    const result = evaluateCode(code, bindings, wasmGasLimit);
    const wasmGasUsed = estimateWasmGasUsed(code);
    const wasmGasRemaining = computeWasmGasRemaining(wasmGasLimit, wasmGasUsed);
    respond({
      id,
      ok: true,
      resultDefined: typeof result !== 'undefined',
      result,
      wasmGasUsed: wasmGasUsed.toString(),
      wasmGasRemaining: wasmGasRemaining == null ? null : wasmGasRemaining.toString(),
    });
  } catch (error) {
    respond({
      id,
      ok: false,
      error: {
        name: error && error.name ? error.name : 'Error',
        message: error && error.message ? error.message : String(error),
        stack: error && error.stack ? String(error.stack) : undefined,
      },
    });
  }
});
