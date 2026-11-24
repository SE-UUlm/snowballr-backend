/**
 * Script to compare the API calls defined in the gRPC API, implemented by the SnowballR Backend and used by the
 * SnowballR Frontend.
 *
 * - Defined calls are the "rpc" declarations in the proto files.
 * - Implemented calls are the overrides in the gRPC server that do not delegate to "super" (the generated base
 *   class, which responds with UNIMPLEMENTED). A missing override still compiles, so it is only caught here.
 * - Used calls are read from the file written by the "get-all-used-calls.js" script in the frontend repository.
 *
 * Writes a Markdown report with a table of all calls and their status.
 *
 * Usage: node scripts/compare-api-calls.js [used-calls-file] [report-file]
 *   used-calls-file  defaults to "all-used-calls.md"
 *   report-file      defaults to "api-calls-report.md"
 */

import fs from "fs";
import path from "path";

const protoDir = "api/grpc/proto";
const serverPath = "src/main/kotlin/se/uulm/snowballr/backend/grpc/SnowballRService.kt";
const usedCallsPath = process.argv[2] ?? "all-used-calls.md";
const reportPath = process.argv[3] ?? "api-calls-report.md";

const rpcPattern = /^\s*rpc\s+(\w+)\s*\(/gm;
const implementedCallsPattern = /override suspend fun (\w+)\(\s*.*?\s*\)\s*(:\s+[\w.]+\s+)*(?:=(?!\s*super\.)|\{)/g;

// Ordered by severity, the report lists the calls in this order
const statuses = [
    { name: "Missing", description: "used by the frontend, but not implemented" },
    { name: "Not in API", description: "used by the frontend, but not defined in the API" },
    { name: "Not implemented", description: "defined in the API, but neither implemented nor used" },
    { name: "Unused", description: "implemented, but not used by the frontend" },
    { name: "OK", description: "implemented and used" },
];

// The Kotlin stubs name methods like the RPC, with a lowercase first letter
const toKotlinName = (rpcName) => rpcName.charAt(0).toLowerCase() + rpcName.slice(1);

const getStatus = ({ defined, implemented, used }) => {
    if (used && !defined) return "Not in API";
    if (used && !implemented) return "Missing";
    if (used) return "OK";
    if (!implemented) return "Not implemented";
    return "Unused";
};

const mark = (value) => (value ? "✓" : "");

try {
    if (!fs.existsSync(usedCallsPath)) {
        throw new Error(
            `"${usedCallsPath}" not found. Run "get-all-used-calls.js" in the frontend repository and copy ` +
                "its output here, or pass the path as the first argument.",
        );
    }

    const definedCalls = new Set(
        fs
            .readdirSync(protoDir)
            .filter((file) => file.endsWith(".proto"))
            .flatMap((file) => [...fs.readFileSync(path.join(protoDir, file), "utf8").matchAll(rpcPattern)])
            .map((m) => toKotlinName(m[1])),
    );
    const implementedCalls = new Set(
        [...fs.readFileSync(serverPath, "utf8").matchAll(implementedCallsPattern)].map((m) => m[1]),
    );
    const usedCalls = new Set(
        fs
            .readFileSync(usedCallsPath, "utf8")
            .split("\n")
            .map((line) => line.trim())
            .filter((line) => line.startsWith("- "))
            .map((line) => line.substring(2).trim()),
    );

    const calls = [...new Set([...definedCalls, ...implementedCalls, ...usedCalls])].map((name) => {
        const call = {
            name,
            defined: definedCalls.has(name),
            implemented: implementedCalls.has(name),
            used: usedCalls.has(name),
        };
        return { ...call, status: getStatus(call) };
    });

    const statusOrder = statuses.map((s) => s.name);
    calls.sort(
        (a, b) => statusOrder.indexOf(a.status) - statusOrder.indexOf(b.status) || a.name.localeCompare(b.name),
    );

    const counts = Object.fromEntries(
        statuses.map((s) => [s.name, calls.filter((c) => c.status === s.name).length]),
    );

    const report = [
        "# API Calls Report",
        "",
        `${calls.length} calls: ${definedCalls.size} defined, ${implementedCalls.size} implemented, ` +
            `${usedCalls.size} used.`,
        "",
        "| Status | Count | Meaning |",
        "|--------|-------|---------|",
        ...statuses.map((s) => `| ${s.name} | ${counts[s.name]} | ${s.description} |`),
        "",
        "| Method | Defined | Implemented | Used | Status |",
        "|--------|:-------:|:-----------:|:----:|--------|",
        ...calls.map(
            (c) => `| ${c.name} | ${mark(c.defined)} | ${mark(c.implemented)} | ${mark(c.used)} | ${c.status} |`,
        ),
        "",
    ].join("\n");

    fs.writeFileSync(reportPath, report, "utf8");

    console.log(`Found ${calls.length} calls:`);
    statuses.forEach((s) => console.log(`  ${s.name}: ${counts[s.name]}`));
    console.log(`Report written to ${reportPath}.`);
} catch (error) {
    console.error(`Error: ${error.message}`);
    process.exit(1);
}
