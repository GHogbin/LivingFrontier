import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';

const resources = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', 'src', 'main', 'resources');
const typed = (type, value) => ({ type, value });
export const int = value => typed(3, value);
export const byte = value => typed(1, value);
export const float = value => typed(5, value);
export const double = value => typed(6, value);
export const string = value => typed(8, value);
export const list = (type, values) => typed(9, { type, values });
export const compound = value => typed(10, value);
export const numbers = (type, values) => list(type, values);
const shortString = text => {
    const contents = Buffer.from(text, 'utf8');
    const size = Buffer.alloc(2);
    size.writeUInt16BE(contents.length);
    return Buffer.concat([size, contents]);
};
function payload(type, value) {
    if (type === 8) return shortString(value);
    if (type === 10) return Buffer.concat([
        ...Object.entries(value).map(([name, tag]) => Buffer.concat([
            Buffer.from([tag.type]), shortString(name), payload(tag.type, tag.value)
        ])), Buffer.from([0])
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

export function writeStructure(file, size, palette, blocks, entities = []) {
    const body = {
        DataVersion: int(3465), size: numbers(3, size), palette: list(10, palette),
        blocks: list(10, blocks), entities: list(10, entities)
    };
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, zlib.gzipSync(Buffer.concat([Buffer.from([10, 0, 0]), payload(10, body)])));
}

export class Structure {
    constructor(size) {
        this.size = size;
        this.blocks = new Map();
        this.entities = [];
    }
    block(x, y, z, name, properties = {}, nbt = null) {
        if ([x, y, z].some((coord, axis) => !Number.isInteger(coord) || coord < 0 || coord >= this.size[axis])) {
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
            id: string('minecraft:chest'),
            LootTable: string(loot.includes(':') ? loot : `livingfrontier:chests/${loot}`)
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
                id: string(name.includes(':') ? name : `livingfrontier:${name}`),
                Pos: numbers(6, [x + 0.5, y, z + 0.5]), Rotation: numbers(5, [yaw, 0]),
                Motion: numbers(6, [0, 0, 0]), PersistenceRequired: byte(1),
                Health: float(name === 'warlord' ? 160 : 26), ...extraNbt
            })
        });
    }
    save(name, resourceRoot = resources) {
        const palette = [];
        const states = new Map();
        const blocks = [...this.blocks.values()].map(block => {
            const key = JSON.stringify([block.name, block.properties]);
            if (!states.has(key)) {
                states.set(key, palette.length);
                const state = { Name: string(block.name) };
                if (Object.keys(block.properties).length) {
                    state.Properties = compound(Object.fromEntries(Object.entries(block.properties)
                        .map(([name, value]) => [name, string(value)])));
                }
                palette.push(state);
            }
            const result = { pos: numbers(3, block.pos), state: int(states.get(key)) };
            if (block.nbt) result.nbt = compound(block.nbt);
            return result;
        });
        writeStructure(path.join(resourceRoot, 'data', 'livingfrontier', 'structures', `${name}.nbt`),
            this.size, palette, blocks, this.entities);
        console.log(`${name}: ${blocks.length} blocks, ${this.entities.length} inhabitants`);
    }
}
