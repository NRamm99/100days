import fs from "node:fs/promises";
import path from "node:path";

const sourceIndexPath = path.join("src", "main", "resources", "templates", "index.html");
const docsDirPath = "docs";
const docsIndexPath = path.join(docsDirPath, "index.html");
const docsNoJekyllPath = path.join(docsDirPath, ".nojekyll");
const rootCnamePath = "CNAME";
const docsCnamePath = path.join(docsDirPath, "CNAME");

async function ensureDocsSite() {
    await fs.mkdir(docsDirPath, { recursive: true });

    const indexHtml = await fs.readFile(sourceIndexPath, "utf8");
    await fs.writeFile(docsIndexPath, indexHtml, "utf8");

    await fs.writeFile(docsNoJekyllPath, "\n", "utf8");

    try {
        const cname = await fs.readFile(rootCnamePath, "utf8");
        await fs.writeFile(docsCnamePath, cname, "utf8");
    } catch (error) {
        if (error && error.code !== "ENOENT") {
            throw error;
        }
    }
}

ensureDocsSite().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
