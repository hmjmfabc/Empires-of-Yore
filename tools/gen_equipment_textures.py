#!/usr/bin/env python3
# 为帝国往事生成装备贴图 (16x16 RGBA, 黑色描边 + 三色调 pixel art)
import zlib, struct, os

PALETTES = {
    # H=高光 M=本体 L=暗部 O=描边 V=木柄亮 W=木柄中 X=木柄暗
    'carbon':  dict(H=(0x98,0xa1,0xab), M=(0x59,0x60,0x69), L=(0x34,0x3a,0x43), O=(0x13,0x15,0x19),
                    V=(0xb0,0x8a,0x55), W=(0x8a,0x66,0x38), X=(0x5f,0x45,0x22)),
    'duraal':  dict(H=(0xe6,0xea,0xee), M=(0xad,0xb5,0xbd), L=(0x7f,0x86,0x8e), O=(0x1b,0x1e,0x21),
                    V=(0xb0,0x8a,0x55), W=(0x8a,0x66,0x38), X=(0x5f,0x45,0x22)),
    'endite':  dict(H=(0xc1,0x93,0xff), M=(0x8a,0x52,0xd8), L=(0x5a,0x2f,0xa0), O=(0x1c,0x0f,0x33),
                    V=(0xb0,0x8a,0x55), W=(0x8a,0x66,0x38), X=(0x5f,0x45,0x22)),
    'titan':   dict(H=(0xe8,0xd8,0xb8), M=(0xb7,0x98,0x6c), L=(0x6e,0x53,0x39), O=(0x0e,0x0a,0x05),
                    V=(0xb0,0x8a,0x55), W=(0x8a,0x66,0x38), X=(0x5f,0x45,0x22)),
}

TOOLS = {
    'pickaxe': [
        ".......HH.......",
        "....HMMMMM......",
        "...HMMMMMMM.....",
        "...MMMMMMM......",
        "..MMM...MMM.....",
        "..LLM...MLL.....",
        "........WW......",
        ".........WW.....",
        "..........WW....",
        "...........WW...",
        "............WW..",
        ".............WW.",
        "..............WW",
        "...............W",
        "................",
        "................",
    ],
    'axe': [
        "..HHHHH..........",
        ".HMMMMMMM........",
        ".HMMMMMM.........",
        ".HMMMMM..........",
        ".HMMMM...........",
        ".HMMMM...........",
        "..HMMM...........",
        "..WMM............",
        "..WWW............",
        "...WWW...........",
        "....WWW..........",
        ".....WWW.........",
        "......WWW........",
        ".......WWW.......",
        "........WWW......",
        ".........WW......",
    ],
    'shovel': [
        "...HHH...........",
        "..HMMMH..........",
        "..HMMMH..........",
        "..HMMMH..........",
        "..HMMMH..........",
        "..HMMMH..........",
        "..HMMMH..........",
        "..HMMMH..........",
        "...WMML..........",
        "....WW...........",
        ".....WW..........",
        "......WW.........",
        ".......WW........",
        "........WW.......",
        ".........WW......",
        "..........WW.....",
    ],
    'hoe': [
        "..HHHHHHHH.......",
        "..HMMMMMMM.......",
        "..HMMMMMMM.......",
        "..HMMMMMMM.......",
        "..MMMMMML........",
        ".....WW..........",
        "......WW.........",
        ".......WW........",
        "........WW.......",
        ".........WW......",
        "..........WW.....",
        "...........WW....",
        "............WW...",
        ".............WW..",
        "..............WW.",
        "...............W.",
    ],
    'sword': [
        "........MM.......",
        ".......MMM.......",
        ".......MMM.......",
        ".......MMM.......",
        ".......MMM.......",
        ".......MMM.......",
        ".......MMM.......",
        ".......MMM.......",
        ".......MMM.......",
        ".......LLL.......",
        "..HMMMMMMMMMH....",
        ".......WWW.......",
        ".......WWW.......",
        ".......WWW.......",
        "......WXWXW......",
        "....WWW.WWW......",
    ],
}

ARMOR = {
    'helmet': [
        "................",
        ".....MMMM.......",
        "....MMMMMM......",
        "...MMMMMMMM.....",
        "..MMMMMMMMMM....",
        "..MMMMMMMMMM....",
        "..MMM.MMM.MM....",
        "..MMM.MMM.MM....",
        "..MMM....MMM....",
        "..HHM....MHH....",
        "...MM....MM.....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    'chestplate': [
        "................",
        "..HHMMMMMMHH....",
        "..HMMMMMMMMH....",
        "..HMMMMMMMMH....",
        "..HMMMMMMMMH....",
        "..HMMMMMMMMH....",
        "...HMMMMMMH.....",
        "...HMMMMMMH.....",
        "...HMMMMMMH.....",
        "...HHMMMMHH.....",
        "....MMMMMM......",
        "....MMMMMM......",
        "....MM..MM......",
        "....MM..MM......",
        "................",
        "................",
    ],
    'leggings': [
        "................",
        "................",
        "....MMMMMM......",
        "...MMMMMMMM.....",
        "...MMMMMMMM.....",
        "....MMMMMM......",
        "....MMMMMM......",
        "...MMMMMMM......",
        "...MMMMMMM......",
        "..MMMM.MMMM.....",
        "..MMM...MMM.....",
        "..MM.....MM.....",
        "..MM.....MM.....",
        "................",
        "................",
        "................",
    ],
    'boots': [
        "................",
        "................",
        "................",
        "................",
        "...MMM...MMM....",
        "...MMM...MMM....",
        "...MMM...MMM....",
        "...MMM...MMM....",
        "...HMM...MMH....",
        "..MMMMM.MMMMM...",
        "..MMMMM.MMMMM...",
        "..MMMM..MMMM....",
        "...MM....MM.....",
        "................",
        "................",
        "................",
    ],
}

INGOT = [
    "................",
    "................",
    "................",
    "................",
    "....HHHHHHHH....",
    "..HHHHMMMMMMH...",
    "..HHMMMMMMMMH...",
    "..HHMMMMMMMMH...",
    "..HHMMMMMMMMH...",
    "..HHMMMMMMMLH...",
    "..HLLLLLLLLLH...",
    "...LLLLLLLLL....",
    "................",
    "................",
    "................",
    "................",
]

def write_png(path, w, h, px):
    def chunk(t, data):
        c = t + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
    raw = b''.join(b'\x00' + bytes(px[y*w*4:(y+1)*w*4]) for y in range(h))
    out = b'\x89PNG\r\n\x1a\n'
    out += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    out += chunk(b'IDAT', zlib.compress(raw, 9))
    out += chunk(b'IEND', b'')
    open(path, 'wb').write(out)

def render(rows, pal):
    w = h = 16
    grid = [[None]*w for _ in range(h)]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal and ch != 'O':
                grid[y][x] = ch
    filled = [(x, y) for y in range(h) for x in range(w) if grid[y][x]]
    for (x, y) in filled:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if dx == 0 and dy == 0: continue
                nx, ny = x+dx, y+dy
                if 0 <= nx < w and 0 <= ny < h and grid[ny][nx] is None:
                    grid[ny][nx] = 'O'
    px = [0]*(w*h*4)
    for y in range(h):
        for x in range(w):
            o = (y*w+x)*4
            c = grid[y][x]
            if c is None: continue
            r, g, b = pal[c]
            px[o:o+4] = [r, g, b, 255]
    return grid, px

def preview(grid):
    for row in grid:
        print(''.join('#' if c else ' ' for c in row))

OUT = 'src/main/resources/assets/empire/textures/item'
os.makedirs(OUT, exist_ok=True)

NAMES = {'carbon': 'carbon_steel', 'duraal': 'duraalumin', 'endite': 'endite'}
PLAN = []
for tier in ('carbon', 'duraal', 'endite'):
    for tool in TOOLS:
        PLAN.append((f'{NAMES[tier]}_{tool}', TOOLS[tool], tier))
    if tier != 'carbon':
        for arm in ARMOR:
            PLAN.append((f'{NAMES[tier]}_{arm}', ARMOR[arm], tier))
PLAN.append(('titanite_hoe', TOOLS['hoe'], 'titan'))
for tier in ('carbon', 'duraal', 'endite'):
    PLAN.append((f'{NAMES[tier]}_ingot', INGOT, tier))

for name, rows, tier in PLAN:
    grid, px = render(rows, PALETTES[tier])
    write_png(f'{OUT}/{name}.png', 16, 16, px)
    print('OK', name)

print('\n== previews ==')
for name, tier in [('pickaxe', 'carbon'), ('axe', 'carbon'), ('shovel', 'carbon'), ('hoe', 'carbon'), ('sword', 'carbon'),
                   ('helmet', 'endite'), ('chestplate', 'endite'), ('leggings', 'endite'), ('boots', 'endite'), ('ingot', 'carbon')]:
    rows = TOOLS[name] if name in TOOLS else (ARMOR[name] if name in ARMOR else INGOT)
    print('--', name)
    grid, _ = render(rows, PALETTES[tier])
    preview(grid)
