#!/usr/bin/env node

const readline = require('readline');
const vm = require('vm');

function evaluateCode(code, bindings) {
  const sandbox = Object.assign({}, bindings || {});
  const context = vm.createContext(sandbox);
  try {
    return vm.runInContext(code, context, { timeout: 1000 });
  } catch (firstError) {
    const wrapped = `(function(){${code}\n})()`;
    return vm.runInContext(wrapped, context, { timeout: 1000 });
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
    const result = evaluateCode(code, bindings);
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
      },
    });
  }
});
