/*
Builds the NPM package: generates ES modules (`.js`) with type declarations (`.d.ts`) from the proto files.

protobuf-ts emits extensionless relative imports (e.g. `from "./main"`). Bundlers accept them, but Node's
ESM loader and TypeScript's `nodenext` resolution do not, so `.js` is appended to every relative import.
 */
import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const DIST = "dist";
const PROTO_DIR = "proto/";
const PLUGIN_OPTIONS = ["generate_dependencies", "output_javascript"];
const RELATIVE_IMPORT = /(from\s+["'])(\.{1,2}\/[^"']+?)(["'])/g;

function cleanDist() {
    rmSync(DIST, { recursive: true, force: true });
    mkdirSync(DIST);
    writeFileSync(join(DIST, ".gitkeep"), "");
}

function generate() {
    execFileSync(
            join("node_modules", ".bin", "protoc"),
            [
                `--ts_out=${DIST}`,
                `--ts_opt=${PLUGIN_OPTIONS.join(",")}`,
                `--proto_path=${PROTO_DIR}`,
                join(PROTO_DIR, "main.proto"),
            ],
            { stdio: "inherit", shell: process.platform === "win32" },
    );
}

function addImportExtensions(dir) {
    for (const entry of readdirSync(dir, { withFileTypes: true })) {
        const path = join(dir, entry.name);
        if (entry.isDirectory()) {
            addImportExtensions(path);
        } else if (entry.name.endsWith(".js") || entry.name.endsWith(".d.ts")) {
            const source = readFileSync(path, "utf8");
            const fixed = source.replace(RELATIVE_IMPORT, (match, start, specifier, end) =>
                    specifier.endsWith(".js") ? match : `${start}${specifier}.js${end}`,
            );
            writeFileSync(path, fixed);
        }
    }
}

cleanDist();
generate();
addImportExtensions(DIST);
