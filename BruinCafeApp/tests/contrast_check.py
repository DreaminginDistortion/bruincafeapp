"""WCAG contrast check for the app's color pairs. Reads the real hex values from Color.kt.
Run:  python3 tests/contrast_check.py
AA needs 4.5:1 for normal text, 3:1 for large text and for UI component outlines."""
import re, sys, pathlib

src = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/java/com/bruincafe/app/ui/theme/Color.kt"
C = {n: int(h, 16) & 0xFFFFFF for n, h in re.findall(r"val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", src.read_text())}
C["White"] = 0xFFFFFF

def lum(rgb):
    ch = [(rgb >> s) & 255 for s in (16, 8, 0)]
    lin = [c / 255 / 12.92 if c / 255 <= 0.03928 else ((c / 255 + 0.055) / 1.055) ** 2.4 for c in ch]
    return 0.2126 * lin[0] + 0.7152 * lin[1] + 0.0722 * lin[2]

def ratio(a, b):
    la, lb = sorted((lum(C[a]), lum(C[b])), reverse=True)
    return (la + 0.05) / (lb + 0.05)

# (text/outline color, background, minimum ratio, where it is used)
pairs = [
    ("CafeInk",      "CafeSurface",   4.5, "body text on screen background"),
    ("CafeInk",      "CafeCream",     4.5, "text on demo-controls panel"),
    ("CafeInk",      "CafeGoldLight", 4.5, "text on menu / order cards"),
    ("White",        "CafeGreen",     4.5, "text on primary buttons, rewards card, cart bar"),
    ("White",        "CafeGoldDark",  4.5, "text on Today's Special / Kitchen closed banner"),
    ("CafeGoldDark", "CafeGoldLight", 4.5, "course label on cards, '+points' text"),
    ("CafeGoldDark", "CafeSurface",   4.5, "Low Stock badge text"),
    ("CafeGreen",    "CafeSurface",   4.5, "In Stock badge, selected tab, links"),
    ("CafeError",    "CafeSurface",   4.5, "Sold Out badge, error text"),
    ("CafeError",    "CafeCream",     4.5, "error text on demo-controls panel"),
    ("CafeGreen",    "CafeCream",     3.0, "chip outline vs panel (UI component boundary)"),
    ("HcBlack",      "HcWhite",       4.5, "high contrast: all text"),
    ("HcWhite",      "HcGreen",       4.5, "high contrast: primary buttons"),
    ("HcError",      "HcWhite",       4.5, "high contrast: errors"),
]
bad = 0
for fg, bg, need, where in pairs:
    r = ratio(fg, bg)
    ok = r >= need
    bad += not ok
    print(f"{'PASS' if ok else 'FAIL'}  {r:5.2f}:1 (need {need})  {fg} on {bg}  -- {where}")

old = ratio("CafeGold", "CafeGoldLight")
print(f"\nFor reference, the ORIGINAL brand gold on cream was only {old:.2f}:1 (fails AA) -> replaced by CafeGoldDark for text.")
print("ALL CONTRAST CHECKS PASSED" if bad == 0 else f"{bad} CHECK(S) FAILED")
sys.exit(1 if bad else 0)
