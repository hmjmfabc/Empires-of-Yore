#!/usr/bin/env python3
"""
帝国往事 - 物品贴图生成器 v2 (换生图方式)
思路: 以原版官方 16x16 贴图为"形状模板", 按亮度分位映射到各材质调色板,
      从而保证轮廓/明暗/像素结构与原版完全一致, 仅替换材质颜色。
      工具/护甲的木柄(暖色)像素保持原样。
"""
import zlib, struct, os

VANILLA = 'build/vanilla_tex'
OUT = 'src/main/resources/assets/empire/textures/item'

# 调色板: H=高光 M=本体 L=暗部 O=描边
PALETTES = {
    'titan':   dict(H=(0xe8,0xd8,0xb8), M=(0xb7,0x98,0x6c), L=(0x6e,0x53,0x39), O=(0x0e,0x0a,0x05)),
    'carbon':  dict(H=(0x98,0xa1,0xab), M=(0x59,0x60,0x69), L=(0x34,0x3a,0x43), O=(0x13,0x15,0x19)),
    'duraal':  dict(H=(0xe6,0xea,0xee), M=(0xad,0xb5,0xbd), L=(0x7f,0x86,0x8e), O=(0x1b,0x1e,0x21)),
    'endite':  dict(H=(0xc1,0x93,0xff), M=(0x8a,0x52,0xd8), L=(0x5a,0x2f,0xa0), O=(0x1c,0x0f,0x33)),
    'alum':    dict(H=(0xf0,0xf3,0xf6), M=(0xc3,0xca,0xd2), L=(0x8f,0x97,0x9f), O=(0x2a,0x2f,0x35)),
    'mag':     dict(H=(0xf6,0xf7,0xf2), M=(0xcf,0xd2,0xc9), L=(0x9a,0x9c,0x92), O=(0x2b,0x2d,0x28)),
    'chrom':   dict(H=(0xdf,0xea,0xf5), M=(0x9f,0xb4,0xc9), L=(0x6b,0x7f,0x95), O=(0x22,0x2b,0x36)),
    'titanium':dict(H=(0xd8,0xde,0xe3), M=(0x8f,0x97,0x9e), L=(0x5d,0x64,0x6b), O=(0x1e,0x23,0x28)),
    'coke':    dict(H=(0x4a,0x4a,0x50), M=(0x24,0x24,0x28), L=(0x12,0x12,0x15), O=(0x05,0x05,0x06)),
    'energy':  dict(H=(0xff,0xb0,0x9a), M=(0xd6,0x4b,0x3a), L=(0x8f,0x2a,0x1f), O=(0x2b,0x0d,0x08)),
    'aenergy': dict(H=(0xff,0xd9,0x8a), M=(0xe0,0x8b,0x1f), L=(0x9c,0x5a,0x10), O=(0x33,0x1d,0x05)),
    'endscrap':dict(H=(0xd9,0xc0,0xff), M=(0x8b,0x5c,0xd6), L=(0x57,0x30,0x9c), O=(0x1d,0x10,0x33)),
    'copper':  dict(H=(0xf0,0xc4,0x8a), M=(0xc0,0x7f,0x3c), L=(0x7d,0x4c,0x1c), O=(0x2a,0x17,0x07)),
    'whitegold':dict(H=(0xff,0xf2,0xc4), M=(0xe0,0xbd,0x5f), L=(0x9c,0x7c,0x2c), O=(0x33,0x28,0x0c)),
}

# 目标贴图 -> (原版模板, 调色板, 是否保留木柄暖色)
PLAN = [
    # 钛合金(补齐/修正方块状贴图) —— 与既有 titanite_pickaxe/sword 的骨金风格一致
    ('titanite_axe',        'iron_axe',        'titan', True),
    ('titanite_shovel',     'iron_shovel',     'titan', True),
    ('titanite_hoe',        'iron_hoe',        'titan', True),
    ('titanite_boots',      'iron_boots',      'titan', False),
    ('titanite_ingot',      'iron_ingot',      'titan', False),
    # 碳钢
    ('carbon_steel_pickaxe','iron_pickaxe',    'carbon', True),
    ('carbon_steel_axe',    'iron_axe',        'carbon', True),
    ('carbon_steel_shovel', 'iron_shovel',     'carbon', True),
    ('carbon_steel_hoe',    'iron_hoe',        'carbon', True),
    ('carbon_steel_sword',  'iron_sword',      'carbon', True),
    ('carbon_steel_ingot',  'iron_ingot',      'carbon', False),
    # 硬铝
    ('duraalumin_pickaxe',  'iron_pickaxe',    'duraal', True),
    ('duraalumin_axe',      'iron_axe',        'duraal', True),
    ('duraalumin_shovel',   'iron_shovel',     'duraal', True),
    ('duraalumin_hoe',      'iron_hoe',        'duraal', True),
    ('duraalumin_sword',    'iron_sword',      'duraal', True),
    ('duraalumin_helmet',   'iron_helmet',     'duraal', False),
    ('duraalumin_chestplate','iron_chestplate','duraal', False),
    ('duraalumin_leggings', 'iron_leggings',   'duraal', False),
    ('duraalumin_boots',    'iron_boots',      'duraal', False),
    ('duraalumin_ingot',    'iron_ingot',      'duraal', False),
    # 末影合金
    ('endite_pickaxe',      'iron_pickaxe',    'endite', True),
    ('endite_axe',          'iron_axe',        'endite', True),
    ('endite_shovel',       'iron_shovel',     'endite', True),
    ('endite_hoe',          'iron_hoe',        'endite', True),
    ('endite_sword',        'iron_sword',      'endite', True),
    ('endite_helmet',       'iron_helmet',     'endite', False),
    ('endite_chestplate',   'iron_chestplate', 'endite', False),
    ('endite_leggings',     'iron_leggings',   'endite', False),
    ('endite_boots',        'iron_boots',      'endite', False),
    ('endite_ingot',        'iron_ingot',      'endite', False),
    ('endite_scrap',        'netherite_scrap', 'endscrap', False),
    # 其他金属锭
    ('aluminum_ingot',      'iron_ingot',      'alum', False),
    ('magnesium_ingot',     'iron_ingot',      'mag', False),
    ('chromium_ingot',      'iron_ingot',      'chrom', False),
    ('titanium_ingot',      'iron_ingot',      'titanium', False),
    # 材料/道具
    ('coke',                'coal',            'coke', False),
    ('energy_stone',        'amethyst_shard',  'energy', False),
    ('advanced_energy_stone','amethyst_shard', 'aenergy', False),
    ('upgrade_tool',        'netherite_upgrade_smithing_template', 'copper', False),
    ('professional_upgrade_tools', 'netherite_upgrade_smithing_template', 'whitegold', False),
    ('raw_chromium',        'raw_iron',        'chrom', False),
    ('raw_magnesium',       'raw_iron',        'mag', False),
]


def decode(path):
    d = open(path, 'rb').read()
    pos, idat, w, h, bd, ct, plte, trns = 8, b'', 0, 0, 0, 0, None, None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos+4])[0]
        typ = d[pos+4:pos+8]
        data = d[pos+8:pos+8+ln]
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
    bits = bd if ct == 3 else {0:1, 2:3, 4:2, 6:4}[ct] * bd
    stride = (w*bits + 7)//8
    bpp = max(1, bits//8)
    prev = bytearray(stride)
    lines = []
    i = 0
    for _ in range(h):
        f = raw[i]; i += 1
        line = bytearray(raw[i:i+stride]); i += stride
        if f == 1:
            for x in range(bpp, stride): line[x] = (line[x]+line[x-bpp]) & 255
        elif f == 2:
            for x in range(stride): line[x] = (line[x]+prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                a = line[x-bpp] if x >= bpp else 0
                line[x] = (line[x]+((a+prev[x])>>1)) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x-bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x-bpp] if x >= bpp else 0
                p = a+b-c
                pa, pb, pc = abs(p-a), abs(p-b), abs(p-c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x]+pr) & 255
        lines.append(bytes(line)); prev = line
    rgba = []
    for y in range(h):
        line = lines[y]
        for x in range(w):
            if ct == 3:
                if bd == 8: idx = line[x]
                elif bd == 4: idx = (line[x//2] >> (4 if x % 2 == 0 else 0)) & 0xF
                elif bd == 2: idx = (line[x//4] >> (6-2*(x % 4))) & 0x3
                else: idx = (line[x//8] >> (7-(x % 8))) & 0x1
                r, g, b = plte[idx*3], plte[idx*3+1], plte[idx*3+2]
                a = trns[idx] if trns and idx < len(trns) else 255
            else:
                o = x*bpp
                if ct == 6: r, g, b, a = line[o], line[o+1], line[o+2], line[o+3]
                elif ct == 2: r, g, b, a = line[o], line[o+1], line[o+2], 255
                elif ct == 4: r, g, b, a = line[o], line[o], line[o], line[o+1]
                else: r = g = b = line[o]; a = 255
            rgba.append((r, g, b, a))
    return w, h, rgba


def write_png(path, w, h, px):
    def chunk(t, data):
        c = t + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
    raw = b''.join(b'\x00' + bytes(px[y*w*4:(y+1)*w*4]) for y in range(h))
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n'
                           + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
                           + chunk(b'IDAT', zlib.compress(raw, 9))
                           + chunk(b'IEND', b''))


def lum(c):
    return 0.299*c[0] + 0.587*c[1] + 0.114*c[2]


def is_wood(c):
    r, g, b, _ = c
    return r > g > b and (r-b) > 25 and r > 60


def recolor(src_rgba, w, h, pal, keep_wood):
    """按亮度分位映射到调色板; keep_wood=True 时保留木柄暖色像素"""
    idxs = [i for i, c in enumerate(src_rgba) if c[3] > 40]
    metals = [i for i in idxs if not (keep_wood and is_wood(src_rgba[i]))]
    lums = sorted(lum(src_rgba[i]) for i in metals)
    if not lums:
        return [0]*(w*h*4)
    def q(p):
        return lums[min(len(lums)-1, int(len(lums)*p))]
    lo, mid, hi = q(0.30), q(0.65), q(0.92)
    out = [0]*(w*h*4)
    for i in idxs:
        c = src_rgba[i]
        if keep_wood and is_wood(c):
            out[i*4:i*4+4] = [c[0], c[1], c[2], 255]
            continue
        l = lum(c)
        if l <= lo:   tone = pal['L']
        elif l <= mid: tone = pal['M']
        elif l <= hi:  tone = pal['H']
        else:          tone = pal['H']
        # 最暗的像素视为描边
        if l < lums[0] + 8:
            tone = pal['O']
        out[i*4:i*4+4] = [tone[0], tone[1], tone[2], 255]
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, template, palname, keep_wood in PLAN:
        src = f'{VANILLA}/{template}.png'
        w, h, rgba = decode(src)
        px = recolor(rgba, w, h, PALETTES[palname], keep_wood)
        write_png(f'{OUT}/{name}.png', w, h, px)
        # 校验: 轮廓(alpha)必须与模板完全一致
        mism = sum(1 for i in range(w*h) if (rgba[i][3] > 40) != (px[i*4+3] > 40))
        print(f'{name:28s} <- {template:36s} {palname:9s} alpha-mismatch={mism}')


if __name__ == '__main__':
    main()
