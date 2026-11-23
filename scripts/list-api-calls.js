/**
 * Script to list the API calls defined in the gRPC API and the ones implemented by the SnowballR Backend.
 *
 * Available calls are the "rpc" declarations in the proto files. Implemented calls are the overrides in the
 * gRPC server that do not delegate to "super" (the generated base class, which responds with UNIMPLEMENTED).
 * Since a missing override still compiles, calls that are available but not implemented are reported.
 *
 * Writes "all-available-calls.md" and "all-implemented-calls.md", which are used by
 * "compare-api-method-states.js".
 *
 * Usage: node scripts/list-api-calls.js
 */

import fs from "fs";
import path from "path";

const protoDir = "api/grpc/proto";
const serverPath = "src/main/kotlin/se/uulm/snowballr/backend/grpc/SnowballRService.kt";
const rpcPattern = /^\s*rpc\s+(\w+)\s*\(/gm;
const implementedCallsPattern = /override suspend fun (\w+)\(\s*.*?\s*\)\s*(:\s+[\w.]+\s+)*(?:=(?!\s*super\.)|\{)/g;

// The Kotlin stubs name methods like the RPC, with a lowercase first letter
const toKotlinName = (rpcName) => rpcName.charAt(0).toLowerCase() + rpcName.slice(1);

const writeList = (filePath, items) =>
    fs.writeFileSync(
        filePath,
        items
            .toSorted()
            .map((m) => `- ${m}`)
            .join("\n"),
        "utf8",
    );

try {
    const availableCalls = fs
        .readdirSync(protoDir)
        .filter((file) => file.endsWith(".proto"))
        .flatMap((file) => [...fs.readFileSync(path.join(protoDir, file), "utf8").matchAll(rpcPattern)])
        .map((m) => toKotlinName(m[1]));

    const serverContent = fs.readFileSync(serverPath, "utf8");
    const implementedCalls = [...serverContent.matchAll(implementedCallsPattern)].map((m) => m[1]);

    writeList("all-available-calls.md", availableCalls);
    writeList("all-implemented-calls.md", implementedCalls);

    console.log(`Found ${availableCalls.length} API calls, ${implementedCalls.length} of them implemented.`);

    const notImplemented = availableCalls.filter((call) => !implementedCalls.includes(call));
    if (notImplemented.length > 0) {
        console.log(`Not implemented (${notImplemented.length}):`);
        notImplemented.toSorted().forEach((call) => console.log(`  - ${call}`));
    }

    console.log("Results written to all-available-calls.md and all-implemented-calls.md.");
} catch (error) {
    console.error(`Error: ${error.message}`);
    process.exit(1);
}
