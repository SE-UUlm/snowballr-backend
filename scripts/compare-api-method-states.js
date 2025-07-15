/**
 * Script to compare API methods between all-available-calls.md and all-used-calls.md
 *
 * Usage: node scripts/compare-api-method-states.js
 */

import fs from "fs";

// File paths
const allAvailableCallsPath = "all-available-calls.md";
const allImplementedCallsPath = "all-implemented-calls.md";
const usedApiMethodsPath = "all-used-calls.md";
const missingCallsPath = "all-missing-calls.md";
const unusedCallsPath = "all-unused-calls.md";

try {
    // Read the files
    const allAvailableCallsContent = fs.readFileSync(
        allAvailableCallsPath,
        "utf8"
    );
    const allImplementedCallsContent = fs.readFileSync(
        allImplementedCallsPath,
        "utf8"
    );
    const usedApiMethodsContent = fs.readFileSync(usedApiMethodsPath, "utf8");

    // Parse bullet point lists
    const parseListItems = (content) => {
        return content
            .trim()
            .split("\n")
            .filter((line) => line.trim().startsWith("- "))
            .map((line) => line.trim().substring(2).trim());
    };

    const allAvailableCalls = parseListItems(allAvailableCallsContent);
    const allImplementedCalls = parseListItems(allImplementedCallsContent);
    const usedApiMethods = parseListItems(usedApiMethodsContent);

    // Find missing calls (used but not implemented)
    const missingCalls = usedApiMethods.filter(
        (method) => !allImplementedCalls.includes(method)
    );

    // Find unused calls (available but not used)
    const unusedCalls = allAvailableCalls.filter(
        (method) => !usedApiMethods.includes(method)
    );

    // Generate output files
    const formatList = (items) =>
        items
            .sort()
            .map((item) => `- ${item}`)
            .join("\n");

    // Write missing calls to file
    const missingCallsContent =
        missingCalls.length > 0
            ? formatList(missingCalls)
            : "# No missing calls found\n\nAll API methods used are available in the API.";
    fs.writeFileSync(missingCallsPath, missingCallsContent, "utf8");

    // Write unused calls to file
    const unusedCallsContent =
        unusedCalls.length > 0
            ? formatList(unusedCalls)
            : "# No unused calls found\n\nAll available API methods are being used.";
    fs.writeFileSync(unusedCallsPath, unusedCallsContent, "utf8");

    console.log(`Comparison completed successfully!`);
    console.log(
        `Found ${missingCalls.length} missing calls and ${unusedCalls.length} unused calls.`
    );
    console.log(
        `Results written to ${missingCallsPath} and ${unusedCallsPath}.`
    );
} catch (error) {
    console.error(`Error: ${error.message}`);
    process.exit(1);
}
