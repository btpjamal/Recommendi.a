#!/usr/bin/env node
// Importador conservador de presets iD para o catálogo Recommendi.a.
// Uso: node scripts/importar-catalogo-osm.mjs ./id-tagging-schema

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const schemaRoot = path.resolve(process.argv[2] ?? './id-tagging-schema');
const baseDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const curatedPath = path.join(baseDir, 'src/data/osmCatalogoPtBr.json');
const outputPath = path.join(baseDir, 'src/data/osmCatalogoAmpliadoPtBr.json');
const reportPath = path.join(baseDir, 'relatorio-importacao-osm.json');

const presetDir = path.join(schemaRoot, 'data/presets');
const translationsDir = path.join(schemaRoot, 'dist/translations');

const validToken = /^[a-zA-Z0-9_:.-]+$/;

const allowedKeys = new Set([
    'leisure',
    'sport',
    'amenity',
    'tourism',
    'natural',
    'highway',
    'landuse',
    'shop',
    'craft',
    'historic',
    'man_made',
    'waterway',
    'aeroway',
    'public_transport'
]);

const categoryNames = {
    leisure: 'Lazer',
    sport: 'Esportes',
    amenity: 'Serviços e equipamentos',
    tourism: 'Turismo',
    natural: 'Natureza',
    highway: 'Caminhos e vias',
    landuse: 'Uso do solo',
    shop: 'Comércio',
    craft: 'Ofícios',
    historic: 'História e cultura',
    man_made: 'Estruturas',
    waterway: 'Água',
    aeroway: 'Aviação',
    public_transport: 'Transporte público'
};

const read = file =>
    JSON.parse(fs.readFileSync(file, 'utf8'));

function translations(locale) {
    const file = path.join(translationsDir, `${locale}.json`);

    if (!fs.existsSync(file)) return {};

    const raw = read(file);

    return (raw[locale] ?? raw).presets?.presets ?? {};
}

function* walk(dir) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);

        if (entry.isDirectory()) {
            yield* walk(full);
        } else if (entry.name.endsWith('.json')) {
            yield full;
        }
    }
}

function terms(value) {
    if (Array.isArray(value)) {
        return value
            .filter(x => typeof x === 'string')
            .map(x => x.trim())
            .filter(Boolean);
    }

    if (typeof value === 'string') {
        return value
            .split(',')
            .map(x => x.trim())
            .filter(Boolean);
    }

    return [];
}

// Validação dos arquivos de entrada
if (!fs.existsSync(curatedPath)) {
    throw new Error(`Catálogo inicial ausente: ${curatedPath}`);
}

if (!fs.existsSync(presetDir)) {
    throw new Error(`Presets ausentes: ${presetDir}`);
}

const ptBR = translations('pt-BR');
const pt = translations('pt');

const original = read(curatedPath);

if (!Array.isArray(original)) {
    throw new Error('Catálogo inicial precisa ser um array JSON.');
}

// Identifica condições OSM equivalentes
const signature = ({ osmKey, osmValue, matchType }) =>
    `${osmKey}|${osmValue ?? ''}|${matchType}`;

// Preserva as entradas existentes
const output = new Map(
    original.map(item => [signature(item), item])
);

const report = {
    versaoGit: '5b05b7cb (referência fornecida; confira se seu clone mudou)',
    analisados: 0,
    importados: 0,
    duplicados: 0,
    semTraducao: 0,
    naoCompativeis: 0,
    mantidosDoCatalogoInicial: original.length,
    total: 0,
    fonte: schemaRoot,
    observacao:
        'Revisar manualmente a relevância e a semântica dos presets importados antes de disponibilizar aos administradores.'
};

// Importação dos presets
for (const file of walk(presetDir)) {
    report.analisados++;

    const id = path
        .relative(presetDir, file)
        .split(path.sep)
        .join('/')
        .replace(/\.json$/, '');

    const preset = read(file);

    const entries =
        preset.tags &&
        typeof preset.tags === 'object' &&
        !Array.isArray(preset.tags)
            ? Object.entries(preset.tags)
            : [];

    // Primeira versão: somente presets com uma tag
    if (entries.length !== 1) {
        report.naoCompativeis++;
        continue;
    }

    const [osmKey, rawValue] = entries[0];

    if (
        !allowedKeys.has(osmKey) ||
        typeof rawValue !== 'string' ||
        !validToken.test(osmKey) ||
        (rawValue !== '*' && !validToken.test(rawValue))
    ) {
        report.naoCompativeis++;
        continue;
    }

    // Prioriza pt-BR e utiliza pt como alternativa
    const translation = ptBR[id]?.name
        ? ptBR[id]
        : pt[id];

    if (
        typeof translation?.name !== 'string' ||
        !translation.name.trim()
    ) {
        report.semTraducao++;
        continue;
    }

    const matchType = rawValue === '*' ? 'EXISTE' : 'IGUAL';
    const osmValue = matchType === 'EXISTE' ? null : rawValue;

    const sig = signature({
        osmKey,
        osmValue,
        matchType
    });

    if (output.has(sig)) {
        report.duplicados++;
        continue;
    }

    output.set(sig, {
        id: `id/${id}`,
        categoria: categoryNames[osmKey] ?? osmKey,
        nome: translation.name.trim(),
        osmKey,
        osmValue,
        matchType,
        termosBusca: [
            ...new Set([
                ...terms(ptBR[id]?.terms),
                ...terms(pt[id]?.terms)
            ])
        ]
    });

    report.importados++;
}

// Organiza o catálogo em português
const result = [...output.values()].sort(
    (a, b) =>
        a.categoria.localeCompare(b.categoria, 'pt-BR') ||
        a.nome.localeCompare(b.nome, 'pt-BR')
);

report.total = result.length;

// Gera os arquivos de saída
fs.writeFileSync(
    outputPath,
    JSON.stringify(result, null, 2) + '\n',
    'utf8'
);

fs.writeFileSync(
    reportPath,
    JSON.stringify(report, null, 2) + '\n',
    'utf8'
);

console.log(
    `Analisados: ${report.analisados} | ` +
    `Importados: ${report.importados} | ` +
    `Sem tradução: ${report.semTraducao} | ` +
    `Incompatíveis: ${report.naoCompativeis} | ` +
    `Duplicados: ${report.duplicados} | ` +
    `Total: ${report.total}`
);

console.log(
    `Catálogo: ${outputPath}\nRelatório: ${reportPath}`
);
