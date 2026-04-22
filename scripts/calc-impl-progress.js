/**
 * Script to calculate the implementation progress of API calls in the SnowballR Backend and update the wiki.
 *
 * Usage: node scripts/calc-impl-progress.js
 */

import assert from "assert";
import fs from "fs";

const serverPath = "src/main/kotlin/se/uulm/snowballr/backend/grpc/SnowballRServer.kt";
const wikiPath = "wiki/Contributing.md";
const replacePattern = "<!-- @add-progress -->";
const allCallsPattern = /override suspend fun (\w+)\(/g;
const implementedCallsPattern = /override suspend fun (\w+)\(\s*.*?\s*\)\s*(:\s+[\w.]+\s+)*=(?!\s*super\.)/g;
// Mode can be "update-wiki" or "write-files" or "all" or "none"
// "all" will perform both actions
const mode = process.argv[2] || "all"; // Default to "all" if not specified
if (!["update-wiki", "write-files", "all", "none"].includes(mode)) {
    console.error(`Invalid mode: ${mode}. Use "update-wiki", "write-files", "all", or "none".`);
    process.exit(1);
}

try {
    // Read the file content
    const fileContent = fs.readFileSync(serverPath, "utf8");

    // Count occurrences of "override suspend fun"
    const allCallsMatches = fileContent.match(allCallsPattern) ?? [];
    const allCallsCount = allCallsMatches.length;

    // Count occurrences of not "super."
    const implementedCallsMatches = fileContent.match(implementedCallsPattern) ?? [];
    const implementedCallsCount = implementedCallsMatches.length;

    // Calculate percentage
    const percentage = allCallsCount > 0 ? Math.round((implementedCallsCount / allCallsCount) * 100) : 0;

    // Count API calls by category/service
    const categories = {};
    const implementedByCategory = {};

    // Determine the category based on the method name
    const getCategory = (methodName) => {
        return methodName.includes("User")
            ? "User"
            : methodName.includes("Project")
            ? "Project"
            : methodName.includes("Paper")
            ? "Paper"
            : methodName.includes("Review") || methodName.includes("Criteri")
            ? "Review"
            : "Other";
    };

    const allCalls = [];
    let match;
    let categoryMatches = 0;
    while ((match = allCallsPattern.exec(fileContent)) !== null) {
        const methodName = match[1];
        allCalls.push(methodName);
        const category = getCategory(methodName);

        categories[category] = (categories[category] ?? 0) + 1;
        categoryMatches++;
    }
    assert(categoryMatches === allCallsCount, "Category matches should equal all API calls");
    console.log(`Found ${categoryMatches} API calls and all were successfully categorized.`);
    console.log("Categories:", categories);

    if (mode === "write-files" || mode === "all") {
        fs.writeFileSync(
            "all-available-calls.md",
            allCalls
                .toSorted()
                .map((m) => `- ${m}`)
                .join("\n"),
            "utf8",
        );
    }

    // Count implemented calls by category
    const implementedCalls = [];
    let implementedMatches = 0;
    while ((match = implementedCallsPattern.exec(fileContent)) !== null) {
        const methodName = match[1];
        implementedCalls.push(methodName);
        const category = getCategory(methodName);

        implementedByCategory[category] = (implementedByCategory[category] ?? 0) + 1;
        implementedMatches++;
    }
    assert(implementedMatches === implementedCallsCount, "Implemented matches should equal implemented API calls");
    if (mode === "write-files" || mode === "all") {
        fs.writeFileSync(
            "all-implemented-calls.md",
            implementedCalls
                .toSorted()
                .map((m) => `- ${m}`)
                .join("\n"),
            "utf8",
        );
    }

    // Calculate remaining work
    const remainingCallsCount = allCallsCount - implementedCallsCount;

    // Output the results
    console.log(`All API calls: ${allCallsCount}`);
    console.log(`Implemented API calls: ${implementedCallsCount}`);
    console.log(`Percentage implemented: ${percentage}%`);
    console.log(`Remaining API calls: ${remainingCallsCount}`);
    console.log("\nImplementation by category:");
    for (const category in categories) {
        const implemented = implementedByCategory[category] ?? 0;
        const total = categories[category];
        const catPercentage = Math.round((implemented / total) * 100);
        console.log(`  ${category}: ${implemented}/${total} (${catPercentage}%)`);
    }

    // Read wiki file
    const wikiContent = fs.readFileSync(wikiPath, "utf8");

    // Create the replacement string with Markdown formatting
    let replacementText = `## API Implementation Progress\n\n`;
    replacementText += `Currently, **${implementedCallsCount}/${allCallsCount} (${percentage}%)** of the API calls are implemented.\n\n`;

    // Add progress bar
    const progressBarLength = 45;
    const filledChars = Math.round((percentage / 100) * progressBarLength);
    const progressBar = "█".repeat(filledChars) + "░".repeat(progressBarLength - filledChars);
    replacementText += `\`${progressBar}\` ${percentage}%\n\n`;

    // Add category breakdown
    replacementText += `### Implementation Progress by Category\n\n`;
    replacementText += `| Category | Implemented | Total | Percentage |\n`;
    replacementText += `|----------|-------------|-------|------------|\n`;

    for (const category in categories) {
        const implemented = implementedByCategory[category] ?? 0;
        const total = categories[category];
        const catPercentage = Math.round((implemented / total) * 100);
        replacementText += `| ${category} | ${implemented} | ${total} | ${catPercentage}% |\n`;
    }

    // Replace the pattern in the wiki content
    if (!wikiContent.includes(replacePattern)) {
        throw new Error(`Pattern "${replacePattern}" not found in the wiki file.`);
    }
    const updatedWikiContent = wikiContent.replace(replacePattern, replacementText);

    // Write the updated content back to the wiki file
    if (mode === "update-wiki" || mode === "all") {
        fs.writeFileSync(wikiPath, updatedWikiContent, "utf8");
        console.log("\nWiki updated with progress statistics!");
    }
} catch (error) {
    console.error(`Error: ${error.message}`);
    process.exit(1);
}
