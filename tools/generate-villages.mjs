import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { Structure, string } from './structure.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const resources = path.join(root, 'src', 'main', 'resources');
const writeJson = (name, value) => {
    const target = path.join(resources, name);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, JSON.stringify(value, null, 2) + '\n');
};
const themes = [
    { biome: 'plains', title: 'Lantern Bell Tower', wood: 'oak', roof: 'dark_oak',
        stone: 'stone_bricks', wall: 'white_terracotta', ground: 'grass_block', path: 'cobblestone',
        accent: 'green', crystal: 'amethyst_block', crop: 'wheat', cropAge: '7', landmark: 'tower',
        normalWeight: 200, abandoned: [['plains_fountain_01', 1], ['plains_meeting_point_1', 1],
            ['plains_meeting_point_2', 1], ['plains_meeting_point_3', 1]] },
    { biome: 'desert', title: 'Oasis Sun Obelisk', wood: 'jungle', roof: 'sandstone',
        stone: 'smooth_sandstone', wall: 'cut_sandstone', ground: 'sand', path: 'smooth_sandstone',
        accent: 'orange', crystal: 'yellow_stained_glass', crop: 'carrots', cropAge: '7', landmark: 'obelisk',
        normalWeight: 245, abandoned: [['desert_meeting_point_1', 2], ['desert_meeting_point_2', 2],
            ['desert_meeting_point_3', 1]] },
    { biome: 'savanna', title: 'Amber Wind Spire', wood: 'acacia', roof: 'acacia',
        stone: 'terracotta', wall: 'orange_terracotta', ground: 'grass_block', path: 'coarse_dirt',
        accent: 'yellow', crystal: 'orange_stained_glass', crop: 'beetroots', cropAge: '3', landmark: 'spire',
        normalWeight: 450, abandoned: [['savanna_meeting_point_1', 2], ['savanna_meeting_point_2', 1],
            ['savanna_meeting_point_3', 3], ['savanna_meeting_point_4', 3]] },
    { biome: 'taiga', title: 'Forest Rune Shrine', wood: 'spruce', roof: 'spruce',
        stone: 'mossy_stone_bricks', wall: 'spruce_planks', ground: 'podzol', path: 'gravel',
        accent: 'cyan', crystal: 'amethyst_block', crop: 'potatoes', cropAge: '7', landmark: 'shrine',
        normalWeight: 98, abandoned: [['taiga_meeting_point_1', 1], ['taiga_meeting_point_2', 1]] },
    { biome: 'snowy', title: 'Frost Lantern Beacon', wood: 'spruce', roof: 'dark_oak',
        stone: 'stone_bricks', wall: 'stripped_spruce_log', ground: 'snow_block', path: 'stone_bricks',
        accent: 'light_blue', crystal: 'packed_ice', crop: 'wheat', cropAge: '7', landmark: 'beacon',
        normalWeight: 300, abandoned: [['snowy_meeting_point_1', 2], ['snowy_meeting_point_2', 1],
            ['snowy_meeting_point_3', 3]] }
];
const layouts = [];
const lantern = (site, x, y, z, hanging = false) => site.block(x, y, z, 'lantern', {
    hanging: String(hanging), waterlogged: 'false'
});
const fenceState = { north: 'true', south: 'true', east: 'true', west: 'true', waterlogged: 'false' };
const stairs = facing => ({ facing, half: 'bottom', shape: 'straight', waterlogged: 'false' });
const jigsaw = (site, pos, orientation, pool, name, target, finalState, joint = 'aligned') => {
    site.block(...pos, 'jigsaw', { orientation }, {
        id: string('minecraft:jigsaw'), pool: string(pool), name: string(`minecraft:${name}`),
        target: string(`minecraft:${target}`), final_state: string(`minecraft:${finalState}`), joint: string(joint)
    });
};

function defenses(site, theme, layout) {
    for (let edge = 1; edge <= 39; edge++) {
        if (edge >= 18 && edge <= 22) continue;
        for (const [x, z] of [[edge, 1], [edge, 39], [1, edge], [39, edge]]) {
            site.block(x, 1, z, theme.stone);
            site.fill(x, 2, z, x, 3, z, `${theme.wood}_fence`, fenceState);
            if (edge % 4 === 1) {
                site.fill(x, 1, z, x, 4, z, `${theme.wood}_log`, { axis: 'y' });
                lantern(site, x, 5, z);
            }
        }
    }
    for (const [x, z, alongX] of [[17, 1, true], [17, 39, true], [1, 17, false], [39, 17, false]]) {
        const otherX = x + (alongX ? 6 : 0);
        const otherZ = z + (alongX ? 0 : 6);
        for (const [px, pz] of [[x, z], [otherX, otherZ]]) {
            site.fill(px, 1, pz, px, 5, pz, theme.stone);
            lantern(site, px, 6, pz);
        }
        site.fill(x, 5, z, otherX, 5, otherZ, `${theme.wood}_log`, { axis: alongX ? 'x' : 'z' });
    }
    for (const [x, z] of [[3, 3], [33, 33]]) {
        for (const dx of [0, 4]) for (const dz of [0, 4]) {
            site.fill(x + dx, 1, z + dz, x + dx, 9, z + dz, `${theme.wood}_log`, { axis: 'y' });
        }
        site.fill(x, 6, z, x + 4, 6, z + 4, `${theme.wood}_planks`);
        for (let edge = 0; edge <= 4; edge++) {
            for (const [dx, dz] of [[edge, 0], [edge, 4], [0, edge], [4, edge]]) {
                site.block(x + dx, 7, z + dz, `${theme.wood}_fence`, fenceState);
            }
        }
        site.fill(x + 1, 1, z, x + 1, 6, z, `${theme.wood}_log`, { axis: 'y' });
        site.block(x + 1, 6, z + 1, 'air');
        for (let y = 1; y <= 6; y++) {
            site.block(x + 1, y, z + 1, 'ladder', { facing: 'south', waterlogged: 'false' });
        }
        site.fill(x - 1, 10, z - 1, x + 5, 10, z + 5, `${theme.wood}_planks`);
        site.fill(x, 11, z, x + 4, 11, z + 4, `${theme.wood}_slab`,
            { type: theme.biome === 'snowy' ? 'top' : 'bottom', waterlogged: 'false' });
        if (theme.biome === 'snowy') site.fill(x, 12, z, x + 4, 12, z + 4, 'snow', { layers: '1' });
        lantern(site, x + 2, 9, z + 2, true);
        layout.towers.push([x + 2, 6, z + 2]);
        layout.ladders.push([x + 1, 1, z + 1]);
    }
}

function cottage(site, theme, layout, x, z, facing) {
    site.fill(x, 0, z, x + 8, 0, z + 6, `${theme.wood}_planks`);
    site.fill(x, 1, z, x + 8, 4, z + 6, theme.wall,
        theme.wall.endsWith('_log') ? { axis: 'y' } : {});
    site.fill(x + 1, 1, z + 1, x + 7, 4, z + 5, 'air');
    for (const dx of [0, 8]) for (const dz of [0, 6]) {
        site.fill(x + dx, 1, z + dz, x + dx, 4, z + dz, `${theme.wood}_log`, { axis: 'y' });
    }
    for (const dx of [2, 6]) for (const dz of [0, 6]) {
        site.block(x + dx, 2, z + dz, 'glass_pane',
            { north: 'false', south: 'false', east: 'true', west: 'true', waterlogged: 'false' });
    }
    const doorZ = z + (facing === 'south' ? 6 : 0);
    for (const half of ['lower', 'upper']) {
        site.block(x + 4, half === 'lower' ? 1 : 2, doorZ, `${theme.wood}_door`,
            { facing, half, hinge: 'left', open: 'false', powered: 'false' });
    }
    layout.doors.push([x + 4, 1, doorZ]);
    site.fill(x + 4, 0, facing === 'south' ? doorZ + 1 : 20,
        x + 4, 0, facing === 'south' ? 20 : doorZ - 1, theme.path);
    if (theme.biome === 'desert') {
        site.fill(x - 1, 5, z - 1, x + 9, 5, z + 7, 'smooth_sandstone');
        for (let dx = 0; dx <= 8; dx += 2) {
            for (const dz of [0, 6]) site.block(x + dx, 6, z + dz, 'cut_sandstone');
        }
    } else if (theme.biome === 'savanna') {
        site.fill(x - 1, 5, z - 1, x + 9, 5, z + 7, 'acacia_planks');
        site.fill(x, 6, z, x + 8, 6, z + 6, 'acacia_slab', { type: 'bottom', waterlogged: 'false' });
        site.fill(x + 2, 7, z + 2, x + 6, 7, z + 4, 'acacia_slab', { type: 'bottom', waterlogged: 'false' });
    } else {
        for (let layer = 0; layer < 5; layer++) {
            const left = x - 1 + layer;
            const right = x + 9 - layer;
            for (let rz = z - 1; rz <= z + 7; rz++) {
                site.block(left, 5 + layer, rz, `${theme.roof}_stairs`, stairs('east'));
                site.block(right, 5 + layer, rz, `${theme.roof}_stairs`, stairs('west'));
            }
            if (layer < 4) {
                site.fill(left + 1, 5 + layer, z, right - 1, 5 + layer, z, `${theme.wood}_planks`);
                site.fill(left + 1, 5 + layer, z + 6, right - 1, 5 + layer, z + 6, `${theme.wood}_planks`);
            }
        }
        site.fill(x + 4, 10, z - 1, x + 4, 10, z + 7, `${theme.roof}_slab`,
            { type: theme.biome === 'snowy' ? 'top' : 'bottom', waterlogged: 'false' });
        if (theme.biome === 'snowy') {
            site.fill(x + 4, 11, z, x + 4, 11, z + 6, 'snow', { layers: '1' });
        }
        site.fill(x + 7, 5, z + 2, x + 7, 10, z + 2, theme.stone);
        site.block(x + 7, 11, z + 2, 'flower_pot');
    }
    for (const dx of [2, 6]) {
        site.block(x + dx, 1, z + 3, `${theme.accent}_bed`,
            { facing: 'north', part: 'foot', occupied: 'false' });
        site.block(x + dx, 1, z + 2, `${theme.accent}_bed`,
            { facing: 'north', part: 'head', occupied: 'false' });
        layout.beds.push([x + dx, 1, z + 2]);
    }
    site.chest(x + 1, 1, z + 4, `minecraft:chests/village/village_${theme.biome}_house`);
    layout.chests.push([x + 1, 1, z + 4]);
    site.block(x + 7, 1, z + 4, 'crafting_table');
    site.fill(x + 1, 4, z + 3, x + 7, 4, z + 3, `${theme.wood}_planks`);
    lantern(site, x + 4, 3, z + 3, true);
}

function farm(site, theme, layout, x, z) {
    for (let dx = 0; dx < 7; dx++) for (let dz = 0; dz < 5; dz++) {
        if (dx === 3) {
            site.block(x + dx, 0, z + dz, 'water', { level: '0' });
        } else {
            site.block(x + dx, 0, z + dz, 'farmland', { moisture: '7' });
            site.block(x + dx, 1, z + dz, theme.crop, { age: theme.cropAge });
        }
    }
    site.block(x + 3, 0, z + 2, `${theme.wood}_planks`);
    site.block(x + 3, 1, z + 2, 'composter', { level: '0' });
    layout.jobs.push([x + 3, 1, z + 2]);
    layout.farms.push([x, 0, z]);
    for (const dx of [-1, 7]) for (const dz of [-1, 5]) {
        site.fill(x + dx, 1, z + dz, x + dx, 3, z + dz, `${theme.wood}_fence`, fenceState);
        lantern(site, x + dx, 4, z + dz);
    }
    if (theme.biome === 'snowy') {
        site.fill(x - 1, 4, z - 1, x + 7, 4, z + 5, 'glass');
        for (const dz of [0, 4]) {
            site.block(x + 3, 2, z + dz, 'glowstone');
        }
    }
}

function market(site, theme, layout, x, z, job) {
    for (const dx of [0, 4]) for (const dz of [0, 4]) {
        site.fill(x + dx, 1, z + dz, x + dx, 3, z + dz, `${theme.wood}_fence`, fenceState);
    }
    for (let dx = 0; dx <= 4; dx++) for (let dz = 0; dz <= 4; dz++) {
        site.block(x + dx, 4, z + dz, dx % 2 ? `${theme.accent}_wool` : 'white_wool');
    }
    site.block(x + 1, 1, z + 1, 'barrel', { facing: 'south', open: 'false' });
    site.block(x + 2, 1, z + 1, job,
        job === 'grindstone' ? { face: 'floor', facing: 'south' } : {});
    layout.jobs.push([x + 2, 1, z + 1]);
    lantern(site, x + 2, 3, z + 2, true);
}

function landmark(site, theme, layout) {
    site.fill(18, 0, 18, 22, 0, 22, theme.stone);
    for (let edge = 18; edge <= 22; edge++) {
        for (const [x, z] of [[edge, 18], [edge, 22], [18, edge], [22, edge]]) {
            site.block(x, 1, z, theme.stone);
        }
    }
    site.fill(19, 1, 19, 21, 1, 21, 'water', { level: '0' });
    site.fill(20, 1, 20, 20, 10, 20, theme.stone);
    site.fill(19, 4, 18, 21, 4, 20, `${theme.wood}_planks`);
    site.block(20, 3, 18, 'bell', { facing: 'north', attachment: 'ceiling', powered: 'false' });
    layout.bell = [20, 3, 18];
    if (theme.landmark === 'tower') {
        for (const x of [19, 21]) for (const z of [19, 21]) {
            site.fill(x, 5, z, x, 9, z, `${theme.wood}_fence`, fenceState);
            lantern(site, x, 10, z);
        }
        site.fill(18, 11, 18, 22, 11, 22, `${theme.roof}_planks`);
        site.fill(19, 12, 19, 21, 12, 21, `${theme.roof}_slab`, { type: 'bottom', waterlogged: 'false' });
    } else if (theme.landmark === 'obelisk') {
        site.fill(19, 2, 19, 21, 3, 21, 'chiseled_sandstone');
        site.fill(20, 4, 20, 20, 11, 20, 'cut_sandstone');
        site.block(20, 12, 20, 'gold_block');
        for (const x of [19, 21]) site.block(x, 11, 20, theme.crystal);
    } else if (theme.landmark === 'spire') {
        site.fill(20, 5, 20, 20, 11, 20, 'acacia_log', { axis: 'y' });
        site.fill(17, 9, 20, 23, 9, 20, 'acacia_log', { axis: 'x' });
        for (let arm = 1; arm <= 3; arm++) {
            site.block(20 - arm, 9 + (arm % 2), 21, 'yellow_wool');
            site.block(20 + arm, 9 - (arm % 2), 19, 'white_wool');
        }
        site.block(20, 12, 20, theme.crystal);
    } else if (theme.landmark === 'shrine') {
        for (const x of [18, 22]) site.fill(x, 2, 20, x, 6, 20, 'mossy_cobblestone');
        site.fill(18, 7, 20, 22, 7, 20, 'mossy_stone_bricks');
        site.block(18, 7, 19, 'spruce_planks');
        site.block(22, 7, 19, 'spruce_planks');
        site.block(20, 8, 19, 'chiseled_stone_bricks');
        site.fill(19, 10, 19, 21, 10, 21, 'spruce_planks');
        site.block(20, 11, 20, theme.crystal);
        lantern(site, 18, 6, 19, true);
        lantern(site, 22, 6, 19, true);
    } else {
        site.fill(20, 5, 20, 20, 10, 20, 'blue_ice');
        for (const x of [19, 21]) for (const z of [19, 21]) {
            site.fill(x, 6, z, x, 10, z, theme.crystal);
        }
        site.fill(19, 11, 19, 21, 11, 21, 'sea_lantern');
        site.block(20, 12, 20, 'snow_block');
    }
    if (theme.landmark !== 'tower') {
        for (const x of [19, 21]) lantern(site, x, 5, 18);
    }
    layout.landmark = [20, 10, 20];
}

function validate(site, theme, layout) {
    const at = (x, y, z) => site.blocks.get(`${x},${y},${z}`);
    const passable = block => block && (block.name === 'minecraft:air' || block.name.endsWith('_door')
        || block.name === 'minecraft:ladder' || block.name === `minecraft:${theme.crop}`
        || (block.name === 'minecraft:jigsaw' && block.nbt.final_state.value === 'minecraft:air'));
    const walkable = (x, z) => passable(at(x, 1, z)) && passable(at(x, 2, z))
        && at(x, 0, z)?.name !== 'minecraft:water';
    const seen = new Set();
    const queue = [[20, 0]];
    for (let index = 0; index < queue.length; index++) {
        const [x, z] = queue[index];
        const key = `${x},${z}`;
        if (seen.has(key) || !walkable(x, z)) continue;
        seen.add(key);
        for (const [dx, dz] of [[-1, 0], [1, 0], [0, -1], [0, 1]]) queue.push([x + dx, z + dz]);
    }
    const accessible = pos => [[-1, 0], [1, 0], [0, -1], [0, 1]]
        .some(([dx, dz]) => seen.has(`${pos[0] + dx},${pos[2] + dz}`));
    for (const pos of [...layout.roads, ...layout.doors, ...layout.ladders]) {
        assert(seen.has(`${pos[0]},${pos[2]}`), `${theme.biome}: disconnected entrance ${pos}`);
    }
    for (const pos of [...layout.beds, ...layout.jobs, ...layout.chests]) {
        assert(accessible(pos), `${theme.biome}: inaccessible village facility ${pos}`);
    }
    for (const pos of layout.residents) {
        assert(seen.has(`${pos[0]},${pos[2]}`) && passable(at(pos[0], 3, pos[2])),
            `${theme.biome}: obstructed vanilla inhabitant ${pos}`);
    }
    assert.equal(layout.beds.length, 4);
    assert.equal(layout.farms.length, 2);
    assert.equal(layout.towers.length, 2);
    assert.equal(layout.jobs.length, 6);
    assert.equal(layout.roads.length, 4);
    assert.equal(at(...layout.bell).name, 'minecraft:bell');
    for (const block of site.blocks.values()) {
        assert(block.name.startsWith('minecraft:'), 'Village blocks must remain vanilla');
    }
}

for (const theme of themes) {
    const layout = { biome: theme.biome, title: theme.title, size: [41, 16, 41], ground: theme.ground,
        stone: theme.stone, wood: theme.wood, beds: [], doors: [], chests: [], farms: [], jobs: [],
        towers: [], ladders: [], roads: [[20, 1, 0], [20, 1, 40], [0, 1, 20], [40, 1, 20]],
        residents: [[31, 0, 8], [9, 0, 34], [17, 0, 13], [23, 0, 27], [23, 0, 20], [17, 0, 20]] };
    const site = new Structure(layout.size);
    site.fill(0, 1, 0, 40, 15, 40, 'air');
    site.fill(0, 0, 0, 40, 0, 40, theme.ground);
    site.fill(11, 0, 11, 29, 0, 29, theme.path);
    site.fill(18, 0, 0, 22, 0, 40, theme.path);
    site.fill(0, 0, 18, 40, 0, 22, theme.path);
    defenses(site, theme, layout);
    cottage(site, theme, layout, 27, 4, 'south');
    cottage(site, theme, layout, 5, 30, 'north');
    farm(site, theme, layout, 4, 12);
    farm(site, theme, layout, 30, 24);
    for (const [x, z, job] of [[12, 12, 'composter'], [24, 12, 'fletching_table'],
        [12, 24, 'cartography_table'], [24, 24, 'grindstone']]) {
        market(site, theme, layout, x, z, job);
    }
    landmark(site, theme, layout);
    for (const [index, direction] of ['north', 'south', 'west', 'east'].entries()) {
        jigsaw(site, layout.roads[index], `${direction}_up`, `minecraft:village/${theme.biome}/streets`,
            'street', 'street', 'air');
    }
    for (let index = 0; index < layout.residents.length; index++) {
        const pool = index < 4 ? `minecraft:village/${theme.biome}/villagers`
            : `minecraft:village/common/${index === 4 ? 'iron_golem' : 'cats'}`;
        jigsaw(site, layout.residents[index], 'up_north', pool, 'bottom', 'bottom',
            index < 2 ? `${theme.wood}_planks` : theme.path, 'rollable');
    }
    if (theme.biome === 'desert') {
        layout.residents.push([25, 0, 20]);
        jigsaw(site, layout.residents.at(-1), 'up_south', 'minecraft:village/desert/camel',
            'empty', 'empty', theme.path);
    }
    validate(site, theme, layout);
    site.save(`village/${theme.biome}/frontier_center`);
    const frontierElement = {
        element_type: 'minecraft:single_pool_element', location: `livingfrontier:village/${theme.biome}/frontier_center`,
        processors: 'minecraft:empty', projection: 'rigid'
    };
    const normalElements = [];
    for (let remaining = theme.normalWeight; remaining > 0; remaining -= 150) {
        normalElements.push({ weight: Math.min(150, remaining), element: frontierElement });
    }
    const elements = [...normalElements, ...theme.abandoned.map(([name, weight]) => ({ weight, element: {
        element_type: 'minecraft:legacy_single_pool_element',
        location: `minecraft:village/${theme.biome}/zombie/town_centers/${name}`,
        processors: theme.biome === 'snowy' ? 'minecraft:empty' : `minecraft:zombie_${theme.biome}`,
        projection: 'rigid'
    } }))];
    assert(elements.every(({ weight }) => Number.isInteger(weight) && weight >= 1 && weight <= 150),
        'Village pool weights must fit the Minecraft codec');
    writeJson(`data/minecraft/worldgen/template_pool/village/${theme.biome}/town_centers.json`,
        { fallback: 'minecraft:empty', elements });
    layouts.push(layout);
}
writeJson('data/livingfrontier/layouts/villages.json', { villages: layouts });
new Structure([224, 24, 44]).save('test/village_arena', path.join(root, 'src', 'gametest', 'resources'));
