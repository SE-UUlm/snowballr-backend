/**
 * Script to replace GitHub URLs pointing to the "develop" branch with a specified branch name
 * in all wiki files and the README.md file.
 *
 * Usage: node wiki-replace-github-urls.js <branch-name>
 */

import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";

// Get current directory
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, "..");

// Parse command line arguments
const args = process.argv.slice(2);
if (args.length !== 1) {
    console.error("Error: Please provide a branch name as argument");
    console.error("Usage: node wiki-replace-github-urls.js <branch-name>");
    process.exit(1);
}

const targetBranch = args[0];
console.log(`Replacing GitHub URLs from "develop" to "${targetBranch}"...`);

// GitHub URL regex pattern - matches URLs pointing to the develop branch
const githubUrlRegex = /https:\/\/github\.com\/SE-UUlm\/snowballr-backend\/blob\/develop\//g;

/**
 * Recursively get all files in a directory.
 *
 * @param {string} dirPath - Directory path
 * @param {Array} fileList - List to append files to
 * @returns {Promise<Array>} - List of file paths
 */
async function getAllFiles(dirPath, fileList = []) {
    const items = await fs.readdir(dirPath, { withFileTypes: true });

    for (const item of items) {
        const fullPath = path.join(dirPath, item.name);

        if (item.isDirectory()) {
            // Recursively process subdirectories
            await getAllFiles(fullPath, fileList);
        } else if (
            item.isFile() &&
            (item.name.endsWith(".md") ||
                item.name.endsWith(".mdx") ||
                item.name.endsWith(".markdown"))
        ) {
            // Only include markdown files
            fileList.push(fullPath);
        }
    }

    return fileList;
}

/**
 * Replace GitHub URLs in a file.
 *
 * @param {string} filePath - Path to the file
 * @returns {Promise<boolean>} - True if file was updated, false otherwise
 */
async function processFile(filePath) {
    try {
        // Read file content
        const content = await fs.readFile(filePath, "utf-8");

        // Replace GitHub URLs
        const updatedContent = content.replace(
            githubUrlRegex,
            `https://github.com/SE-UUlm/snowballr-backend/blob/${targetBranch}/`,
        );

        // If content was updated, write it back to file
        if (content !== updatedContent) {
            await fs.writeFile(filePath, updatedContent, "utf-8");
            console.log(`Updated: ${path.relative(rootDir, filePath)}`);
            return true;
        }

        return false;
    } catch (error) {
        console.error(`Error processing file ${filePath}:`, error.message);
        return false;
    }
}

/**
 * Main function
 */
async function main() {
    try {
        // Get paths to process
        const wikiPath = path.join(rootDir, "wiki");
        const readmePath = path.join(rootDir, "README.md");

        const wikiFiles = await getAllFiles(wikiPath);
        const filesToProcess = [...wikiFiles, readmePath];

        // Process all files
        let updatedCount = 0;
        for (const file of filesToProcess) {
            const wasUpdated = await processFile(file);
            if (wasUpdated) updatedCount++;
        }

        // Log results
        console.log(
            `\nProcessed ${filesToProcess.length} file(s), updated ${updatedCount} file(s).`,
        );

        if (updatedCount > 0) {
            console.log("GitHub URLs have been successfully replaced.");
        } else {
            console.log("No GitHub URLs were found to replace.");
        }
    } catch (error) {
        console.error("Error:", error.message);
        process.exit(1);
    }
}

// Run the script
main();
