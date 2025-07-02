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

try {
    // Read the file content
    const fileContent = fs.readFileSync(serverPath, "utf8");

    // Count occurrences of "override suspend fun"
    const allCallsPattern = /override suspend fun/g;
    const allCallsMatches = fileContent.match(allCallsPattern) ?? [];
    const allCalls = allCallsMatches.length;

    // Count occurrences of "mainService."
    const implementedCallsPattern = /mainService\./g;
    const implementedCallsMatches =
        fileContent.match(implementedCallsPattern) ?? [];
    const implementedCalls = implementedCallsMatches.length;

    // Calculate percentage
    const percentage =
        allCalls > 0 ? Math.round((implementedCalls / allCalls) * 100) : 0;

    // Count API calls by category/service
    const categoryPattern = /override suspend fun (\w+)\(/g;
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

    let match;
    let categoryMatches = 0;
    while ((match = categoryPattern.exec(fileContent)) !== null) {
        const methodName = match[1];
        const category = getCategory(methodName);

        categories[category] = (categories[category] ?? 0) + 1;
        categoryMatches++;
    }
    assert(
        categoryMatches === allCalls,
        "Category matches should equal all API calls"
    );
    console.log(
        `Found ${categoryMatches} API calls and all were successfully categorized.`
    );
    console.log("Categories:", categories);

    // Count implemented calls by category
    const implementedPattern = /mainService\.(\w+)\(/g;
    let implementedMatches = 0;
    while ((match = implementedPattern.exec(fileContent)) !== null) {
        const methodName = match[1];
        const category = getCategory(methodName);

        implementedByCategory[category] =
            (implementedByCategory[category] ?? 0) + 1;
        implementedMatches++;
    }
    assert(
        implementedMatches === implementedCalls,
        "Implemented matches should equal implemented API calls"
    );

    // Calculate remaining work
    const remainingCalls = allCalls - implementedCalls;

    // Output the results
    console.log(`All API calls: ${allCalls}`);
    console.log(`Implemented API calls: ${implementedCalls}`);
    console.log(`Percentage implemented: ${percentage}%`);
    console.log(`Remaining API calls: ${remainingCalls}`);
    console.log("\nImplementation by category:");
    for (const category in categories) {
        const implemented = implementedByCategory[category] ?? 0;
        const total = categories[category];
        const catPercentage = Math.round((implemented / total) * 100);
        console.log(
            `  ${category}: ${implemented}/${total} (${catPercentage}%)`
        );
    }

    // Read wiki file
    const wikiContent = fs.readFileSync(wikiPath, "utf8");

    // Create the replacement string with Markdown formatting
    let replacementText = `## API Implementation Progress\n\n`;
    replacementText += `Currently, **${implementedCalls}/${allCalls} (${percentage}%)** of the API calls are implemented.\n\n`;

    // Add progress bar
    const progressBarLength = 20;
    const filledChars = Math.round((percentage / 100) * progressBarLength);
    const progressBar =
        "█".repeat(filledChars) + "░".repeat(progressBarLength - filledChars);
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
        throw new Error(
            `Pattern "${replacePattern}" not found in the wiki file.`
        );
    }
    const updatedWikiContent = wikiContent.replace(
        replacePattern,
        replacementText
    );

    // Write the updated content back to the wiki file
    fs.writeFileSync(wikiPath, updatedWikiContent, "utf8");

    console.log("\nWiki updated with progress statistics!");
} catch (error) {
    console.error(`Error: ${error.message}`);
    process.exit(1);
}
