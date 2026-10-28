#!/usr/bin/env python3
"""Generates all 16x16 textures (no external dependencies). Run from the project root: python3 tools/generate_textures.py"""
import os, struct, zlib

OUT = "src/main/resources/assets/fldsmdfr/textures"

def png(path, px):
    raw = b"".join(b"\x00" + bytes([c for p in row for c in p]) for row in px)
    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) \
        + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "wb").write(data)

def blank():
    return [[(0, 0, 0, 0)] * 16 for _ in range(16)]

def rect(g, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                g[y][x] = c + (255,) if len(c) == 3 else c

def ell(g, cx, cy, rx, ry, c):
    for y in range(16):
        for x in range(16):
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                g[y][x] = c + (255,)

def outline(g, c=(40, 25, 15)):
    o = [row[:] for row in g]
    for y in range(16):
        for x in range(16):
            if g[y][x][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and g[ny][nx][3] > 0:
                        o[y][x] = c + (255,)
                        break
    return o

BUN = (222, 160, 80); PATTY = (110, 60, 30); LET = (90, 190, 60); CHEESE = (250, 200, 40)
RED = (200, 50, 40); TAN = (235, 190, 120)

def burger():
    g = blank()
    ell(g, 8, 6, 6, 4, BUN); rect(g, 2, 7, 13, 7, BUN)
    for x, y in ((6, 4), (9, 3), (11, 5), (4, 5)): g[y][x] = (255, 235, 190, 255)
    rect(g, 2, 8, 13, 8, LET); rect(g, 3, 9, 12, 9, CHEESE)
    rect(g, 2, 10, 13, 11, PATTY); rect(g, 3, 12, 12, 13, BUN)
    return outline(g)

def pizza():
    g = blank()
    for y in range(2, 14):
        w = int((y - 2) * 0.6) + 1
        rect(g, 8 - w, y, 8 + w - 1, y, CHEESE)
    rect(g, 3, 12, 12, 14, (200, 130, 50))
    for x, y in ((7, 5), (9, 8), (6, 9), (8, 11), (10, 11)): rect(g, x, y, x + 1, y + 1, RED)
    return outline(g)

def hotdog():
    g = blank()
    rect(g, 1, 6, 14, 12, TAN)
    rect(g, 2, 7, 13, 10, (200, 70, 50))
    for i in range(2, 13, 2): rect(g, i, 8, i, 8, (250, 215, 40))
    rect(g, 1, 12, 14, 12, (200, 150, 90))
    return outline(g)

def sandwich():
    g = blank()
    for y in range(3, 13):
        w = 12 - (y - 3)
        rect(g, 8 - w // 2, y, 8 + w // 2 - 1, y, TAN)
    for y in range(5, 11):
        w = 9 - (y - 5)
        rect(g, 8 - w // 2, y, 8 + w // 2 - 1, y, LET if y % 2 else RED)
    rect(g, 3, 12, 12, 13, (190, 130, 70))
    return outline(g)

def donut():
    g = blank()
    ell(g, 8, 8, 6.5, 6.5, (215, 150, 80))
    ell(g, 8, 8, 6, 6, (240, 110, 170))
    ell(g, 8, 8, 2, 2, (0, 0, 0, 0)[:3])
    for y in range(6, 11):
        for x in range(6, 11):
            if (x - 8) ** 2 + (y - 8) ** 2 <= 4: g[y][x] = (0, 0, 0, 0)
    for x, y in ((4, 5), (6, 3), (10, 4), (12, 7), (11, 11), (5, 11), (3, 8), (8, 13)):
        g[y][x] = (255, 255, 255, 255)
    g[4][9] = (80, 200, 255, 255); g[12][8] = (255, 230, 60, 255); g[9][4] = (120, 255, 120, 255)
    return outline(g)

def cake():
    g = blank()
    rect(g, 2, 7, 13, 13, (250, 235, 220))
    rect(g, 2, 7, 13, 8, (240, 120, 160))
    rect(g, 2, 10, 13, 10, (120, 70, 40))
    rect(g, 2, 13, 13, 13, (200, 150, 90))
    ell(g, 8, 5, 2, 2, RED)
    g[3][8] = (60, 140, 40, 255)
    return outline(g)

def fries():
    g = blank()
    for i, x in enumerate(range(3, 13, 2)):
        rect(g, x, 2 + (i % 3), x + 1, 9, (250, 205, 70))
    rect(g, 2, 8, 13, 14, (210, 40, 40))
    rect(g, 2, 8, 13, 9, (235, 70, 60))
    rect(g, 6, 10, 9, 12, (250, 220, 60))
    return outline(g)

def core_tex():
    g = [[(245, 244, 238, 255)] * 16 for _ in range(16)]
    rect(g, 0, 5, 15, 7, (236, 115, 30)); rect(g, 0, 11, 15, 12, (236, 115, 30))
    rect(g, 0, 0, 15, 0, (170, 170, 175)); rect(g, 0, 15, 15, 15, (170, 170, 175))
    for x in (3, 8, 13): g[9][x] = (90, 170, 255, 255)
    return g

def anchor_tex():
    g = [[(110, 114, 122, 255)] * 16 for _ in range(16)]
    rect(g, 0, 0, 15, 1, (80, 84, 92)); rect(g, 0, 14, 15, 15, (80, 84, 92))
    rect(g, 5, 5, 10, 10, (236, 115, 30)); rect(g, 6, 6, 9, 9, (255, 190, 90))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)): g[y][x] = (200, 200, 205, 255)
    return g

if __name__ == "__main__":
    for n, f in (("burger", burger), ("pizza", pizza), ("hotdog", hotdog), ("sandwich", sandwich),
                 ("donut", donut), ("cake", cake), ("fries", fries)):
        png(f"{OUT}/items/{n}.png", f())
    png(f"{OUT}/blocks/fldsmdfr_core.png", core_tex())
    png(f"{OUT}/blocks/fldsmdfr_anchor.png", anchor_tex())
    print("textures generated")
