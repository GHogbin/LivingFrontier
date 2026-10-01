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

// Paint the complete cuboid net, including both caps and every side face.
function boxPanel(image, u, v, w, h, d, color, style = 'plain') {
    const tint = amount => color.map(channel => channel + amount);
    image.panel(u, v, 2 * (w + d), h + d, color, style);
    image.panel(u + d, v, w, d, tint(12), style);
    image.panel(u + d + w, v, w, d, tint(-12), style);
    image.panel(u, v + d, d, h, tint(-5), style);
    image.panel(u + d + w, v + d, d, h, tint(5), style);
}

function boar() {
    const image = atlas(127);
    const { panel, pixel } = image;
    boxPanel(image, 0, 0, 10, 8, 16, [108, 73, 48], 'fur');
    boxPanel(image, 0, 24, 11, 6, 7, [83, 55, 39], 'fur');
    boxPanel(image, 36, 24, 6, 1, 12, [146, 108, 78], 'fur');
    boxPanel(image, 64, 0, 8, 7, 6, [116, 78, 51], 'fur');
    boxPanel(image, 92, 0, 6, 4, 5, [151, 107, 81], 'fur');
    boxPanel(image, 92, 10, 6, 3, 1, [80, 51, 43]);
    boxPanel(image, 72, 24, 1, 4, 1, [226, 209, 163], 'bone');
    boxPanel(image, 80, 24, 3, 3, 1, [100, 63, 44], 'fur');
    boxPanel(image, 88, 24, 2, 2, 1, [167, 109, 94]);
    boxPanel(image, 0, 40, 3, 4, 3, [94, 61, 41], 'fur');
    boxPanel(image, 12, 40, 3, 2, 3, [39, 32, 30]);
    boxPanel(image, 24, 40, 1, 1, 4, [106, 69, 47], 'fur');
    boxPanel(image, 34, 40, 1, 2, 2, [47, 38, 32], 'fur');
    boxPanel(image, 0, 50, 2, 2, 10, [57, 42, 32], 'fur');
    boxPanel(image, 24, 52, 1, 3, 2, [45, 35, 29], 'fur');
    panel(108, 12, 4, 3, [28, 22, 19]);
    panel(112, 12, 4, 3, [228, 166, 78]);
    // Snout front face and the cloven front/back faces of each hoof.
    panel(94, 11, 1, 1, [38, 26, 26]);
    panel(97, 11, 1, 1, [38, 26, 26]);
    panel(16, 43, 1, 2, [18, 18, 19]);
    panel(22, 43, 1, 2, [18, 18, 19]);
    // Short coarse bristle streaks, restricted to the two flank faces.
    for (const origin of [0, 26]) {
        for (let x = 2; x < 15; x += 4) {
            pixel(origin + x, 18, [130, 94, 61]);
            pixel(origin + x, 19, [88, 59, 41]);
            pixel(origin + x + 1, 21, [131, 94, 62]);
        }
    }
    return image.pixels;
}

function prowler() {
    const image = atlas(149);
    const { panel, pixel } = image;
    boxPanel(image, 0, 0, 7, 5, 14, [44, 53, 67], 'fur');
    boxPanel(image, 0, 20, 8, 6, 6, [34, 41, 54], 'fur');
    boxPanel(image, 28, 20, 5, 1, 11, [82, 93, 106], 'fur');
    boxPanel(image, 64, 0, 7, 5, 6, [49, 60, 76], 'fur');
    boxPanel(image, 90, 0, 4, 2, 4, [86, 94, 108], 'fur');
    boxPanel(image, 106, 0, 4, 1, 1, [18, 23, 30]);
    boxPanel(image, 90, 8, 4, 1, 4, [38, 39, 49]);
    boxPanel(image, 64, 16, 2, 4, 2, [35, 43, 58], 'fur');
    boxPanel(image, 72, 16, 1, 3, 1, [119, 77, 77]);
    boxPanel(image, 80, 16, 2, 2, 3, [59, 69, 86], 'fur');
    boxPanel(image, 90, 16, 1, 1, 3, [21, 27, 36], 'fur');
    boxPanel(image, 0, 36, 2, 6, 2, [42, 49, 62], 'fur');
    boxPanel(image, 8, 36, 2, 2, 3, [25, 31, 42], 'fur');
    boxPanel(image, 24, 36, 1, 2, 1, [236, 223, 188], 'bone');
    panel(28, 36, 8, 4, [15, 19, 24]);
    panel(36, 36, 8, 4, [245, 181, 53]);
    boxPanel(image, 0, 48, 2, 2, 8, [45, 53, 68], 'fur');
    boxPanel(image, 20, 48, 2, 2, 6, [23, 29, 39], 'fur');
    boxPanel(image, 40, 48, 2, 3, 4, [29, 36, 48], 'fur');
    // Broken slate stripes follow both long flank faces of the torso net.
    for (const origin of [0, 21]) {
        for (let x = 2; x < 13; x += 4) {
            panel(origin + x, 14, 2, 2, [27, 35, 48], 'fur');
            pixel(origin + x + 1, 16, [30, 38, 52]);
            pixel(origin + x, 18, [66, 76, 92]);
        }
    }
    // Amber side-eye nets retain a narrow, black vertical pupil.
    panel(36, 37, 1, 1, [15, 19, 24]);
    panel(38, 37, 1, 1, [15, 19, 24]);
    return image.pixels;
}

function skyWraith() {
    const image = atlas(173);
    const { panel, pixel } = image;
    boxPanel(image, 0, 0, 6, 7, 5, [100, 182, 182], 'glow');
    boxPanel(image, 24, 0, 4, 6, 1, [164, 230, 217], 'glow');
    boxPanel(image, 40, 0, 5, 5, 5, [91, 163, 177], 'glow');
    boxPanel(image, 60, 0, 3, 2, 2, [143, 209, 212]);
    boxPanel(image, 70, 0, 1, 4, 2, [177, 234, 223], 'bone');
    panel(80, 0, 8, 4, [226, 255, 210]);
    panel(88, 0, 4, 4, [211, 251, 225]);
    panel(96, 0, 6, 3, [25, 66, 80]);
    boxPanel(image, 0, 16, 6, 1, 7, [37, 84, 102], 'veins');
    boxPanel(image, 28, 16, 7, 1, 5, [43, 102, 118], 'veins');
    boxPanel(image, 54, 16, 3, 1, 3, [66, 125, 137], 'veins');
    boxPanel(image, 0, 32, 6, 1, 2, [145, 211, 206], 'bone');
    boxPanel(image, 16, 32, 7, 1, 1, [157, 224, 214], 'bone');
    boxPanel(image, 32, 32, 3, 1, 1, [182, 239, 224], 'bone');
    boxPanel(image, 40, 32, 1, 3, 1, [219, 249, 220], 'bone');
    boxPanel(image, 44, 32, 1, 4, 1, [87, 149, 163], 'glow');
    boxPanel(image, 48, 32, 1, 1, 2, [195, 241, 222], 'bone');
    boxPanel(image, 68, 16, 2, 1, 6, [88, 167, 181], 'veins');
    boxPanel(image, 88, 16, 1, 2, 3, [157, 225, 212], 'glow');
    // A small chest rune sits entirely inside the forward-facing 4x6 panel.
    for (const [x, y] of [[26, 2], [27, 2], [25, 3], [28, 3], [26, 4], [27, 4], [26, 5], [27, 5]]) {
        pixel(x, y, [48, 125, 140]);
    }
    // Pale veins on both cap faces of the thin membranes remain visible in flight.
    for (const [u, v, w, d] of [[0, 16, 6, 7], [28, 16, 7, 5], [54, 16, 3, 3]]) {
        for (let y = 0; y < d; y++) {
            pixel(u + d + Math.min(w - 1, Math.floor(y * w / d)), v + y, [107, 178, 181]);
            pixel(u + d + w + Math.min(w - 1, Math.floor(y * w / d)), v + y, [76, 149, 162]);
        }
    }
    return image.pixels;
}

mkdirSync(output, { recursive: true });
const textures = {
    deer: deer(),
    songbird: songbird(),
    firefly: firefly(),
    raider: raider(false),
    warlord: raider(true),
    boar: boar(),
    prowler: prowler(),
    sky_wraith: skyWraith(),
};
for (const [name, pixels] of Object.entries(textures)) {
    const path = join(output, `${name}.png`);
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, encodePng(pixels));
    console.log(`${name}.png: original ${width}x${height} RGBA atlas`);
}
