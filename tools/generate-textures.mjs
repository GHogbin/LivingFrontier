import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deflateSync } from 'node:zlib';

const width = 128;
const height = 64;
const output = fileURLToPath(new URL('../src/main/resources/assets/livingfrontier/textures/entity/', import.meta.url));

function crc32(bytes) {
    let crc = 0xffffffff;
    for (const byte of bytes) {
        crc ^= byte;
        for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
    }
    return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
    const label = Buffer.from(type, 'ascii');
    const result = Buffer.alloc(data.length + 12);
    result.writeUInt32BE(data.length);
    label.copy(result, 4);
    data.copy(result, 8);
    result.writeUInt32BE(crc32(Buffer.concat([label, data])), result.length - 4);
    return result;
}

function encodePng(pixels) {
    const header = Buffer.alloc(13);
    header.writeUInt32BE(width, 0);
    header.writeUInt32BE(height, 4);
    header[8] = 8;
    header[9] = 6;
    const scanlines = Buffer.alloc(height * (width * 4 + 1));
    for (let y = 0; y < height; y++) {
        pixels.copy(scanlines, y * (width * 4 + 1) + 1, y * width * 4, (y + 1) * width * 4);
    }
    return Buffer.concat([
        Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
        chunk('IHDR', header),
        chunk('IDAT', deflateSync(scanlines, { level: 9 })),
        chunk('IEND', Buffer.alloc(0)),
    ]);
}

function atlas(seed) {
    const pixels = Buffer.alloc(width * height * 4);
    const clamp = value => Math.max(0, Math.min(255, Math.round(value)));
    function pixel(x, y, color, shade = 0) {
        if (x < 0 || y < 0 || x >= width || y >= height) throw new Error(`Pixel outside atlas: ${x},${y}`);
        const index = (y * width + x) * 4;
        pixels[index] = clamp(color[0] + shade);
        pixels[index + 1] = clamp(color[1] + shade);
        pixels[index + 2] = clamp(color[2] + shade);
        pixels[index + 3] = 255;
    }
    function panel(x, y, w, h, color, style = 'plain') {
        if (x + w > width || y + h > height) throw new Error('UV panel outside atlas');
        for (let dy = 0; dy < h; dy++) {
            for (let dx = 0; dx < w; dx++) {
                const hash = (Math.imul(x + dx + 17, 374761393)
                    ^ Math.imul(y + dy + seed, 668265263)) >>> 0;
                let shade = (hash % 7) - 3;
                if (style === 'fur' && (dx + dy * 3) % 13 === 0) shade -= 10;
                if (style === 'feathers') {
                    if (dy % 4 === 0) shade -= 24;
                    if ((dx + Math.floor(dy / 4) * 2) % 7 === 0) shade += 15;
                }
                if (style === 'metal') {
                    if (dy % 7 === 0) shade -= 19;
                    if (dx % 9 === 1 && dy % 7 === 1) shade += 34;
                    if ((dx * 2 + dy) % 29 === 0) shade += 17;
                }
                if (style === 'cloth') {
                    if (dx % 5 === 0) shade -= 8;
                    if (dy % 9 === 0) shade += 7;
                }
                if (style === 'wood' && dx % 3 === 0) shade -= 17;
                if (style === 'bone' && dx % 4 === 0) shade -= 12;
                if (style === 'veins' && (dx % 4 === 0 || (dx + dy) % 7 === 0)) shade -= 32;
                if (style === 'glow') shade += (dy % 6 < 3 ? 15 : -13);
                pixel(x + dx, y + dy, color, shade);
            }
        }
    }
    panel(0, 0, width, height, [26, 26, 29]);
    return { pixels, panel, pixel };
}

function deer() {
    const image = atlas(19);
    const { panel, pixel } = image;
    panel(0, 0, 64, 32, [139, 90, 51], 'fur');
    panel(64, 0, 32, 24, [165, 112, 66], 'fur');
    panel(96, 0, 32, 24, [198, 167, 112], 'fur');
    panel(0, 32, 16, 32, [121, 77, 43], 'fur');
    panel(16, 32, 16, 32, [209, 187, 137], 'bone');
    panel(32, 32, 16, 16, [31, 26, 22]);
    panel(64, 24, 48, 24, [221, 203, 154], 'fur');
    panel(48, 48, 16, 16, [184, 132, 102], 'fur');
    panel(64, 48, 24, 16, [231, 216, 177], 'fur');
    panel(32, 48, 16, 16, [47, 34, 26]);
    // The body net has depth 16: flank pixels live at y=16..22.
    for (const x of [18, 23, 31, 37, 41]) {
        for (const y of [18, 21]) {
            pixel(x, y, [217, 180, 119]);
            pixel(x + 1, y, [197, 154, 93]);
        }
    }
    return image.pixels;
}

function songbird() {
    const { pixels, panel } = atlas(41);
    panel(0, 0, 64, 32, [93, 106, 106], 'feathers');
    panel(64, 0, 32, 24, [102, 121, 124], 'feathers');
    panel(0, 32, 32, 32, [66, 82, 91], 'feathers');
    panel(32, 32, 16, 16, [124, 83, 44]);
    panel(48, 32, 16, 16, [220, 164, 62]);
    panel(32, 48, 16, 16, [17, 23, 25]);
    panel(64, 24, 48, 24, [197, 68, 42], 'feathers');
    panel(64, 48, 24, 16, [54, 74, 84], 'feathers');
    // Pale cheek bands on the two side faces of the 4x4x4 head net.
    panel(64, 6, 4, 1, [198, 205, 187]);
    panel(72, 6, 4, 1, [198, 205, 187]);
    return pixels;
}

function firefly() {
    const { pixels, panel } = atlas(67);
    panel(0, 0, 64, 32, [55, 76, 55], 'metal');
    panel(64, 0, 32, 24, [37, 47, 40]);
    panel(0, 32, 32, 32, [163, 192, 165], 'veins');
    panel(32, 32, 16, 32, [50, 58, 38]);
    panel(48, 32, 16, 16, [227, 189, 80]);
    panel(64, 24, 48, 24, [192, 231, 70], 'glow');
    return pixels;
}

function raider(warlord) {
    const { pixels, panel } = atlas(warlord ? 103 : 89);
    panel(0, 0, 64, 32, warlord ? [56, 65, 79] : [94, 111, 111], 'metal');
    panel(64, 0, 32, 24, warlord ? [142, 128, 118] : [160, 146, 128], 'fur');
    panel(96, 0, 32, 24, warlord ? [83, 34, 43] : [38, 61, 65], 'cloth');
    panel(0, 32, 16, 32, warlord ? [83, 37, 46] : [53, 70, 73], 'cloth');
    panel(16, 32, 16, 32, [220, 203, 159], 'bone');
    panel(32, 32, 16, 32, [35, 34, 40], 'metal');
    panel(48, 32, 16, 32, [110, 73, 43], 'wood');
    panel(64, 24, 32, 24, warlord ? [125, 42, 46] : [103, 75, 52], 'cloth');
    panel(96, 24, 32, 24, [193, 146, 58], 'metal');
    panel(64, 48, 24, 16, warlord ? [134, 155, 170] : [173, 191, 191], 'metal');
    panel(112, 48, 16, 16, warlord ? [251, 142, 58] : [225, 189, 97]);
    // Torso's front face occupies (4,4)..(11,14); add a riveted harness.
    panel(5, 4, 1, 10, [51, 39, 31], 'wood');
    panel(10, 4, 1, 10, [51, 39, 31], 'wood');
    panel(7, 7, 2, 2, [194, 154, 74], 'metal');
    return pixels;
}

mkdirSync(output, { recursive: true });
const textures = {
    deer: deer(),
    songbird: songbird(),
    firefly: firefly(),
    raider: raider(false),
    warlord: raider(true),
};
for (const [name, pixels] of Object.entries(textures)) {
    const path = join(output, `${name}.png`);
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, encodePng(pixels));
    console.log(`${name}.png: original ${width}x${height} RGBA atlas`);
}
