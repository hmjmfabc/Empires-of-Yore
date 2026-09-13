#!/usr/bin/env python3
"""
帝国往事 资源完整性校验 (防止"缺模型/缺贴图/缺键名/纯色贴图"这类问题复发)
用法: python3 tools/validate_assets.py
"""
import glob
import json
import os
import re
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, 'src/main/java')
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/empire')
DATA = os.path.join(ROOT, 'src/main/resources/data')
LANGS = ['zh_cn', 'en_us', 'zh_tw']

problems = []


def decode_png(path):
    """返回 (w, h, [(r,g,b,a), ...])；支持 8/4/2/1 位与调色板 PNG。"""
    d = open(path, 'rb').read()
    pos, idat, w, h, bd, ct, plte, trns = 8, b'', 0, 0, 0, 0, None, None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            w, h, bd, ct = struct.unpack('>IIBB', data[:10])
        elif typ == b'IDAT':
            idat += data
        elif typ == b'PLTE':
            plte = data
        elif typ == b'tRNS':
            trns = data
        pos += 12 + ln
    raw = zlib.decompress(idat)
    bits = bd if ct == 3 else {0: 1, 2: 3, 4: 2, 6: 4}[ct] * bd
    stride = (w * bits + 7) // 8
    bpp = max(1, bits // 8)
    prev = bytearray(stride)
    lines = []
    i = 0
    for _ in range(h):
        f = raw[i]; i += 1
        line = bytearray(raw[i:i + stride]); i += stride
        if f == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x - bpp]) & 255
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x - bpp] if x >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        lines.append(bytes(line))
        prev = line
    out = []
    for y in range(h):
        line = lines[y]
        for x in range(w):
            if ct == 3:
                if bd == 8:
                    idx = line[x]
                elif bd == 4:
                    idx = (line[x // 2] >> (4 if x % 2 == 0 else 0)) & 0xF
                elif bd == 2:
                    idx = (line[x // 4] >> (6 - 2 * (x % 4))) & 0x3
                else:
                    idx = (line[x // 8] >> (7 - (x % 8))) & 0x1
                r, g, b = plte[idx * 3], plte[idx * 3 + 1], plte[idx * 3 + 2]
                a = trns[idx] if trns and idx < len(trns) else 255
            else:
                o = x * bpp
                if ct == 6:
                    r, g, b, a = line[o], line[o + 1], line[o + 2], line[o + 3]
                elif ct == 2:
                    r, g, b, a = line[o], line[o + 1], line[o + 2], 255
                elif ct == 4:
                    r, g, b, a = line[o], line[o], line[o], line[o + 1]
                else:
                    r = g = b = line[o]; a = 255
            out.append((r, g, b, a))
    return w, h, out


def collect_registrations():
    items, blocks = set(), set()
    for f in glob.glob(os.path.join(JAVA, '**/*.java'), recursive=True):
        t = open(f, encoding='utf-8').read()
        for m in re.finditer(r'ITEMS\.register(?:SimpleBlockItem)?\("([a-z0-9_]+)"', t):
            items.add(m.group(1))
        for m in re.finditer(r'BLOCKS\.register\("([a-z0-9_]+)"', t):
            blocks.add(m.group(1))
    return items, blocks


def check_models(items, blocks):
    for i in sorted(items):
        if not os.path.exists(f'{ASSETS}/models/item/{i}.json'):
            problems.append(f'缺物品模型: models/item/{i}.json')
    for b in sorted(blocks):
        if not os.path.exists(f'{ASSETS}/blockstates/{b}.json'):
            problems.append(f'缺 blockstate: {b}')
        if not os.path.exists(f'{ASSETS}/models/block/{b}.json'):
            problems.append(f'缺方块模型: models/block/{b}.json')
    for f in glob.glob(f'{ASSETS}/models/**/*.json', recursive=True):
        d = json.load(open(f, encoding='utf-8'))
        for v in d.get('textures', {}).values():
            if isinstance(v, str) and v.startswith('empire:'):
                if not os.path.exists(f'{ASSETS}/textures/{v[7:]}.png'):
                    problems.append(f'模型引用缺失贴图: {v} ({os.path.basename(f)})')


def check_lang(items, blocks):
    langs = {l: json.load(open(f'{ASSETS}/lang/{l}.json', encoding='utf-8')) for l in LANGS}
    base = set(langs['zh_cn'])
    # 被引用的键(Java + 数据包)
    referenced = set()
    for f in glob.glob(os.path.join(JAVA, '**/*.java'), recursive=True):
        for m in re.finditer(r'translatable\("([^"]+)"', open(f, encoding='utf-8').read()):
            referenced.add(m.group(1))
    for f in glob.glob(f'{DATA}/**/*.json', recursive=True):
        for m in re.finditer(r'"translate"\s*:\s*"([^"]+)"', open(f, encoding='utf-8').read()):
            referenced.add(m.group(1))
    for l, d in langs.items():
        if set(d) != base:
            problems.append(f'{l} 键集与 zh_cn 不一致: 差 {len(base ^ set(d))} 个')
        for i in sorted(items):
            if f'item.empire.{i}' not in d and f'block.empire.{i}' not in d:
                problems.append(f'{l} 缺键: item.empire.{i}')
        for b in sorted(blocks):
            if f'block.empire.{b}' not in d:
                problems.append(f'{l} 缺键: block.empire.{b}')
        for k in sorted(referenced - set(d)):
            problems.append(f'{l} 缺引用键: {k}')


def check_armor_layers():
    for mat in ['duraalumin', 'endite', 'titanite', 'trinity', 'miku3939']:
        for n in (1, 2):
            p = f'{ASSETS}/textures/models/armor/{mat}_layer_{n}.png'
            if not os.path.exists(p):
                problems.append(f'缺护甲层纹理: {mat}_layer_{n}.png')
                continue
            w, h, px = decode_png(p)
            if (w, h) != (64, 32):
                problems.append(f'护甲层尺寸异常: {mat}_layer_{n} = {w}x{h}')


def check_flat_textures():
    """纯色/单色阶(平板)贴图检测。"""
    for p in sorted(glob.glob(f'{ASSETS}/textures/**/*.png', recursive=True)):
        w, h, px = decode_png(p)
        opaque = [c for c in px if c[3] > 40]
        if not opaque:
            problems.append(f'全透明贴图: {p[len(ASSETS) + 1:]}')
            continue
        colors = {c[:3] for c in opaque}
        xs = [i % w for i, c in enumerate(px) if c[3] > 40]
        ys = [i // w for i, c in enumerate(px) if c[3] > 40]
        bw, bh = max(xs) - min(xs) + 1, max(ys) - min(ys) + 1
        fill = len(opaque) / (bw * bh)
        is_block = '/textures/block/' in p or '/models/armor/' in p
        if len(colors) <= 3 and len(opaque) > 20 and not is_block:
            problems.append(f'贴图色阶过少(疑似平板): {p[len(ASSETS) + 1:]} ({len(colors)} 色)')
        if fill > 0.97 and len(opaque) < 256 and '/textures/item/' in p:
            problems.append(f'物品贴图像实心方块: {p[len(ASSETS) + 1:]} (填充 {fill:.2f})')


def main():
    items, blocks = collect_registrations()
    check_models(items, blocks)
    check_lang(items, blocks)
    check_armor_layers()
    check_flat_textures()
    print(f'物品 {len(items)} | 方块 {len(blocks)} | 问题 {len(problems)}')
    for p in problems:
        print('  -', p)
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
