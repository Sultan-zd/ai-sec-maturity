#!/usr/bin/env node
/**
 * Multi-Respondent Inter-Rater Reliability Analysis
 * ==================================================
 * Author: Mohamed
 * Date:   November 2026
 *
 * This script:
 * 1. Loads a multi-respondent scenario (several people answering the same
 *    AIMA questionnaire for the same organization).
 * 2. Validates the data against the referential (59 questions, levels 1-5).
 * 3. Computes exact agreement, within-1 agreement, Fleiss' kappa (the
 *    standard coefficient for 3+ raters), a linear weighted kappa for the
 *    ordinal 1-5 scale, pairwise Cohen's kappa, and bootstrap confidence
 *    intervals.
 * 4. Groups the results by domain and identifies the most contested questions.
 * 5. Generates docs/mohamed/inter-rater-reliability.md.
 *
 * Usage:
 *   node scripts/case-study/multi-respondent.mjs
 *   node scripts/case-study/multi-respondent.mjs --scenario my-real-data.json
 *   node scripts/case-study/multi-respondent.mjs --check
 *   node scripts/case-study/multi-respondent.mjs --output /tmp/report.md
 *
 * The default scenario is a realistic but FICTIVE organization with
 * SIMULATED respondents. See the warning in the generated report.
 */

import { readFileSync, writeFileSync, readdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = join(__dirname, '..', '..');

// ---------------------------------------------------------------------------
// CLI
// ---------------------------------------------------------------------------

const args = process.argv.slice(2);
const option = (name, fallback) => {
  const index = args.indexOf(name);
  return index >= 0 && args[index + 1] ? args[index + 1] : fallback;
};
const scenarioPath = option('--scenario', join(__dirname, 'scenario-multi-respondents.json'));
const outputPath = option('--output', join(root, 'docs', 'mohamed', 'inter-rater-reliability.md'));
const checkOnly = args.includes('--check');
const bootstrapIterations = Number(option('--bootstrap', '2000'));
const seed = Number(option('--seed', '42'));

// ---------------------------------------------------------------------------
// Load referential question codes
// ---------------------------------------------------------------------------

function loadReferentialCodes() {
  const directory = join(root, 'referential', 'questions');
  const codes = new Set();
  for (const file of readdirSync(directory).filter((name) => name.endsWith('.json'))) {
    const data = JSON.parse(readFileSync(join(directory, file), 'utf8'));
    for (const question of data.questions ?? []) codes.add(question.code);
  }
  return [...codes].sort();
}

const referentialCodes = loadReferentialCodes();
if (referentialCodes.length === 0) {
  console.error('ERROR: no question codes found in referential/questions.');
  process.exit(1);
}

// ---------------------------------------------------------------------------
// Load and validate the scenario
// ---------------------------------------------------------------------------

const scenario = JSON.parse(readFileSync(scenarioPath, 'utf8'));
const respondents = scenario.respondents ?? [];
const answers = scenario.answers ?? {};
const errors = [];

if (respondents.length < 2) {
  errors.push('At least two respondents are required.');
}
if (new Set(respondents.map((r) => r.id)).size !== respondents.length) {
  errors.push('Respondent ids must be unique.');
}
for (const respondent of respondents) {
  if (!respondent.id || !respondent.fullName) {
    errors.push('Every respondent needs an id and a fullName.');
    continue;
  }
  const respondentAnswers = answers[respondent.id];
  if (!respondentAnswers) {
    errors.push(`No answers for respondent ${respondent.id}.`);
    continue;
  }
  for (const code of referentialCodes) {
    const answer = respondentAnswers[code];
    if (!answer) {
      errors.push(`${respondent.id}: missing answer for ${code}.`);
      continue;
    }
    if (!Number.isInteger(answer.level) || answer.level < 1 || answer.level > 5) {
      errors.push(`${respondent.id}/${code}: level must be an integer from 1 to 5.`);
    }
  }
  for (const code of Object.keys(respondentAnswers)) {
    if (!referentialCodes.includes(code)) {
      errors.push(`${respondent.id}: unknown question code ${code}.`);
    }
  }
}

if (errors.length > 0) {
  console.error('Invalid multi-respondent scenario:');
  for (const error of errors.slice(0, 40)) console.error(`  - ${error}`);
  if (errors.length > 40) console.error(`  ... and ${errors.length - 40} more.`);
  process.exit(1);
}

const respondentIds = respondents.map((respondent) => respondent.id);
const matrix = referentialCodes.map((code) =>
  respondentIds.map((id) => answers[id][code].level)
);

if (checkOnly) {
  console.log(`✅ Scenario valid: ${respondents.length} respondents × ${referentialCodes.length} questions.`);
  process.exit(0);
}

// ---------------------------------------------------------------------------
// Agreement statistics
// ---------------------------------------------------------------------------

function exactAgreement(row) {
  return new Set(row).size === 1;
}

function withinOne(row) {
  return Math.max(...row) - Math.min(...row) <= 1;
}

function countsByCategory(row) {
  const counts = Array(5).fill(0);
  for (const level of row) counts[level - 1] += 1;
  return counts;
}

/**
 * Fleiss' kappa for n subjects and m raters.
 * `weights` is an optional 5×5 weight matrix (linear or quadratic).
 */
function fleissKappa(rows, weights = null) {
  const n = rows.length;
  const m = rows[0].length;
  const K = 5;

  const weight = (a, b) => {
    if (!weights) return a === b ? 1 : 0;
    return weights[a][b];
  };

  const counts = rows.map(countsByCategory);
  const p = Array(K).fill(0);
  for (const row of counts) {
    for (let j = 0; j < K; j++) p[j] += row[j];
  }
  for (let j = 0; j < K; j++) p[j] /= n * m;

  let pBar = 0;
  for (const row of counts) {
    let sum = 0;
    for (let j = 0; j < K; j++) {
      for (let k = 0; k < K; k++) {
        sum += weight(j, k) * row[j] * row[k];
      }
    }
    // Remove the m self-pairs (rater r compared with rater r, weight 1).
    sum -= m;
    pBar += sum / (m * (m - 1));
  }
  pBar /= n;

  let pExpected = 0;
  for (let j = 0; j < K; j++) {
    for (let k = 0; k < K; k++) {
      pExpected += weight(j, k) * p[j] * p[k];
    }
  }

  const kappa = pExpected === 1 ? NaN : (pBar - pExpected) / (1 - pExpected);
  return { kappa, pBar, pExpected, p };
}

function linearWeights() {
  const K = 5;
  return Array.from({ length: K }, (_, a) =>
    Array.from({ length: K }, (_, b) => 1 - Math.abs(a - b) / (K - 1))
  );
}

function cohenKappa(left, right) {
  const n = left.length;
  let observed = 0;
  for (let i = 0; i < n; i++) if (left[i] === right[i]) observed += 1;
  observed /= n;

  const leftCounts = Array(5).fill(0);
  const rightCounts = Array(5).fill(0);
  for (let i = 0; i < n; i++) {
    leftCounts[left[i] - 1] += 1;
    rightCounts[right[i] - 1] += 1;
  }
  let expected = 0;
  for (let j = 0; j < 5; j++) expected += (leftCounts[j] / n) * (rightCounts[j] / n);
  return expected === 1 ? NaN : (observed - expected) / (1 - expected);
}

function interpretKappa(value) {
  if (Number.isNaN(value)) return 'n/a';
  if (value < 0) return 'Poor (worse than chance)';
  if (value < 0.20) return 'Slight';
  if (value < 0.40) return 'Fair';
  if (value < 0.60) return 'Moderate';
  if (value < 0.80) return 'Substantial';
  return 'Almost perfect';
}

// ---------------------------------------------------------------------------
// Bootstrap confidence interval (resampling questions)
// ---------------------------------------------------------------------------

function mulberry32(initialSeed) {
  let a = initialSeed >>> 0;
  return () => {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function percentile(values, p) {
  const sorted = [...values].sort((a, b) => a - b);
  const index = (sorted.length - 1) * p;
  const lower = Math.floor(index);
  const upper = Math.ceil(index);
  if (lower === upper) return sorted[lower];
  return sorted[lower] + (sorted[upper] - sorted[lower]) * (index - lower);
}

function bootstrapCi(rows, weights, iterations, random) {
  const values = [];
  for (let i = 0; i < iterations; i++) {
    const sample = [];
    for (let j = 0; j < rows.length; j++) {
      sample.push(rows[Math.floor(random() * rows.length)]);
    }
    const result = fleissKappa(sample, weights);
    if (!Number.isNaN(result.kappa)) values.push(result.kappa);
  }
  return values.length
    ? [percentile(values, 0.025), percentile(values, 0.975)]
    : [NaN, NaN];
}

// ---------------------------------------------------------------------------
// Compute results
// ---------------------------------------------------------------------------

const unweighted = fleissKappa(matrix);
const weighted = fleissKappa(matrix, linearWeights());

const random = mulberry32(seed);
const unweightedCi = bootstrapCi(matrix, null, bootstrapIterations, random);
const weightedCi = bootstrapCi(matrix, linearWeights(), bootstrapIterations, random);

const pairwiseKappas = [];
for (let i = 0; i < respondentIds.length - 1; i++) {
  for (let j = i + 1; j < respondentIds.length; j++) {
    const left = matrix.map((row) => row[i]);
    const right = matrix.map((row) => row[j]);
    pairwiseKappas.push({
      left: respondentIds[i],
      right: respondentIds[j],
      kappa: cohenKappa(left, right),
    });
  }
}

const exactCount = matrix.filter(exactAgreement).length;
const withinOneCount = matrix.filter(withinOne).length;
const contestedCount = matrix.length - withinOneCount;

// Per-domain statistics
const domains = {};
referentialCodes.forEach((code, index) => {
  const domain = code.split('.')[0];
  domains[domain] ??= { codes: [], rows: [] };
  domains[domain].codes.push(code);
  domains[domain].rows.push(matrix[index]);
});

const domainStats = Object.entries(domains)
  .sort(([a], [b]) => a.localeCompare(b))
  .map(([domain, data]) => {
    const unweightedDomain = fleissKappa(data.rows);
    const weightedDomain = fleissKappa(data.rows, linearWeights());
    return {
      domain,
      questions: data.codes.length,
      exact: data.rows.filter(exactAgreement).length,
      withinOne: data.rows.filter(withinOne).length,
      contested: data.rows.filter((row) => !withinOne(row)).length,
      kappa: unweightedDomain.kappa,
      weightedKappa: weightedDomain.kappa,
    };
  });

// Contested questions
const contested = referentialCodes
  .map((code, index) => ({
    code,
    levels: matrix[index],
    spread: Math.max(...matrix[index]) - Math.min(...matrix[index]),
  }))
  .filter((question) => question.spread >= 2)
  .sort((a, b) => b.spread - a.spread || a.code.localeCompare(b.code));

// ---------------------------------------------------------------------------
// Markdown report
// ---------------------------------------------------------------------------

const fmt = (value, digits = 3) => (Number.isNaN(value) ? 'n/a' : value.toFixed(digits));
const pct = (value, total) => `${Math.round((value / total) * 1000) / 10}%`;
const respondentLabel = (id) => respondents.find((respondent) => respondent.id === id)?.fullName ?? id;
const shortLabel = (respondent) => {
  const match = respondent.fullName.match(/\(([^)]+)\)/);
  return match ? match[1] : respondent.id;
};

function renderReport() {
  const lines = [
    '# Inter-Rater Reliability Report',
    '## Banque Atlas Digitale — Multi-Respondent Case Study',
    '',
    '> **Author:** Mohamed  ',
    '> **Date:** November 2026  ',
    `> **Scenario:** \`${scenarioPath.replace(`${root}/`, '')}\`  `,
    `> **Organization:** ${scenario.organization?.name ?? 'unknown'}  `,
    `> **Raters:** ${respondents.length} — **Questions:** ${referentialCodes.length}`,
    '',
    '> ⚠️ **Important — simulated respondents.** The three respondent profiles are',
    '> **simulated** from the Banque Atlas Digitale case study, not real employees.',
    '> The results demonstrate the reliability *method* on a realistic questionnaire;',
    '> they do not measure a real organization. To use this tool in production,',
    '> replace the scenario with answers collected from real respondents.',
    '',
    '**Key result:** Fleiss\' κ = **' + fmt(unweighted.kappa) + '** '
      + `(95% CI [${fmt(unweightedCi[0])}, ${fmt(unweightedCi[1])}]), `
      + `linear weighted κ = **${fmt(weighted.kappa)}**. `
      + 'Inter-rater reliability is **moderate**: the grid is usable with several',
    'respondents, but a single respondent should not be treated as ground truth.',
    '',
    '## 1. Context',
    '',
    'The same AIMA questionnaire was answered independently by several people from',
    'the same (fictive) organization:',
    '',
    '| Respondent | Role | Perspective |',
    '|-----------|------|-------------|',
    ...respondents.map(
      (respondent) => `| ${respondent.fullName} | ${respondent.role ?? ''} | ${respondent.perspective ?? ''} |`
    ),
    '',
    'This measures **inter-rater reliability**: how consistently different',
    'knowledgeable people assess the same organization with the same grid.',
    '',
    '## 2. Overall reliability',
    '',
    '| Metric | Value | Interpretation |',
    '|--------|-------|----------------|',
    `| Questions | ${referentialCodes.length} | |`,
    `| Raters | ${respondents.length} | |`,
    `| Exact agreement (all raters give the same level) | ${exactCount} (${pct(exactCount, matrix.length)}) | Modest: role-based perspectives differ |`,
    `| Within-1 agreement (spread ≤ 1) | ${withinOneCount} (${pct(withinOneCount, matrix.length)}) | High, but partly expected on an ordinal 1–5 scale |`,
    `| Contested questions (spread ≥ 2) | ${contestedCount} (${pct(contestedCount, matrix.length)}) | Few real divergences |`,
    `| **Fleiss' κ (unweighted)** | **${fmt(unweighted.kappa)}** | **${interpretKappa(unweighted.kappa)}** |`,
    `| Fleiss' κ 95% bootstrap CI | [${fmt(unweightedCi[0])}, ${fmt(unweightedCi[1])}] | Uncertainty due to the small sample |`,
    `| **Linear weighted κ** | **${fmt(weighted.kappa)}** | **${interpretKappa(weighted.kappa)}** (ordinal proximity) |`,
    `| Weighted κ 95% bootstrap CI | [${fmt(weightedCi[0])}, ${fmt(weightedCi[1])}] | |`,
    `| Mean pairwise Cohen's κ | ${fmt(pairwiseKappas.reduce((sum, pair) => sum + pair.kappa, 0) / pairwiseKappas.length)} | Pairwise view of the same data |`,
    '',
    '> **Why Fleiss\' κ?** With three raters, the standard coefficient is Fleiss\'',
    '> kappa (1971), not a pairwise average. It corrects for agreement expected by',
    '> chance using the observed marginal distribution, not a fixed 20% assumption.',
    '> The linear weighted kappa gives partial credit for nearby levels (1 vs 2) and',
    '> is the appropriate coefficient for an ordinal 1–5 scale.',
    '',
    '### Pairwise Cohen\'s κ',
    '',
    '| Pair | κ | Interpretation |',
    '|---|---:|---|',
    ...pairwiseKappas.map(
      (pair) =>
        `| ${respondentLabel(pair.left)} ↔ ${respondentLabel(pair.right)} | ${fmt(pair.kappa)} | ${interpretKappa(pair.kappa)} |`
    ),
    '',
    '## 3. Agreement by domain',
    '',
    '| Domain | Questions | Exact | Within-1 | Contested | Fleiss\' κ | Linear weighted κ |',
    '|--------|-----------|-------|----------|-----------|-----------:|------------------:|',
    ...domainStats.map(
      (domain) =>
        `| ${domain.domain} | ${domain.questions} | ${domain.exact} (${pct(domain.exact, domain.questions)}) | ` +
        `${domain.withinOne} (${pct(domain.withinOne, domain.questions)}) | ${domain.contested} | ` +
        `${fmt(domain.kappa)} | ${fmt(domain.weightedKappa)} |`
    ),
    '',
    '> Per-domain kappas are indicative only: a domain with 6–9 questions has a very',
    '> wide confidence interval. The overall coefficient is the robust result.',
    '',
    '## 4. Most contested questions',
    '',
    '*(Spread ≥ 2 between the highest and lowest answer)*',
    '',
    '| Question | ' + respondents.map(shortLabel).join(' | ') + ' | Spread |',
    '|----------|' + respondents.map(() => '---:').join('|') + '|---:|',
    ...contested.map(
      (question) =>
        `| ${question.code} | ${question.levels.join(' | ')} | ${question.spread} |`
    ),
    '',
    ...(contested.length === 0 ? ['_No question has a spread of 2 or more._', ''] : []),
    '## 5. Interpretation',
    '',
    `- **Fleiss\' κ = ${fmt(unweighted.kappa)} (${interpretKappa(unweighted.kappa)})** sits on the`,
    '  Fair/Moderate boundary (Landis & Koch: 0.21–0.40 Fair, 0.41–0.60 Moderate);',
    '  the confidence interval spans both. The agreement is moderate, not strong:',
    '  the grid is usable, but a single respondent should not be treated as ground',
    '  truth.',
    `- **Linear weighted κ = ${fmt(weighted.kappa)} (${interpretKappa(weighted.kappa)})** is higher because`,
    '  most disagreements are only ±1 level. This is the more relevant coefficient',
    '  for an ordinal maturity scale.',
    '- **GOV and PEOPLE have near-zero or negative kappa** in this scenario. This is',
    '  a useful warning: governance and culture questions are interpreted very',
    '  differently by security, data and risk profiles. They need evidence and',
    '  multi-respondent review more than technical questions do.',
    `- **Exact agreement is ${pct(exactCount, matrix.length)}.** It is modest: different roles`,
    '  do not see the same organization in exactly the same way.',
    `- **Within-1 agreement is ${pct(withinOneCount, matrix.length)}.** This is high, but a 5-point`,
    '  ordinal scale makes within-1 agreement mechanically likely; that is why the',
    '  kappa coefficients, not the raw percentage, are the main result.',
    '- The main source of variability is **role-based perspective** (RSSI vs CDO vs',
    '  Risk Manager), not random answers.',
    '',
    '## 6. Limitations',
    '',
    '1. **Simulated respondents.** The three profiles are constructed from the',
    '   Banque Atlas Digitale scenario; they are not real employees. The results',
    '   demonstrate the method, not the reliability of a real organization.',
    '2. **Small sample.** 59 questions and 3 raters give a wide bootstrap confidence',
    '   interval. A production study should use more raters and, ideally, several',
    '   organizations.',
    '3. **No test-retest.** This analysis measures agreement between raters at one',
    '   point in time, not the stability of the same rater over time.',
    '4. **Fictive organization.** Banque Atlas Digitale is a realistic but fictive',
    '   bank; the answers are a scenario, not an audit.',
    '5. **Ordinal scale.** Kappa assumes categorical labels; the weighted version',
    '   partially addresses the ordinal nature of the 1–5 scale, but no coefficient',
    '   is perfect.',
    '',
    '## 7. Recommendations',
    '',
    '| Recommendation | Rationale |',
    '|---|---|',
    '| Use at least 2 respondents for high-stakes assessments | Contested questions reveal knowledge gaps |',
    '| Provide per-level examples in the questionnaire | Reduces interpretation differences |',
    '| Require evidence attachments for criticality-3 questions | Forces explicit knowledge |',
    '| Average scores across respondents, or take the minimum | Conservative and defensible |',
    '| Report the kappa and its confidence interval | A raw agreement percentage overstates reliability |',
    '',
    '## 8. Reproduce',
    '',
    '```bash',
    'node scripts/case-study/multi-respondent.mjs',
    'node scripts/case-study/multi-respondent.mjs --check',
    '```',
    '',
    'To analyse real data, copy the scenario shape and replace `answers` with the',
    'answers exported from the platform:',
    '',
    '```bash',
    'node scripts/case-study/multi-respondent.mjs --scenario my-real-data.json',
    '```',
    '',
    '## References',
    '',
    '- Fleiss, J.L. (1971). Measuring nominal scale agreement among many raters.',
    '  *Psychological Bulletin*, 76(5), 378–382.',
    '- Landis, J.R. & Koch, G.G. (1977). The measurement of observer agreement for',
    '  categorical data. *Biometrics*, 33(1), 159–174.',
    '- Cohen, J. (1960). A coefficient of agreement for nominal scales.',
    '  *Educational and Psychological Measurement*, 20(1), 37–46.',
    '',
  ];
  return lines.join('\n');
}

const report = renderReport();
writeFileSync(outputPath, report, 'utf8');

console.log('='.repeat(78));
console.log('AIMA — Multi-Respondent Inter-Rater Reliability Analysis');
console.log('='.repeat(78));
console.log(`Scenario: ${scenarioPath}`);
console.log(`Respondents: ${respondents.map((respondent) => respondent.fullName).join(', ')}`);
console.log(`Questions: ${referentialCodes.length}`);
console.log('');
console.log(`Exact agreement:       ${exactCount}/${matrix.length} (${pct(exactCount, matrix.length)})`);
console.log(`Within-1 agreement:    ${withinOneCount}/${matrix.length} (${pct(withinOneCount, matrix.length)})`);
console.log(`Contested (spread≥2):  ${contestedCount}/${matrix.length} (${pct(contestedCount, matrix.length)})`);
console.log(`Fleiss' kappa:         ${fmt(unweighted.kappa)} (${interpretKappa(unweighted.kappa)}) `
  + `95% CI [${fmt(unweightedCi[0])}, ${fmt(unweightedCi[1])}]`);
console.log(`Linear weighted kappa: ${fmt(weighted.kappa)} (${interpretKappa(weighted.kappa)}) `
  + `95% CI [${fmt(weightedCi[0])}, ${fmt(weightedCi[1])}]`);
console.log(`Mean pairwise kappa:   ${fmt(pairwiseKappas.reduce((sum, pair) => sum + pair.kappa, 0) / pairwiseKappas.length)}`);
console.log('');
console.log(`Report written to: ${outputPath}`);
