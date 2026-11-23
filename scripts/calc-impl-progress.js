/**
 * Script to list the API calls implemented by the SnowballR Backend gRPC server.
 *
 * Writes "all-available-calls.md" and "all-implemented-calls.md", which are used by
 * "compare-api-method-states.js".
 *
 * Usage: node scripts/calc-impl-progress.js
 */

import fs from "fs";

const serverPath = "src/main/kotlin/se/uulm/snowballr/backend/grpc/SnowballRService.kt";
const allCallsPattern = /override suspend fun (\w+)\(/g;
const implementedCallsPattern = /override suspend fun (\w+)\(\s*.*?\s*\)\s*(:\s+[\w.]+\s+)*(?:=(?!\s*super\.)|\{)/g;

const writeList = (path, items) =>
    fs.writeFileSync(
        path,
        items
            .toSorted()
            .map((m) => `- ${m}`)
            .join("\n"),
        "utf8",
    );

try {
    const fileContent = fs.readFileSync(serverPath, "utf8");

    const allCalls = [...fileContent.matchAll(allCallsPattern)].map((m) => m[1]);
    const implementedCalls = [...fileContent.matchAll(implementedCallsPattern)].map((m) => m[1]);

    writeList("all-available-calls.md", allCalls);
    writeList("all-implemented-calls.md", implementedCalls);

    console.log(`Found ${allCalls.length} API calls, ${implementedCalls.length} of them implemented.`);
    console.log("Results written to all-available-calls.md and all-implemented-calls.md.");
} catch (error) {
    console.error(`Error: ${error.message}`);
    process.exit(1);
}
