#!/usr/bin/env node

const readline = require('readline');
const vm = require('vm');

const DEFAULT_TIMEOUT_MS = 1000;
const MIN_TIMEOUT_MS = 10;

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
  return { __result: result, events: emittedEvents };
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
    respond({
      id,
      ok: true,
      result,
      wasmGasUsed: '0',
      wasmGasRemaining: wasmGasLimit,
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
