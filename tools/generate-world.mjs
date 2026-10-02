import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import './generate-villages.mjs';

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
tag('traveller_habitat', ['#minecraft:is_forest', '#minecraft:is_taiga', 'minecraft:plains', 'minecraft:savanna', 'minecraft:meadow']);
tag('has_structure/raider_camp', ['minecraft:plains', 'minecraft:forest', 'minecraft:birch_forest', '#minecraft:is_taiga']);
tag('has_structure/ruined_keep', ['minecraft:plains', 'minecraft:meadow', 'minecraft:forest', 'minecraft:birch_forest']);

for (const name of ['deer', 'songbird', 'firefly', 'raider', 'boar', 'prowler', 'sky_wraith', 'traveller']) {
    data(`forge/biome_modifier/${name}_spawns`, {
        type: `${namespace}:wildlife_spawns`, biomes: `#${namespace}:${name}_habitat`, mob: name
    });
}

const names = { deer: 'Deer', songbird: 'Songbird', firefly: 'Firefly', raider: 'Frontier Raider',
    warlord: 'Frontier Warlord', boar: 'Wild Boar', prowler: 'Forest Prowler', sky_wraith: 'Sky Wraith',
    traveller: 'Traveller', village_guard: 'Village Guard' };
const language = {
    'block.livingfrontier.sun_rune': 'Sun Rune (Gold)',
    'block.livingfrontier.leaf_rune': 'Leaf Rune (Moss)',
    'block.livingfrontier.wave_rune': 'Wave Rune (Blue)',
    'block.livingfrontier.seal_controller': 'Three-Seal Controller',
    'block.livingfrontier.seal_barrier': 'Sealed Portcullis',
    'message.livingfrontier.puzzle.unbound': 'This rune is unbound, or its seal controller is not loaded.',
    'message.livingfrontier.puzzle.already_solved': 'Seal %s is already complete.',
    'message.livingfrontier.puzzle.wrong': 'Wrong rune: seal %s sequence reset. Read the room hint.',
    'message.livingfrontier.puzzle.seal_complete': 'Seal %s complete! %s / 3 seals awakened.',
    'message.livingfrontier.puzzle.progress': 'Seal %s: %s / 3 runes correct.',
    'message.livingfrontier.puzzle.reset': 'Unfinished sequences reset. %s completed seals remain.',
    'message.livingfrontier.puzzle.status': '%s / 3 seals complete. Sneak-right-click here to reset unfinished sequences.',
    'message.livingfrontier.encounter.raider': 'You hear raiders approaching in the distance...',
    'message.livingfrontier.encounter.prowler': 'A hunting pack is stalking the forest nearby...',
    'message.livingfrontier.encounter.sky_wraith': 'Something circles in the sky above...',
    'message.livingfrontier.guard': 'I keep watch around the bell. Stay close to the village after dark.',
    'message.livingfrontier.guard.archer': 'I cover the roads with my bow, and fight up close if trouble reaches the village.',
    'message.livingfrontier.traveller.0': 'I have come from the woodland roads. Prowlers hunt there after sunset.',
    'message.livingfrontier.traveller.1': 'Listen before you look up. A wraith warns you before it dives.',
    'message.livingfrontier.traveller.2': 'There are ruined keeps beyond the fields. Ask the villagers if they need supplies.',
    'message.livingfrontier.villager.greeting.farmer': 'Good to see a friendly face. The fields keep us busy.',
    'message.livingfrontier.villager.greeting.librarian': 'Every traveller brings a story. Perhaps you can help with our supplies.',
    'message.livingfrontier.villager.greeting.smith': 'There is always more work at the forge. A village needs good tools.',
    'message.livingfrontier.villager.greeting.general': 'Welcome. Stay a while; there is more to a village than its trading stalls.',
    'message.livingfrontier.villager.request': 'Bring me %s %s. Hold them and sneak-right-click me again for 2 emeralds.',
    'message.livingfrontier.villager.completed': 'Thank you! Here are 2 emeralds. Your help will be remembered.',
    'message.livingfrontier.villager.thanks_today': 'You have already helped today. Come back after a full village day.',
    'message.livingfrontier.villager.other_errand': 'Finish the supply errand you accepted from another villager first.',
    'message.livingfrontier.villager.unwelcome': 'We have not forgotten the trouble you caused. Give us some space.'
};
for (const [name, label] of Object.entries(names)) {
    language[`entity.${namespace}.${name}`] = label;
    language[`item.${namespace}.${name}_spawn_egg`] = `${label} Spawn Egg`;
    const egg = `${name}_spawn_egg`;
    writeJson(`assets/${namespace}/models/item/${egg}.json`, {
        parent: 'minecraft:item/template_spawn_egg'
    });
}
for (const locale of ['en_us', 'en_gb']) {
    const target = `assets/${namespace}/lang/${locale}.json`;
    const existing = fs.existsSync(path.join(resources, target))
        ? JSON.parse(fs.readFileSync(path.join(resources, target), 'utf8')) : {};
    writeJson(target, { ...existing, ...language });
}
for (const [name, texture] of [['sun_rune', 'gold_block'], ['leaf_rune', 'mossy_stone_bricks'],
    ['wave_rune', 'lapis_block'], ['seal_controller', 'chiseled_deepslate'], ['seal_barrier', 'iron_bars']]) {
    writeJson(`assets/${namespace}/blockstates/${name}.json`, {
        variants: { '': { model: `${namespace}:block/${name}` } }
    });
    writeJson(`assets/${namespace}/models/block/${name}.json`, {
        parent: 'minecraft:block/cube_all', render_type: name === 'seal_barrier' ? 'minecraft:cutout' : 'minecraft:solid',
        textures: { all: `minecraft:block/${texture}` }
    });
    writeJson(`assets/${namespace}/models/item/${name}.json`, { parent: `${namespace}:block/${name}` });
}
data('loot_tables/entities/deer', { type: 'minecraft:entity', pools: [pool([item('leather', 0, 2)]), pool([item('beef', 1, 2)])] });
data('loot_tables/entities/songbird', { type: 'minecraft:entity', pools: [pool([item('feather', 0, 1)])] });
data('loot_tables/entities/firefly', { type: 'minecraft:entity', pools: [] });
data('loot_tables/entities/boar', { type: 'minecraft:entity', pools: [pool([item('porkchop', 1, 3)]), pool([item('leather', 0, 1)])] });
data('loot_tables/entities/prowler', { type: 'minecraft:entity', pools: [pool([item('bone', 1, 2)]), pool([item('leather', 0, 1)])] });
data('loot_tables/entities/sky_wraith', { type: 'minecraft:entity', pools: [pool([item('phantom_membrane', 0, 1)])] });
data('loot_tables/entities/traveller', { type: 'minecraft:entity', pools: [] });
data('loot_tables/entities/village_guard', { type: 'minecraft:entity', pools: [] });
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
        if ([x, y, z].some((coord, index) => !Number.isInteger(coord) || coord < 0 || coord >= this.size[index])) {
            throw new Error(`Block outside structure: ${x},${y},${z}`);
        }
        this.blocks.set(`${x},${y},${z}`, {
            pos: [x, y, z], name: name.includes(':') ? name : `minecraft:${name}`, properties, nbt
        });
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
    wallSign(x, y, z, lines, facing = 'north') {
        assert(lines.length <= 4 && lines.every(line => line.length <= 20), 'Sign hints must fit four short lines');
        this.block(x, y, z, 'spruce_wall_sign', { facing, waterlogged: 'false' }, {
            id: string('minecraft:sign'), is_waxed: byte(1),
            front_text: compound({
                messages: list(8, [...lines, ...Array(4 - lines.length).fill('')].map(text => JSON.stringify({ text }))),
                color: string('black'), has_glowing_text: byte(0)
            })
        });
    }
    mob(name, x, y, z, yaw = 0, extraNbt = {}) {
        this.entities.push({
            pos: numbers(6, [x + 0.5, y, z + 0.5]), blockPos: numbers(3, [x, y, z]),
            nbt: compound({
                id: string(`${namespace}:${name}`), Pos: numbers(6, [x + 0.5, y, z + 0.5]),
                Rotation: numbers(5, [yaw, 0]), Motion: numbers(6, [0, 0, 0]),
                PersistenceRequired: byte(1), Health: float(name === 'warlord' ? 160 : 26), ...extraNbt
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

const fence = { north: 'true', south: 'true', east: 'true', west: 'true', waterlogged: 'false' };
const bars = { ...fence };
const lantern = (site, x, y, z, hanging = true) => site.block(x, y, z, 'lantern', {
    hanging: String(hanging), waterlogged: 'false'
});
const banner = (site, x, y, z, facing = 'south') => site.block(x, y, z, 'red_wall_banner', { facing }, {
    id: string('minecraft:banner')
});
const campLayout = { size: [31, 13, 31], guards: 6, chests: [] };
const keepLayout = {
    size: [57, 22, 57], guards: 9, chests: [],
    entrance: [28, 1, 56], gateController: [25, 1, 17],
    gateOrigin: [27, 1, 14], gateWidth: 3, gateHeight: 4, bossPosition: [28, 1, 8],
    rooms: [
        { name: 'Dawn', room: 0, runes: [
            { symbol: 'sun', pos: [6, 1, 46] }, { symbol: 'leaf', pos: [8, 1, 46] }, { symbol: 'wave', pos: [10, 1, 46] }
        ], hintPosition: [8, 3, 47] },
        { name: 'River', room: 1, runes: [
            { symbol: 'sun', pos: [45, 1, 38] }, { symbol: 'leaf', pos: [47, 1, 38] }, { symbol: 'wave', pos: [49, 1, 38] }
        ], hintPosition: [47, 3, 39] },
        { name: 'Root', room: 2, runes: [
            { symbol: 'sun', pos: [6, 1, 25] }, { symbol: 'leaf', pos: [8, 1, 25] }, { symbol: 'wave', pos: [10, 1, 25] }
        ], hintPosition: [8, 3, 26] }
    ]
};
const supply = (site, layout, x, y, z, loot, facing = 'south') => {
    site.chest(x, y, z, loot, facing);
    layout.chests.push([x, y, z]);
};

const camp = new Structure(campLayout.size);
camp.fill(0, 1, 0, 30, 12, 30, 'air');
camp.fill(0, 0, 0, 30, 0, 30, 'grass_block', { snowy: 'false' });
for (let x = 2; x <= 28; x++) for (let z = 2; z <= 28; z++) {
    if ((x >= 13 && x <= 17) || (z >= 11 && z <= 12) || (z >= 20 && z <= 22)) {
        camp.block(x, 0, z, (x * 7 + z * 3) % 9 === 0 ? 'gravel' : 'coarse_dirt');
    }
}
camp.fill(14, 0, 28, 16, 0, 30, 'gravel');
for (let edge = 1; edge <= 29; edge++) {
    for (const [x, z] of [[edge, 1], [edge, 29], [1, edge], [29, edge]]) {
        if (z === 29 && x >= 13 && x <= 17) continue;
        const post = edge % 3 === 1;
        camp.fill(x, 1, z, x, post ? 3 : 2, z, post ? 'spruce_log' : 'spruce_fence', post ? { axis: 'y' } : fence);
    }
}
for (const x of [12, 18]) camp.fill(x, 1, 29, x, 5, 29, 'spruce_log', { axis: 'y' });
camp.fill(12, 5, 29, 18, 5, 29, 'spruce_log', { axis: 'x' });
banner(camp, 12, 4, 30);
banner(camp, 18, 4, 30);
camp.wallSign(18, 3, 30, ['The stone fortress', 'holds three seals', 'Seek Dawn and River', 'then the Root'], 'south');

function tent(x, z, width, depth, wool, facing) {
    const at = (u, v) => {
        if (facing === 'east') return [x + v, z + width - 1 - u];
        if (facing === 'west') return [x + depth - 1 - v, z + u];
        if (facing === 'north') return [x + width - 1 - u, z + depth - 1 - v];
        return [x + u, z + v];
    };
    const place = (u, y, v, block, properties = {}) => {
        const [wx, wz] = at(u, v);
        camp.block(wx, y, wz, block, properties);
    };
    const middle = Math.floor(width / 2);
    for (let u = 0; u < width; u++) for (let v = 0; v < depth; v++) {
        const ridge = 3 + Math.min(u, width - 1 - u);
        place(u, 0, v, u === middle ? 'red_wool' : 'spruce_planks');
        place(u, ridge, v, wool);
        if (u === 0 || u === width - 1 || v === 0 || v === depth - 1) {
            for (let y = 1; y < ridge; y++) place(u, y, v, wool);
        }
        if (v === depth - 1 && Math.abs(u - middle) <= 1) {
            place(u, 1, v, 'air');
            place(u, 2, v, 'air');
        }
    }
    for (const v of [0, depth - 1]) place(middle, 3 + middle, v, 'spruce_log', { axis: 'y' });
    const [cx, cz] = at(1, 2);
    supply(camp, campLayout, cx, 1, cz, 'raider_camp', facing);
    const [lx, lz] = at(middle, 2);
    lantern(camp, lx, 2 + middle, lz);
}
tent(11, 3, 7, 7, 'red_wool', 'south');
tent(3, 13, 7, 7, 'white_wool', 'east');
tent(21, 4, 7, 8, 'cyan_wool', 'west');

function watchtower(x, z) {
    for (const dx of [0, 4]) for (const dz of [0, 4]) {
        camp.fill(x + dx, 1, z + dz, x + dx, 9, z + dz, 'spruce_log', { axis: 'y' });
    }
    camp.fill(x, 6, z, x + 4, 6, z + 4, 'spruce_planks');
    for (let edge = 0; edge <= 4; edge++) {
        for (const [dx, dz] of [[edge, 0], [edge, 4], [0, edge], [4, edge]]) {
            camp.block(x + dx, 7, z + dz, 'spruce_fence', fence);
        }
    }
    camp.fill(x + 1, 1, z, x + 1, 6, z, 'spruce_log', { axis: 'y' });
    for (let y = 1; y <= 6; y++) {
        camp.block(x + 1, y, z + 1, 'ladder', { facing: 'south', waterlogged: 'false' });
    }
    camp.fill(x - 1, 10, z - 1, x + 5, 10, z + 5, 'spruce_planks');
    camp.fill(x, 11, z, x + 4, 11, z + 4, 'spruce_slab', { type: 'bottom', waterlogged: 'false' });
    camp.fill(x + 1, 12, z + 1, x + 3, 12, z + 3, 'spruce_slab', { type: 'bottom', waterlogged: 'false' });
    lantern(camp, x + 2, 9, z + 2);
    banner(camp, x + 2, 6, z + 5);
    camp.mob('raider', x + 2, 7, z + 2, 180, { FrontierArcher: byte(1) });
}
watchtower(3, 3);
watchtower(23, 23);

function shed(x1, z1, x2, z2, facing) {
    camp.fill(x1, 0, z1, x2, 0, z2, 'spruce_planks');
    camp.fill(x1, 1, z1, x2, 4, z2, 'spruce_planks');
    camp.fill(x1 + 1, 1, z1 + 1, x2 - 1, 4, z2 - 1, 'air');
    const doorX = facing === 'east' ? x2 : x1;
    camp.fill(doorX, 1, z1 + 1, doorX, 3, z2 - 1, 'air');
    for (const x of [x1, x2]) for (const z of [z1, z2]) {
        camp.fill(x, 1, z, x, 4, z, 'stripped_spruce_log', { axis: 'y' });
    }
    camp.fill(x1 - 1, 5, z1 - 1, x2 + 1, 5, z2 + 1, 'dark_oak_planks');
    lantern(camp, Math.floor((x1 + x2) / 2), 4, Math.floor((z1 + z2) / 2));
    const chestX = facing === 'east' ? x1 + 1 : x2 - 1;
    for (const z of [z1 + 1, z2 - 1]) supply(camp, campLayout, chestX, 1, z, 'raider_camp', facing);
}
shed(4, 23, 10, 27, 'east');
shed(20, 15, 27, 19, 'west');
for (const [x, z] of [[22, 16], [22, 18], [6, 25]]) camp.block(x, 1, z, 'barrel', { facing: 'up', open: 'false' });
camp.block(23, 1, 18, 'anvil', { facing: 'east' });
camp.fill(20, 1, 21, 22, 1, 22, 'hay_block', { axis: 'y' });
camp.fill(20, 2, 21, 21, 2, 21, 'hay_block', { axis: 'y' });
for (const [x, z, axis] of [[11, 15, 'z'], [19, 14, 'z'], [15, 11, 'x'], [14, 19, 'x']]) {
    camp.block(x, 1, z, 'spruce_log', { axis });
}
// A barred cooking cage keeps the outdoor fire visible without an exposed damage surface.
camp.fill(14, 0, 14, 16, 0, 16, 'bricks');
camp.block(15, 1, 15, 'campfire', { facing: 'north', lit: 'true', signal_fire: 'false', waterlogged: 'false' });
for (let x = 14; x <= 16; x++) for (let z = 14; z <= 16; z++) {
    if (x !== 15 || z !== 15) camp.fill(x, 1, z, x, 2, z, 'iron_bars', bars);
    camp.block(x, 3, z, 'iron_bars', bars);
}
for (const [x, z, yaw] of [[13, 27, 180], [17, 27, 180], [11, 20, 90], [19, 12, 270]]) {
    camp.mob('raider', x, 1, z, yaw, { FrontierArcher: byte(0) });
}

const keep = new Structure(keepLayout.size);
keep.fill(0, 1, 0, 56, 21, 56, 'air');
keep.fill(0, 0, 0, 56, 0, 56, 'cobblestone');
keep.fill(4, 0, 4, 52, 0, 53, 'stone_bricks');
function masonry(x1, y1, z1, x2, y2, z2) {
    for (let x = x1; x <= x2; x++) for (let y = y1; y <= y2; y++) for (let z = z1; z <= z2; z++) {
        const grain = (x * 31 + y * 17 + z * 13) % 19;
        keep.block(x, y, z, grain < 3 ? 'mossy_stone_bricks' : grain < 5 ? 'cracked_stone_bricks'
            : grain === 5 ? 'cobblestone' : 'stone_bricks');
    }
}
for (const [x1, z1, x2, z2] of [[1, 1, 55, 3], [1, 53, 55, 55], [1, 1, 3, 55], [53, 1, 55, 55]]) {
    masonry(x1, 1, z1, x2, 12, z2);
    keep.fill(x1, 10, z1, x2, 10, z2, 'polished_andesite');
}
for (let edge = 4; edge <= 52; edge += 3) {
    for (const [x, z] of [[edge, 2], [edge, 54], [2, edge], [54, edge]]) {
        masonry(x, 13, z, x + (z === 2 || z === 54 ? 1 : 0), 14, z + (x === 2 || x === 54 ? 1 : 0));
    }
}
for (const [x, z] of [[1, 1], [48, 1], [1, 48], [48, 48]]) {
    masonry(x, 1, z, x + 7, 18, z + 7);
    keep.fill(x + 1, 17, z + 1, x + 6, 17, z + 6, 'polished_deepslate');
    for (let edge = 0; edge <= 7; edge++) {
        if (edge % 3 === 2) continue;
        for (const [dx, dz] of [[edge, 0], [edge, 7], [0, edge], [7, edge]]) {
            masonry(x + dx, 19, z + dz, x + dx, edge % 3 === 0 ? 21 : 20, z + dz);
        }
    }
    for (let y = 6; y <= 8; y++) {
        keep.block(x + 3, y, z, 'iron_bars', bars);
        keep.block(x + 4, y, z, 'iron_bars', bars);
        keep.block(x + 3, y, z + 7, 'iron_bars', bars);
        keep.block(x + 4, y, z + 7, 'iron_bars', bars);
    }
}
for (const edge of [13, 21, 35, 43]) {
    for (const y of [5, 6, 7]) {
        for (const [x, z] of [[edge, 1], [edge, 55], [1, edge], [55, edge]]) keep.block(x, y, z, 'iron_bars', bars);
    }
}

// All passages are carved out of a continuous six-block-high stone shell, never an open courtyard.
masonry(4, 1, 16, 52, 6, 52);
masonry(23, 1, 14, 33, 6, 16);
const passages = [
    [23, 50, 33, 53], [27, 46, 29, 49], [27, 53, 29, 56],
    [15, 44, 41, 46], [39, 38, 41, 46],
    [15, 38, 41, 40], [15, 32, 17, 40],
    [15, 32, 41, 34], [39, 26, 41, 34],
    [15, 26, 41, 28], [15, 20, 17, 28],
    [15, 20, 41, 22], [39, 16, 41, 22],
    [27, 16, 41, 18], [27, 14, 29, 18], [24, 16, 26, 18],
    [5, 39, 12, 47], [13, 44, 14, 46],
    [44, 30, 51, 39], [42, 32, 43, 34],
    [5, 17, 12, 26], [13, 20, 14, 22],
    [5, 32, 14, 34], [42, 20, 50, 22],
    [15, 50, 22, 52], [34, 50, 43, 52]
];
for (const [x1, z1, x2, z2] of passages) {
    keep.fill(x1, 1, z1, x2, 5, z2, 'air');
    for (let x = x1; x <= x2; x++) for (let z = z1; z <= z2; z++) {
        if (z === 56) continue;
        keep.block(x, 0, z, (x + z) % 6 === 0 ? 'mossy_stone_bricks' : 'polished_andesite');
    }
}
// Reseal the southern parapet above the entrance and roof every room and dead end.
keep.fill(27, 6, 53, 29, 6, 55, 'stone_bricks');
for (const [x1, z1, x2, z2] of passages) {
    if (z2 === 56) continue;
    keep.fill(x1, 6, z1, x2, 6, z2, 'stone_bricks');
}
for (const [x, z] of [[28, 52], [28, 45], [40, 42], [28, 39], [16, 36], [28, 33], [40, 30],
    [28, 27], [16, 24], [28, 21], [40, 19], [31, 17], [9, 42], [47, 34], [9, 20], [8, 33], [46, 21]]) {
    keep.block(x, 6, z, 'dark_oak_planks');
    lantern(keep, x, 5, z);
}
for (const [x, z] of [[20, 45], [34, 39], [22, 33], [32, 27], [21, 21], [35, 17]]) {
    keep.fill(x, 6, z - 1, x, 6, z + 1, 'stripped_dark_oak_log', { axis: 'z' });
}
for (const [x, z] of [[5, 39], [12, 39], [44, 30], [51, 30], [5, 17], [12, 17]]) {
    keep.block(x, 4, z, 'cobweb');
}
keep.wallSign(25, 3, 50, ['Three seals, then', 'the Warlord', '', 'Wrong order resets'], 'south');
for (const [x, z] of [[24, 56], [32, 56]]) banner(keep, x, 8, z);
for (const x of [24, 32]) {
    keep.block(x, 1, 51, 'chiseled_stone_bricks');
    lantern(keep, x, 2, 51, false);
}

const offsets = (from, to, prefix) => ({
    [`${prefix}Forward`]: int(from[2] - to[2]),
    [`${prefix}Right`]: int(to[0] - from[0]),
    [`${prefix}Up`]: int(to[1] - from[1])
});
const clues = [
    ['Dawn seal', 'SUN > LEAF > WAVE', 'Press in this order', 'Mistake? Start again'],
    ['River seal', 'WAVE > SUN > LEAF', 'Press in this order', 'Mistake? Start again'],
    ['Root seal', 'LEAF > WAVE > SUN', 'Press in this order', 'Mistake? Start again']
];
for (const room of keepLayout.rooms) {
    for (const rune of room.runes) {
        keep.block(...rune.pos, `${namespace}:${rune.symbol}_rune`, { facing: 'north' }, {
            id: string(`${namespace}:rune_stone`), Bound: byte(1), Room: int(room.room),
            ...offsets(rune.pos, keepLayout.gateController, 'Gate')
        });
        keep.block(rune.pos[0], 0, rune.pos[2], 'chiseled_stone_bricks');
    }
    keep.wallSign(...room.hintPosition, clues[room.room]);
    banner(keep, room.hintPosition[0] - 2, 4, room.hintPosition[2], 'north');
    banner(keep, room.hintPosition[0] + 2, 4, room.hintPosition[2], 'north');
}

// The boss hall has eight clear blocks above its floor and exactly one portal.
masonry(17, 1, 4, 39, 10, 14);
keep.fill(18, 1, 5, 38, 8, 13, 'air');
keep.fill(18, 9, 5, 38, 9, 13, 'polished_deepslate');
keep.fill(27, 1, 14, 29, 4, 15, 'air');
keep.fill(27, 0, 5, 29, 0, 13, 'red_wool');
keep.fill(27, 1, 5, 29, 4, 5, 'polished_deepslate');
keep.block(28, 5, 5, 'chiseled_stone_bricks');
for (const [x, z] of [[20, 6], [36, 6], [20, 12], [36, 12]]) {
    keep.block(x, 1, z, 'chiseled_stone_bricks');
    lantern(keep, x, 2, z, false);
}
for (const x of [21, 35]) {
    lantern(keep, x, 8, 9);
    banner(keep, x, 5, 5);
}
for (const [x, z] of [[18, 5], [38, 5], [18, 13], [38, 13]]) keep.block(x, 6, z, 'cobweb');
keep.block(...keepLayout.gateController, `${namespace}:seal_controller`, { facing: 'north' }, {
    id: string(`${namespace}:seal_controller`), Bound: byte(1),
    ...offsets(keepLayout.gateController, keepLayout.gateOrigin, 'Door'),
    DoorWidth: int(keepLayout.gateWidth), DoorHeight: int(keepLayout.gateHeight),
    ...offsets(keepLayout.gateController, keepLayout.bossPosition, 'Boss')
});
keep.wallSign(25, 3, 16, ['Three seals endure', 'Dawn / River / Root', 'All must be lit', 'The Warlord waits'], 'south');
for (let dx = 0; dx < keepLayout.gateWidth; dx++) for (let dy = 0; dy < keepLayout.gateHeight; dy++) {
    keep.block(keepLayout.gateOrigin[0] + dx, keepLayout.gateOrigin[1] + dy, keepLayout.gateOrigin[2],
        `${namespace}:seal_barrier`);
}
for (const [x, z, facing] of [[15, 51, 'east'], [43, 51, 'west'], [6, 33, 'east'], [50, 21, 'west'],
    [6, 40, 'east'], [50, 31, 'west'], [6, 18, 'east'], [19, 8, 'east'], [37, 8, 'west']]) {
    supply(keep, keepLayout, x, 1, z, 'ruined_keep', facing);
}
keep.mob('warlord', ...keepLayout.bossPosition, 180, { FrontierSealed: byte(1), NoAI: byte(1) });
for (const [x, z, yaw] of [[28, 52, 180], [35, 45, 90], [28, 39, 270], [22, 33, 90], [28, 27, 270],
    [35, 21, 90], [9, 44, 90], [47, 33, 270], [9, 21, 90]]) {
    keep.mob('raider', x, 1, z, yaw, { FrontierArcher: byte([35, 47].includes(x) ? 1 : 0) });
}

const key = pos => pos.join(',');
const inside = (site, pos) => pos.every((value, axis) => Number.isInteger(value) && value >= 0 && value < site.size[axis]);
const getBlock = (site, pos) => site.blocks.get(key(pos));
const decoration = name => name.endsWith('_wall_sign') || name.endsWith('_wall_banner') || name === 'minecraft:ladder';
function clear(site, pos, opened = false) {
    if (!inside(site, pos)) return false;
    const block = getBlock(site, pos);
    return !block || block.name === 'minecraft:air' || decoration(block.name)
        || (opened && block.name === `${namespace}:seal_barrier`);
}
function support(site, pos) {
    const block = getBlock(site, pos);
    return inside(site, pos) && block && !clear(site, pos) && !block.name.endsWith('_slab')
        && !block.name.endsWith('_fence') && block.name !== 'minecraft:iron_bars'
        && block.name !== 'minecraft:lantern' && block.name !== 'minecraft:cobweb';
}
function walkable(site, [x, y, z], opened = false) {
    return clear(site, [x, y, z], opened) && clear(site, [x, y + 1, z], opened) && support(site, [x, y - 1, z]);
}
function flood(site, start, opened = false) {
    assert(walkable(site, start, opened), `Entrance blocked at ${start}`);
    const queue = [start], distance = new Map([[key(start), 0]]), previous = new Map();
    for (let index = 0; index < queue.length; index++) {
        const pos = queue[index], [x, y, z] = pos;
        for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
            // Check step-up/down as well as flat travel so decorative blocks cannot hide a roof bypass.
            for (const dy of [0, 1, -1]) {
                const next = [x + dx, y + dy, z + dz], id = key(next);
                if (distance.has(id) || !walkable(site, next, opened)) continue;
                if (dy === 1 && !clear(site, [x, y + 2, z], opened)) continue;
                if (dy === -1 && !clear(site, [x + dx, y + 1, z + dz], opened)) continue;
                distance.set(id, distance.get(key(pos)) + 1);
                previous.set(id, key(pos));
                queue.push(next);
            }
        }
    }
    return { distance, previous };
}
function adjacentReachable(site, reachable, pos) {
    return [[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dz]) => {
        const candidate = [pos[0] + dx, pos[1], pos[2] + dz];
        return walkable(site, candidate) && reachable.has(key(candidate))
            && Math.hypot(candidate[0] - pos[0], 1.62 - 0.5, candidate[2] - pos[2]) <= 3;
    });
}
function validateSite(site, expectedGuards, name) {
    assert(site.blocks.size === site.size.reduce((product, value) => product * value, 1), `${name}: incomplete block grid`);
    assert(site.entities.filter(entity => entity.nbt.value.id.value === `${namespace}:raider`).length === expectedGuards);
    for (const block of site.blocks.values()) {
        assert(inside(site, block.pos), `${name}: palette coordinate outside template`);
        assert(/^[a-z0-9_]+:[a-z0-9_]+$/.test(block.name), `${name}: invalid palette name ${block.name}`);
        if (block.name === 'minecraft:lantern' && block.properties.hanging === 'true') {
            assert(support(site, [block.pos[0], block.pos[1] + 1, block.pos[2]]), `${name}: unsupported hanging lantern ${block.pos}`);
        }
        if (block.name.endsWith('_wall_sign') || block.name.endsWith('_wall_banner')) {
            const behind = { north: [0, 1], south: [0, -1], east: [-1, 0], west: [1, 0] }[block.properties.facing];
            assert(support(site, [block.pos[0] + behind[0], block.pos[1], block.pos[2] + behind[1]]),
                `${name}: unsupported sign/banner ${block.pos}`);
        }
    }
    for (const entity of site.entities) {
        const pos = entity.blockPos.value.values, nbt = entity.nbt.value;
        assert(inside(site, pos), `${name}: spawn outside template`);
        assert(!Object.keys(nbt).some(field => field.startsWith('UUID')), `${name}: fixed entity UUID`);
        assert(support(site, [pos[0], pos[1] - 1, pos[2]]), `${name}: floating spawn ${pos}`);
        const height = nbt.id.value === `${namespace}:warlord` ? 3 : 2;
        for (let dy = 0; dy < height; dy++) assert(clear(site, [pos[0], pos[1] + dy, pos[2]]), `${name}: obstructed spawn ${pos}`);
    }
}
function validateKeep() {
    const closed = flood(keep, keepLayout.entrance), opened = flood(keep, keepLayout.entrance, true);
    const bossKey = key(keepLayout.bossPosition);
    assert(!closed.distance.has(bossKey), 'Boss hall reachable before opening the portcullis');
    assert(opened.distance.has(bossKey), 'Boss hall inaccessible after opening the portcullis');
    for (const room of keepLayout.rooms) for (const rune of room.runes) {
        const block = getBlock(keep, rune.pos);
        assert.equal(block.name, `${namespace}:${rune.symbol}_rune`);
        assert.equal(block.properties.facing, 'north');
        assert.equal(block.nbt.id.value, `${namespace}:rune_stone`);
        assert.equal(block.nbt.Bound.type, 1);
        assert.equal(block.nbt.Bound.value, 1);
        assert.equal(block.nbt.Room.type, 3);
        assert.equal(block.nbt.Room.value, room.room);
        assert(adjacentReachable(keep, closed.distance, rune.pos), `Unreachable rune ${room.name}: ${rune.pos}`);
        for (const [field, tag] of Object.entries(offsets(rune.pos, keepLayout.gateController, 'Gate'))) {
            assert.equal(block.nbt[field].type, 3);
            assert.equal(block.nbt[field].value, tag.value, `Incorrect rune binding ${rune.pos}`);
        }
    }
    const controller = getBlock(keep, keepLayout.gateController);
    assert.equal(controller.properties.facing, 'north');
    assert(adjacentReachable(keep, closed.distance, keepLayout.gateController), 'Controller is unreachable');
    for (const [field, tag] of Object.entries({
        ...offsets(keepLayout.gateController, keepLayout.gateOrigin, 'Door'),
        ...offsets(keepLayout.gateController, keepLayout.bossPosition, 'Boss')
    })) assert.equal(controller.nbt[field].value, tag.value);
    const barriers = [...keep.blocks.values()].filter(block => block.name === `${namespace}:seal_barrier`);
    assert.equal(barriers.length, keepLayout.gateWidth * keepLayout.gateHeight);
    for (const block of barriers) {
        assert(block.pos[0] >= 27 && block.pos[0] <= 29 && block.pos[1] >= 1 && block.pos[1] <= 4 && block.pos[2] === 14);
    }
    for (const entity of keep.entities) {
        if (entity.nbt.value.id.value === `${namespace}:raider`) {
            assert(closed.distance.has(key(entity.blockPos.value.values)), 'Guard behind the sealed gate');
        } else {
            assert.equal(entity.nbt.value.FrontierSealed.value, 1);
            assert.equal(entity.nbt.value.NoAI.value, 1);
            assert.equal(entity.nbt.value.Health.value, 160);
        }
    }
    for (let dx = -1; dx <= 1; dx++) for (let dz = -1; dz <= 1; dz++) for (let dy = 0; dy < 5; dy++) {
        assert(clear(keep, [28 + dx, 1 + dy, 8 + dz]), 'Boss has insufficient floor/roof clearance');
    }
    for (const chest of keepLayout.chests) {
        assert(adjacentReachable(keep, opened.distance, chest), `Inaccessible keep loot ${chest}`);
    }
    keepLayout.solutionLength = opened.distance.get(bossKey);
    assert(keepLayout.solutionLength > 90, `Maze route too short: ${keepLayout.solutionLength}`);
    const route = [];
    for (let cursor = bossKey; cursor; cursor = opened.previous.get(cursor)) route.push(cursor.split(',').map(Number));
    const turns = route.slice(2).filter((pos, index) => {
        const first = route[index], second = route[index + 1];
        return pos[0] - second[0] !== second[0] - first[0] || pos[2] - second[2] !== second[2] - first[2];
    }).length;
    assert(turns >= 8, `Maze path is insufficiently winding: ${turns} turns`);
    console.log(`Maze verified: ${keepLayout.solutionLength} steps, ${turns} turns; all nine runes reachable with gate sealed.`);
}
validateSite(camp, campLayout.guards, 'Camp');
validateSite(keep, keepLayout.guards, 'Keep');
assert.equal(getBlock(camp, [0, 0, 0]).name, 'minecraft:grass_block');
assert.equal(getBlock(keep, [0, 0, 0]).name, 'minecraft:cobblestone');
assert(!camp.entities.some(entity => entity.nbt.value.id.value === `${namespace}:warlord`), 'Camp must not spawn a boss');
const campReachable = flood(camp, [15, 1, 30]).distance;
for (const chest of campLayout.chests) assert(adjacentReachable(camp, campReachable, chest), `Inaccessible camp loot ${chest}`);
validateKeep();
camp.save('raider_camp');
keep.save('ruined_keep');
data('layouts/sites', { camp: campLayout, keep: keepLayout });

for (const [name, size] of [
    ['empty', [8, 8, 8]], ['arena', [64, 18, 40]], ['encounters', [128, 32, 128]], ['site_arena', [128, 30, 80]]
]) {
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
