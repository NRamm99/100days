import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";

const username = process.env.GITHUB_USERNAME ?? "NRamm99";
const outputPath = process.env.CONTRIBUTIONS_OUTPUT ?? path.join("docs", "contributions.json");
const contributionsUrl = `https://github.com/users/${username}/contributions`;

function decodeHtml(text) {
    return text
        .replace(/&quot;/g, "\"")
        .replace(/&#39;/g, "'")
        .replace(/&amp;/g, "&")
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">");
}

function extractTotal(html) {
    const match = html.match(/<h2[^>]*>\s*([\d,]+)\s+contributions\s+in the last year\s*<\/h2>/i);
    if (!match) {
        throw new Error("Could not find total contributions in GitHub response.");
    }

    return Number.parseInt(match[1].replace(/,/g, ""), 10);
}

function extractCountsById(html) {
    const countsById = new Map();
    const tooltipPattern = /<tool-tip[^>]*for="([^"]+)"[^>]*>([\s\S]*?)<\/tool-tip>/gi;

    for (const match of html.matchAll(tooltipPattern)) {
        const tooltipTarget = match[1];
        const tooltipText = decodeHtml(match[2].replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim());
        const countMatch = tooltipText.match(/(\d+)\s+contribution/i);
        const count = countMatch ? Number.parseInt(countMatch[1], 10) : 0;
        countsById.set(tooltipTarget, count);
    }

    return countsById;
}

function extractContributionDays(html) {
    const countsById = extractCountsById(html);
    const contributions = [];
    const dayPattern = /<td[^>]*data-date="([^"]+)"[^>]*id="([^"]+)"[^>]*data-level="(\d+)"[^>]*class="ContributionCalendar-day"/gi;

    for (const match of html.matchAll(dayPattern)) {
        const date = match[1];
        const id = match[2];
        const level = Number.parseInt(match[3], 10);

        contributions.push({
            date,
            count: countsById.get(id) ?? 0,
            level
        });
    }

    contributions.sort((left, right) => left.date.localeCompare(right.date));
    return contributions;
}

async function fetchHtml(url) {
    const response = await fetch(url, {
        headers: {
            "User-Agent": "100days-pages-generator",
            "Accept": "text/html"
        }
    });

    if (!response.ok) {
        throw new Error(`GitHub returned ${response.status} for ${url}`);
    }

    return response.text();
}

async function main() {
    const html = await fetchHtml(contributionsUrl);
    const payload = {
        total: {
            lastYear: extractTotal(html)
        },
        contributions: extractContributionDays(html),
        generatedAt: new Date().toISOString(),
        source: contributionsUrl
    };

    await fs.mkdir(path.dirname(outputPath), { recursive: true });
    await fs.writeFile(outputPath, JSON.stringify(payload, null, 2) + "\n", "utf8");
}

main().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
