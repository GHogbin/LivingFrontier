import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const resources = path.join(root, 'src', 'main', 'resources');
const namespace = 'livingfrontier';
const writeJson = (name, value) => {
    const target = path.join(resources, name);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, JSON.stringify(value, null, 2) + '\n');
};
const data = (name, value) => writeJson(`data/${namespace}/${name}.json`, value);
const tag = (name, values) => data(`tags/worldgen/biome/${name}`, { replace: false, values });
const uniform = (min, max) => ({ type: 'minecraft:uniform', min, max });
const item = (name, min = 1, max = min, weight = 1) => ({
    type: 'minecraft:item', name: `minecraft:${name}`, weight,
    functions: [{ function: 'minecraft:set_count', count: min === max ? min : uniform(min, max) }]
});
const pool = (entries, rolls = 1) => ({ rolls, entries });

tag('deer_habitat', ['#minecraft:is_forest', '#minecraft:is_taiga', 'minecraft:plains', 'minecraft:meadow']);
tag('songbird_habitat', ['#minecraft:is_forest', '#minecraft:is_taiga', 'minecraft:plains', 'minecraft:meadow']);
tag('firefly_habitat', ['minecraft:swamp', 'minecraft:mangrove_swamp', 'minecraft:forest', 'minecraft:flower_forest']);
tag('raider_habitat', ['minecraft:plains', '#minecraft:is_taiga', '#minecraft:is_forest']);
tag('boar_habitat', ['#minecraft:is_forest', '#minecraft:is_taiga', 'minecraft:plains']);
tag('prowler_habitat', ['#minecraft:is_forest', '#minecraft:is_taiga']);
tag('sky_wraith_habitat', ['#minecraft:is_forest', '#minecraft:is_taiga', 'minecraft:plains', 'minecraft:meadow']);
tag('has_structure/raider_camp', ['minecraft:plains', 'minecraft:forest', 'minecraft:birch_forest', '#minecraft:is_taiga']);
tag('has_structure/ruined_keep', ['minecraft:plains', 'minecraft:meadow', 'minecraft:forest', 'minecraft:birch_forest']);

for (const name of ['deer', 'songbird', 'firefly', 'raider', 'boar', 'prowler', 'sky_wraith']) {
    data(`forge/biome_modifier/${name}_spawns`, {
        type: `${namespace}:wildlife_spawns`, biomes: `#${namespace}:${name}_habitat`, mob: name
    });
}

const names = { deer: 'Deer', songbird: 'Songbird', firefly: 'Firefly', raider: 'Frontier Raider',
    warlord: 'Frontier Warlord', boar: 'Wild Boar', prowler: 'Forest Prowler', sky_wraith: 'Sky Wraith' };
const language = {
    'message.livingfrontier.encounter.raider': 'You hear raiders approaching in the distance...',
    'message.livingfrontier.encounter.prowler': 'A hunting pack is stalking the forest nearby...',
    'message.livingfrontier.encounter.sky_wraith': 'Something circles in the sky above...'
};
for (const [name, label] of Object.entries(names)) {
    language[`entity.${namespace}.${name}`] = label;
    language[`item.${namespace}.${name}_spawn_egg`] = `${label} Spawn Egg`;
    const egg = `${name}_spawn_egg`;
    writeJson(`assets/${namespace}/models/item/${egg}.json`, {
        parent: 'minecraft:item/template_spawn_egg'
    });
}
writeJson(`assets/${namespace}/lang/en_us.json`, language);
writeJson(`assets/${namespace}/lang/en_gb.json`, language);
data('loot_tables/entities/deer', { type: 'minecraft:entity', pools: [pool([item('leather', 0, 2)]), pool([item('beef', 1, 2)])] });
data('loot_tables/entities/songbird', { type: 'minecraft:entity', pools: [pool([item('feather', 0, 1)])] });
data('loot_tables/entities/firefly', { type: 'minecraft:entity', pools: [] });
data('loot_tables/entities/boar', { type: 'minecraft:entity', pools: [pool([item('porkchop', 1, 3)]), pool([item('leather', 0, 1)])] });
data('loot_tables/entities/prowler', { type: 'minecraft:entity', pools: [pool([item('bone', 1, 2)]), pool([item('leather', 0, 1)])] });
data('loot_tables/entities/sky_wraith', { type: 'minecraft:entity', pools: [pool([item('phantom_membrane', 0, 1)])] });
data('loot_tables/entities/raider', {
    type: 'minecraft:entity',
    pools: [pool([item('emerald', 0, 2)]), pool([item('iron_nugget', 1, 5)])]
});
data('loot_tables/entities/warlord', {
    type: 'minecraft:entity',
    pools: [pool([item('diamond', 3, 5)]), pool([item('emerald', 8, 16)]), pool([item('golden_apple', 1, 2)]),
        pool([item('sentry_armor_trim_smithing_template')])]
});
data('loot_tables/chests/raider_camp', {
    type: 'minecraft:chest',
    pools: [pool([item('bread', 2, 5, 8), item('iron_ingot', 1, 3, 5), item('arrow', 4, 12, 7),
        item('emerald', 1, 3, 3), item('leather', 2, 5, 4), item('golden_apple', 1, 1, 1)], uniform(3, 5))]
});
data('loot_tables/chests/ruined_keep', {
    type: 'minecraft:chest',
    pools: [pool([item('iron_ingot', 3, 8, 7), item('gold_ingot', 2, 6, 5), item('diamond', 1, 3, 2),
        item('emerald', 3, 8, 5), item('golden_apple', 1, 2, 2), item('experience_bottle', 3, 8, 4)], uniform(4, 6))]
});

for (const [name, spacing, salt] of [['raider_camp', 28, 7389211], ['ruined_keep', 52, 7389239]]) {
    data(`worldgen/template_pool/${name}`, {
        fallback: 'minecraft:empty',
        elements: [{ weight: 1, element: {
            element_type: 'minecraft:single_pool_element', location: `${namespace}:${name}`,
            processors: 'minecraft:empty', projection: 'rigid'
        } }]
    });
    data(`worldgen/structure/${name}`, {
        type: 'minecraft:jigsaw', biomes: `#${namespace}:has_structure/${name}`,
        step: 'surface_structures', spawn_overrides: {}, terrain_adaptation: 'beard_thin',
        // Keep the root template in the generation graph on both platform versions.
        start_pool: `${namespace}:${name}`, size: 1, start_height: { absolute: 0 },
        project_start_to_heightmap: 'WORLD_SURFACE_WG', use_expansion_hack: false,
        max_distance_from_center: 64
    });
    data(`worldgen/structure_set/${name}`, {
        structures: [{ structure: `${namespace}:${name}`, weight: 1 }],
        placement: { type: 'minecraft:random_spread', spacing, separation: Math.floor(spacing / 3), salt,
            exclusion_zone: { other_set: 'minecraft:villages', chunk_count: 8 } }
    });
}

// Minimal typed NBT writer: structure coordinates must retain int/double/float types.
const typed = (type, value) => ({ type, value });
const int = value => typed(3, value);
const byte = value => typed(1, value);
const float = value => typed(5, value);
const double = value => typed(6, value);
const string = value => typed(8, value);
const list = (type, values) => typed(9, { type, values });
const compound = value => typed(10, value);
const numbers = (type, values) => list(type, values);
const shortString = text => {
    const contents = Buffer.from(text, 'utf8');
    const size = Buffer.alloc(2);
    size.writeUInt16BE(contents.length);
    return Buffer.concat([size, contents]);
};
function payload(type, value) {
    if (type === 8) return shortString(value);
    if (type === 10) return Buffer.concat([
        ...Object.entries(value).map(([name, tag]) => Buffer.concat([Buffer.from([tag.type]), shortString(name), payload(tag.type, tag.value)])),
        Buffer.from([0])
    ]);
    if (type === 9) {
        const header = Buffer.alloc(5);
        header.writeUInt8(value.type);
        header.writeInt32BE(value.values.length, 1);
        return Buffer.concat([header, ...value.values.map(item => payload(value.type, item))]);
    }
    const sizes = { 1: 1, 3: 4, 5: 4, 6: 8 };
    if (!(type in sizes)) throw new Error(`Unsupported NBT type ${type}`);
    const buffer = Buffer.alloc(sizes[type]);
    if (type === 1) buffer.writeInt8(value);
    if (type === 3) buffer.writeInt32BE(value);
    if (type === 5) buffer.writeFloatBE(value);
    if (type === 6) buffer.writeDoubleBE(value);
    return buffer;
}

class Structure {
    constructor(size) {
        this.size = size;
        this.blocks = new Map();
        this.entities = [];
    }
    block(x, y, z, name, properties = {}, nbt = null) {
        if ([x, y, z].some((coord, index) => coord < 0 || coord >= this.size[index])) {
            throw new Error(`Block outside structure: ${x},${y},${z}`);
        }
        this.blocks.set(`${x},${y},${z}`, { pos: [x, y, z], name: `minecraft:${name}`, properties, nbt });
    }
    fill(x1, y1, z1, x2, y2, z2, name, properties = {}) {
        for (let x = x1; x <= x2; x++) for (let y = y1; y <= y2; y++) for (let z = z1; z <= z2; z++) {
            this.block(x, y, z, name, properties);
        }
    }
    chest(x, y, z, loot, facing = 'south') {
        this.block(x, y, z, 'chest', { facing, type: 'single', waterlogged: 'false' }, {
            id: string('minecraft:chest'), LootTable: string(`${namespace}:chests/${loot}`)
        });
    }
    mob(name, x, y, z, yaw = 0) {
        this.entities.push({
            pos: numbers(6, [x + 0.5, y, z + 0.5]), blockPos: numbers(3, [x, y, z]),
            nbt: compound({
                id: string(`${namespace}:${name}`), Pos: numbers(6, [x + 0.5, y, z + 0.5]),
                Rotation: numbers(5, [yaw, 0]), Motion: numbers(6, [0, 0, 0]),
                PersistenceRequired: byte(1), Health: float(name === 'warlord' ? 160 : 26)
            })
        });
    }
    save(name) {
        const palette = [];
        const states = new Map();
        const blocks = [...this.blocks.values()].map(block => {
            const key = JSON.stringify([block.name, block.properties]);
            if (!states.has(key)) {
                states.set(key, palette.length);
                const state = { Name: string(block.name) };
                if (Object.keys(block.properties).length) {
                    state.Properties = compound(Object.fromEntries(Object.entries(block.properties).map(([k, v]) => [k, string(v)])));
                }
                palette.push(state);
            }
            const result = { pos: numbers(3, block.pos), state: int(states.get(key)) };
            if (block.nbt) result.nbt = compound(block.nbt);
            return result;
        });
        const body = { DataVersion: int(3465), size: numbers(3, this.size),
            palette: list(10, palette), blocks: list(10, blocks), entities: list(10, this.entities) };
        const nbt = Buffer.concat([Buffer.from([10, 0, 0]), payload(10, body)]);
        const target = path.join(resources, 'data', namespace, 'structures', `${name}.nbt`);
        fs.mkdirSync(path.dirname(target), { recursive: true });
        fs.writeFileSync(target, zlib.gzipSync(nbt));
        console.log(`${name}: ${blocks.length} blocks, ${this.entities.length} inhabitants`);
    }
}

const camp = new Structure([21, 9, 21]);
camp.fill(0, 1, 0, 20, 8, 20, 'air');
camp.fill(0, 0, 0, 20, 0, 20, 'grass_block', { snowy: 'false' });
camp.fill(3, 0, 3, 17, 0, 17, 'coarse_dirt');
for (let coord = 1; coord < 20; coord++) {
    for (let y = 1; y <= 3; y++) {
        camp.block(coord, y, 1, 'spruce_log', { axis: 'y' });
        camp.block(coord, y, 19, 'spruce_log', { axis: 'y' });
        camp.block(1, y, coord, 'spruce_log', { axis: 'y' });
        camp.block(19, y, coord, 'spruce_log', { axis: 'y' });
    }
}
camp.fill(9, 1, 18, 11, 3, 20, 'air');
for (const [x, z] of [[4, 4], [13, 4]]) {
    camp.fill(x, 1, z, x + 3, 1, z + 4, 'spruce_planks');
    camp.fill(x, 2, z + 3, x + 3, 4, z + 3, 'white_wool');
    camp.fill(x, 2, z, x, 3, z + 2, 'white_wool');
    camp.fill(x + 3, 2, z, x + 3, 3, z + 2, 'white_wool');
    camp.fill(x, 4, z, x + 3, 4, z + 2, 'white_wool');
    camp.fill(x + 1, 5, z, x + 2, 5, z + 3, 'white_wool');
}
camp.block(10, 1, 10, 'campfire', { facing: 'north', lit: 'true', signal_fire: 'false', waterlogged: 'false' });
for (const [x, z] of [[7, 10], [13, 10], [10, 7], [10, 13]]) {
    camp.block(x, 1, z, 'spruce_log', { axis: 'x' });
}
camp.chest(5, 2, 6, 'raider_camp');
camp.chest(14, 2, 6, 'raider_camp');
camp.fill(3, 1, 14, 5, 5, 16, 'spruce_planks');
camp.fill(3, 2, 14, 5, 4, 16, 'air');
camp.block(4, 2, 15, 'ladder', { facing: 'south', waterlogged: 'false' });
for (let y = 3; y <= 5; y++) camp.block(4, y, 15, 'ladder', { facing: 'south', waterlogged: 'false' });
camp.block(4, 6, 14, 'lantern', { hanging: 'false', waterlogged: 'false' });
camp.mob('raider', 8, 1, 17, 180);
camp.mob('raider', 12, 1, 17, 180);
camp.mob('raider', 6, 1, 9, 90);
camp.mob('raider', 15, 1, 11, 270);
camp.save('raider_camp');

const keep = new Structure([29, 15, 29]);
keep.fill(0, 1, 0, 28, 14, 28, 'air');
keep.fill(0, 0, 0, 28, 0, 28, 'cobblestone');
keep.fill(2, 0, 2, 26, 0, 26, 'stone_bricks');
for (let coord = 2; coord <= 26; coord++) {
    const height = coord % 7 === 0 ? 3 : 6;
    for (let y = 1; y <= height; y++) {
        const material = (coord + y) % 5 === 0 ? 'mossy_stone_bricks' : 'stone_bricks';
        keep.block(coord, y, 2, material);
        keep.block(coord, y, 26, material);
        keep.block(2, y, coord, material);
        keep.block(26, y, coord, material);
    }
    if (coord % 2 === 0) {
        for (const [x, z] of [[coord, 2], [coord, 26], [2, coord], [26, coord]]) {
            keep.block(x, height + 1, z, 'stone_bricks');
        }
    }
}
keep.fill(12, 1, 25, 16, 4, 28, 'air');
for (const [x, z] of [[3, 3], [21, 3], [3, 21], [21, 21]]) {
    keep.fill(x, 1, z, x + 4, 9, z + 4, 'stone_bricks');
    keep.fill(x + 1, 1, z + 1, x + 3, 8, z + 3, 'air');
    keep.fill(x + 1, 1, z + 4, x + 3, 3, z + 4, 'air');
    keep.fill(x, 9, z, x + 4, 9, z + 4, 'stone_bricks');
    for (let edge = 0; edge <= 4; edge += 2) {
        keep.block(x + edge, 10, z, 'stone_bricks');
        keep.block(x + edge, 10, z + 4, 'stone_bricks');
        keep.block(x, 10, z + edge, 'stone_bricks');
        keep.block(x + 4, 10, z + edge, 'stone_bricks');
    }
}
keep.fill(9, 1, 4, 19, 7, 12, 'stone_bricks');
keep.fill(10, 1, 5, 18, 6, 11, 'air');
keep.fill(12, 1, 11, 16, 4, 12, 'air');
keep.fill(10, 7, 5, 18, 7, 11, 'air');
keep.fill(13, 1, 6, 15, 1, 8, 'polished_deepslate');
keep.fill(13, 2, 5, 15, 4, 5, 'polished_deepslate');
keep.fill(12, 1, 13, 16, 1, 15, 'air');
for (let z = 13; z <= 24; z++) {
    keep.block(14, 0, z, 'cracked_stone_bricks');
    if (z % 4 === 0) {
        for (const x of [9, 19]) {
            keep.block(x, 1, z, 'cobblestone_wall', { up: 'true', north: 'none', south: 'none', east: 'none', west: 'none', waterlogged: 'false' });
            keep.block(x, 2, z, 'lantern', { hanging: 'false', waterlogged: 'false' });
        }
    }
}
keep.chest(10, 1, 7, 'ruined_keep', 'east');
keep.chest(18, 1, 7, 'ruined_keep', 'west');
keep.mob('warlord', 14, 2, 7, 180);
for (const [x, z] of [[11, 21], [17, 21], [8, 15], [20, 15], [11, 10], [17, 10]]) keep.mob('raider', x, 1, z, 180);
keep.save('ruined_keep');

for (const [name, size] of [['empty', [8, 8, 8]], ['arena', [64, 18, 40]], ['encounters', [128, 32, 128]]]) {
    const floor = [];
    if (name === 'encounters') {
        for (let x = 0; x < size[0]; x++) for (let z = 0; z < size[2]; z++) {
            floor.push({ pos: numbers(3, [x, 1, z]), state: int(0) });
        }
    }
    const body = { DataVersion: int(3465), size: numbers(3, size),
        palette: list(10, name === 'encounters' ? [{ Name: string('minecraft:grass_block') }] : []),
        blocks: list(10, floor), entities: list(10, []) };
    const target = path.join(root, 'src', 'gametest', 'resources', 'data', namespace, 'structures', 'test', `${name}.nbt`);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, zlib.gzipSync(Buffer.concat([Buffer.from([10, 0, 0]), payload(10, body)])));
}
