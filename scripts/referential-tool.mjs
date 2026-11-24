// Outil du référentiel AIMA : valide les fichiers JSON et génère la grille d'audit détaillée.
// Usage : node scripts/referential-tool.mjs            (valider + générer)
//         node scripts/referential-tool.mjs --check    (valider uniquement)
import { readFileSync, readdirSync, writeFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const refDir = join(root, "referential");
const readJson = (path) => JSON.parse(readFileSync(path, "utf8"));
const referential = readJson(join(refDir, "referentiel-v1.json"));
const questionFiles = readdirSync(join(refDir, "questions")).filter((f) => f.endsWith(".json"));
const questionDocs = questionFiles.map((f) => ({ file: f, doc: readJson(join(refDir, "questions", f)) }));
const questions = questionDocs.flatMap(({ doc }) => doc.questions);

const errors = [];

// ---------- 1. Contrat : forme des fichiers (JSON Schema, referential/schema/) ----------
// La forme du JSON est partagée avec les autres modules (labo red-team, gouvernance) :
// un champ inconnu, manquant ou mal typé est refusé ici, avant toute autre vérification.
let Ajv2020;
try {
  ({ default: Ajv2020 } = await import("ajv/dist/2020.js"));
} catch {
  console.error("❌ Dépendance manquante : lancer « npm ci » dans le dossier scripts/");
  process.exit(1);
}
const ajv = new Ajv2020({ allErrors: true });
const validators = new Map();   // un schéma n'est compilé qu'une fois
const checkShape = (schemaFile, data, label) => {
  if (!validators.has(schemaFile)) validators.set(schemaFile, ajv.compile(readJson(join(refDir, "schema", schemaFile))));
  const validate = validators.get(schemaFile);
  if (!validate(data)) {
    for (const e of validate.errors) errors.push(`${label}${e.instancePath || "/"} : ${e.message}`);
  }
};
checkShape("referentiel.schema.json", referential, "referentiel-v1.json");
for (const { file, doc } of questionDocs) checkShape("questions.schema.json", doc, `questions/${file}`);
if (errors.length) {
  console.error(`❌ Le référentiel ne respecte pas le contrat (${errors.length} erreur(s)) :\n - ` + errors.join("\n - "));
  process.exit(1);
}

// ---------- 2. Cohérence (règles que le schéma ne peut pas exprimer) ----------
for (const { file, doc } of questionDocs) {
  if (`${doc.domain}.json` !== file) errors.push(`questions/${file} : le domaine déclaré est ${doc.domain}`);
  for (const q of doc.questions) {
    if (!q.code.startsWith(`${doc.domain}.`)) errors.push(`${q.code} : rangée dans le fichier du domaine ${doc.domain}`);
  }
}
const sourceCodes = new Set(referential.sources.map((s) => s.code));
const subdomains = new Map();
for (const d of referential.domains) {
  for (const sd of d.subdomains) {
    if (subdomains.has(sd.code)) errors.push(`${sd.code} : code de sous-domaine en double`);
    if (!sd.code.startsWith(`${d.code}.`)) errors.push(`${sd.code} : rangé dans le domaine ${d.code}`);
    for (const src of Object.keys(sd.references)) {
      if (!sourceCodes.has(src)) errors.push(`${sd.code} : référence à une source non déclarée (${src})`);
    }
    subdomains.set(sd.code, { ...sd, domain: d.code });
  }
}
const domainCodes = referential.domains.map((d) => d.code);
if (new Set(domainCodes).size !== domainCodes.length) errors.push("Code de domaine en double");
for (const d of domainCodes) {
  if (!questionDocs.some(({ doc }) => doc.domain === d)) errors.push(`${d} : aucun fichier questions/${d}.json`);
}

const weightSum = referential.domains.reduce((s, d) => s + d.weight, 0);
if (Math.abs(weightSum - 1) > 1e-9) errors.push(`Somme des poids des domaines = ${weightSum} (attendu 1)`);

const seen = new Set();
for (const q of questions) {
  if (seen.has(q.code)) errors.push(`${q.code} : code en double`);
  seen.add(q.code);
  if (!subdomains.has(q.subdomain)) errors.push(`${q.code} : sous-domaine inconnu ${q.subdomain}`);
  if (!q.code.startsWith(q.subdomain + ".")) errors.push(`${q.code} : code incohérent avec ${q.subdomain}`);
  if (![1, 2, 3].includes(q.criticality)) errors.push(`${q.code} : criticité invalide`);
  if (!["LOW", "MEDIUM", "HIGH"].includes(q.effort)) errors.push(`${q.code} : effort invalide`);
  if (q.levels?.length !== 5) errors.push(`${q.code} : il faut 5 niveaux`);
  if (q.recommendations?.length !== 4) errors.push(`${q.code} : il faut 4 recommandations`);
  if (!q.text?.trim().endsWith("?")) errors.push(`${q.code} : la question doit se terminer par « ? »`);
  if (!q.evidence) errors.push(`${q.code} : preuves attendues manquantes`);
}
for (const code of subdomains.keys()) {
  const n = questions.filter((q) => q.subdomain === code).length;
  if (n < 2) errors.push(`${code} : seulement ${n} question(s), minimum 2`);
}

if (errors.length) {
  console.error(`❌ ${errors.length} erreur(s) :\n - ` + errors.join("\n - "));
  process.exit(1);
}
const crit = [1, 2, 3].map((c) => questions.filter((q) => q.criticality === c).length);
console.log(
  `✅ Référentiel valide : ${referential.domains.length} domaines, ${subdomains.size} sous-domaines, ` +
    `${questions.length} questions (criticité 1/2/3 : ${crit.join("/")})`
);
if (process.argv.includes("--check")) process.exit(0);

// ---------- Génération de la grille d'audit ----------
const CRIT = { 1: "Normale", 2: "Importante", 3: "**Critique**" };
const EFFORT = { LOW: "Faible", MEDIUM: "Moyen", HIGH: "Élevé" };
const levelNames = referential.maturityLevels.map((l) => l.name);
const esc = (s) => s.replace(/\|/g, "\\|");

let md = `# 03b — Grille d'audit détaillée AIMA v${referential.version}\n\n`;
md += `> Fichier **généré automatiquement** par \`scripts/referential-tool.mjs\` à partir de \`referential/\`. Ne pas modifier à la main.\n\n`;
md += `**${questions.length} questions** · ${subdomains.size} sous-domaines · ${referential.domains.length} domaines\n\n`;
md += `## Sommaire\n\n`;
for (const d of referential.domains) md += `- [D${d.order} — ${d.name}](#d${d.order}) (poids ${Math.round(d.weight * 100)} %)\n`;

for (const d of referential.domains) {
  md += `\n---\n\n<a id="d${d.order}"></a>\n## D${d.order} — ${d.name} (${d.code})\n\n${d.description}\n`;
  for (const sd of d.subdomains) {
    md += `\n### ${sd.code} — ${sd.name}\n\n${sd.description}\n\n`;
    const refs = Object.entries(sd.references).map(([k, v]) => `${k} ${v.join(", ")}`).join(" · ");
    md += `*Références : ${refs}*\n`;
    for (const q of questions.filter((x) => x.subdomain === sd.code)) {
      md += `\n#### ${q.code} — ${q.text}\n\n`;
      md += `Criticité : ${CRIT[q.criticality]} · Effort de mise en œuvre : ${EFFORT[q.effort]}  \n`;
      md += `Preuves attendues : ${q.evidence}\n\n`;
      md += `| Niveau | Situation constatée | Recommandation pour passer au niveau suivant |\n|---|---|---|\n`;
      q.levels.forEach((l, i) => {
        md += `| ${i + 1} — ${levelNames[i]} | ${esc(l)} | ${i < 4 ? esc(q.recommendations[i]) : "—"} |\n`;
      });
    }
  }
}
writeFileSync(join(root, "docs", "03b-grille-audit-detaillee.md"), md, "utf8");
console.log("📄 docs/03b-grille-audit-detaillee.md généré");
