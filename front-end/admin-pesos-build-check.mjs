import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { build } from 'file:///D:/idea%20projects/front-end/node_modules/vite/dist/node/index.js';
import react from 'file:///D:/idea%20projects/front-end/node_modules/@vitejs/plugin-react/dist/index.js';

const root = 'D:/idea projects/front-end';
const replacements = new Map([
    ['src/pages/AdminHobbies.jsx', resolve('admin-pesos/front-end/src/pages/AdminHobbies.jsx')],
    ['src/components/HobbyRecommendationFields.jsx', resolve('admin-pesos/front-end/src/components/HobbyRecommendationFields.jsx')],
    ['src/pages/css/HobbyRecommendationFields.css', resolve('admin-pesos/front-end/src/pages/css/HobbyRecommendationFields.css')],
].map(([relative, staged]) => [`${root}/${relative}`, staged]));

await build({
    root,
    configFile: false,
    plugins: [{
        name: 'staged-admin-review',
        enforce: 'pre',
        resolveId(source, importer) {
            if (!importer || !source.startsWith('.')) return null;
            const candidate = resolve(importer.split('?')[0], '..', source).replaceAll('\\', '/');
            for (const path of [candidate, `${candidate}.jsx`]) {
                if (replacements.has(path)) return path;
            }
            return null;
        },
        load(id) {
            const staged = replacements.get(id.split('?')[0].replaceAll('\\', '/'));
            return staged ? readFileSync(staged, 'utf8') : null;
        },
    }, react()],
    build: { outDir: resolve('admin-pesos-build'), emptyOutDir: true },
});