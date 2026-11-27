#!/usr/bin/env python3
"""
Turn the weekly Bruin Café lunch-menu PDF (the one emailed in Outlook) into Supabase rows.

    python3 tools/import_weekly_menu.py menu.pdf --location hq --year 2026
    -> writes menu_<location>_<start-date>.sql and .csv next to the PDF (or --out DIR)

Paste the .sql into the Supabase SQL editor (or have Claude run it). It is safe to re-run:
it first removes that location's lunch rows for the week, then inserts the new ones, and
creates inventory rows (default 20) for the new items.

Needs `pip install pdfplumber`; if the menu is a picture also `pip install rapidocr-onnxruntime numpy` (PNG/JPG files work too). Run with no arguments to pick the PDF in a window. ALWAYS read the printed summary: PDF layouts vary, and the
course (appetizer/entree/dessert) for multi-item days is a best guess.
"""
import argparse, csv, datetime as dt, re, subprocess, sys
from pathlib import Path

DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"]
MONTHS = {m: i for i, m in enumerate(
    ["January", "February", "March", "April", "May", "June", "July",
     "August", "September", "October", "November", "December"], 1)}
THEME_RE = re.compile(r"(day|week|night|fest|special|celebration)", re.I)
DESSERT_WORDS = ("tiramisu", "cake", "pie", "cookie", "brownie", "gelato", "ice cream",
                 "cheesecake", "pudding", "cannoli", "flan", "mousse", "tart")
APPETIZER_WORDS = ("cups", "bruschetta", "antipasto", "dip", "wings", "bites", "starter", "salad")
LONG_LINE = 75   # description lines are wrapped near 100 chars; item names are short


ocr_used = False
IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".bmp", ".webp"}


I_WORDS = {"am", "was", "will", "have", "had", "can", "would", "think", "love", "like"}


def fix_ocr_spacing(text):
    """OCR often splits a capital from its word ('P hilly', 'D ay'). Re-join them (never 'A' or 'I am')."""
    def join(m):
        cap, word = m.group(1), m.group(2)
        if cap == "A" or (cap == "I" and word in I_WORDS):
            return m.group(0)
        return cap + word
    text = re.sub(r"\b([B-Z]) ([a-z]{2,})\b", join, text)
    return re.sub(r"(?<=[a-z])(?=[A-Z][a-z])", " ", text)      # "DayItaly" -> "Day Italy"


def load_ocr_engine():
    """Returns a function image -> [(box, text), ...]. Works with either RapidOCR package and shows the REAL error."""
    errors = []
    try:                                           # newer package: pip install rapidocr onnxruntime
        from rapidocr import RapidOCR as Engine
        eng = Engine()

        def run(img):
            r = eng(img)
            return list(zip(r.boxes, r.txts)) if r is not None and r.txts else []
        return run
    except Exception as e:                         # not just ImportError: DLL / version problems land here too
        errors.append(f"rapidocr: {type(e).__name__}: {e}")
    try:                                           # older package: pip install rapidocr-onnxruntime
        from rapidocr_onnxruntime import RapidOCR as Engine
        eng = Engine()

        def run(img):
            result, _ = eng(img)
            return [(b, t) for b, t, _c in (result or [])]
        return run
    except Exception as e:
        errors.append(f"rapidocr_onnxruntime: {type(e).__name__}: {e}")
    sys.exit("This menu is an image and no OCR engine could be loaded.\n"
             f"Python: {sys.executable}  ({sys.version.split()[0]})\n  " + "\n  ".join(errors) +
             "\nInstall into THAT Python, e.g.:  \"" + sys.executable + "\" -m pip install rapidocr onnxruntime")


def ocr_images(images):
    """OCR for menus that are pictures (no extra programs needed)."""
    global ocr_used
    ocr_used = True
    run, out = load_ocr_engine(), []
    for img in images:
        boxes = sorted(((b[0][1], b[0][0], b[2][1] - b[0][1], t) for b, t in run(img)))
        line, line_y, line_h = [], None, 0
        for y, x, h, t in boxes + [(10**9, 0, 1, "")]:      # group boxes on the same row, left to right
            if line_y is not None and abs(y - line_y) > max(line_h, h) * 0.6:
                out.append(" ".join(t2 for _x, t2 in sorted(line)))
                line = []
            if not line:
                line_y, line_h = y, h
            line.append((x, t))
        out.append("")
    return fix_ocr_spacing("\n".join(out))


def pdf_text(path, force_ocr=False):
    """Text PDFs: pdfplumber (or poppler's pdftotext). Pictures / scanned PDFs: OCR."""
    path = Path(path)
    if path.suffix.lower() in IMAGE_EXTS:
        print("Image file - reading it with OCR...")
        return ocr_images([str(path)])
    text = ""
    if not force_ocr:
        try:
            import pdfplumber
            with pdfplumber.open(str(path)) as pdf:
                text = "\n".join((page.extract_text() or "") for page in pdf.pages)
        except ImportError:
            try:
                text = subprocess.run(["pdftotext", "-layout", str(path), "-"],
                                      capture_output=True, text=True, check=True).stdout
            except FileNotFoundError:
                pass
    if len(re.findall(r"Monday|Tuesday|Wednesday|Thursday|Friday", text)) >= 2:
        return text
    print("No readable text in this PDF (it's a picture) - reading it with OCR...")
    try:
        import pdfplumber
        import numpy as np
        with pdfplumber.open(str(path)) as pdf:
            imgs = [np.array(pg.to_image(resolution=220).original.convert("RGB")) for pg in pdf.pages]
    except ImportError:
        sys.exit("Run  pip install pdfplumber numpy  and try again.")
    return ocr_images(imgs)


def clean(s):
    return (s.replace("’", "'").replace("—", "-").strip())


def week_start(lines, year):
    """Finds a footer like 'September 7-11' (any dash) and returns the Monday date."""
    for ln in lines:
        m = re.search(r"([A-Z][a-z]+)\s+(\d{1,2})\s*[–—-]\s*(?:[A-Z][a-z]+\s+)?(\d{1,2})", ln)
        if m and m.group(1) in MONTHS:
            return dt.date(year, MONTHS[m.group(1)], int(m.group(2)))
        # OCR sometimes drops the dash ("September 711"): try each split, keep the one that is a Monday
        m = re.search(r"\b([A-Z][a-z]+)\s+(\d{3,4})\b", ln)
        if m and m.group(1) in MONTHS:
            digits = m.group(2)
            for cut in (1, 2):
                try:
                    start = dt.date(year, MONTHS[m.group(1)], int(digits[:cut]))
                    end_day = int(digits[cut:])
                except ValueError:
                    continue
                if start.weekday() == 0 and end_day == start.day + 4:
                    return start
    return None


def join_wrapped(parts):
    out = ""
    for p in parts:
        out = out[:-1] + p if out.endswith("-") and out[-2:-1].isalpha() else (out + " " + p if out else p)
    return out.strip()


DAY_RE = re.compile(r"^(?:\W{0,3}|OF\s+)?(Monday|Tuesday|Wednesday|Thursday|Friday)\b\s*[\u2013\u2014:-]?\s*(.*)$", re.I)
# Words from the Blue Ridge logo / footer that OCR picks up and that are never menu text
NOISE_LINE = re.compile(r"^(?i:blue\s*ridge|and technical|culinary|inary art\w*|academy.*)$|^[A-Z]{4,}$|subject to change")
NOISE_PREFIX = re.compile(r"^(?:(?i:blue\s*ridge|of)\s+(?=[A-Z])|(?:[A-Z'|.:]{2,}\s+)+(?=[A-Za-z]))")
# Common OCR slips on this menu's script font. Add your own as "wrong": "right" (case-insensitive, whole phrase).
OCR_FIXES = {"ftatbread": "flatbread", "meted": "melted", "ayers ot pasta": "Layers of pasta",
             "ofthree": "of three", "altredo": "Alfredo", "latbread": "flatbread", "served-with": "Served with",
             "oF": "of", "For dessert": "for dessert", "elote )": "Elote)", "cate": "Café"}


def apply_fixes(text):
    extra = Path(__file__).with_name("menu_fixes.txt")          # optional: one  wrong=right  per line
    fixes = dict(OCR_FIXES)
    if extra.exists():
        for ln in extra.read_text(encoding="utf-8").splitlines():
            if "=" in ln and not ln.startswith("#"):
                k, v = ln.split("=", 1); fixes[k.strip()] = v.strip()
    for wrong, right in fixes.items():
        s_ = re.sub(r"(?<![A-Za-z])" + re.escape(wrong.strip()) + r"(?![A-Za-z])", right, text, flags=re.I)
        text = s_
    return text


DAY_DASH_RE = re.compile(r"\b(Monday|Tuesday|Wednesday|Thursday|Friday)\b\s*[\u2013\u2014-]\s*(\S.*)$")


def find_day_header(ln):
    """'Tuesday', 'Thursday - Mexican Fiesta!', or 'IT'S Tuesday - Bacon Fries' (side-panel text before it)."""
    m = DAY_DASH_RE.search(ln)
    if m and len(ln[:m.start()]) <= 25 and not re.search(r"[a-z]{3,}", ln[:m.start()]):
        return m.group(1), m.group(2).strip()
    m = DAY_RE.match(ln)
    if m and len(m.group(2)) < 40 and not (m.group(2) and m.group(2)[0].islower()):
        return m.group(1).title(), m.group(2).strip()
    return None


def parse_blocks(text):
    """-> (all lines, {day: [lines]}, {day: theme}). Handles 'Thursday - Mexican Fiesta!' style headers."""
    lines = [clean(l) for l in text.splitlines()]
    blocks, themes, current = {}, {}, None
    for ln in lines:
        hdr = find_day_header(ln)
        if hdr:
            current = hdr[0]
            blocks[current] = []
            if hdr[1]:
                themes[current] = hdr[1]
            continue
        if not current or not ln or NOISE_LINE.search(ln):
            continue
        if re.fullmatch(r"[A-Z][a-z]+\s+(?:\d{1,2}\s*[\u2013\u2014-]\s*(?:[A-Z][a-z]+\s+)?\d{1,2}|\d{3,4})", ln):
            current = None          # footer date range
            continue
        blocks[current].append(NOISE_PREFIX.sub("", ln))
    return lines, blocks, themes


def course_for(name, index, total):
    low = name.lower()
    if any(w in low for w in DESSERT_WORDS):
        return "dessert"
    if total >= 3 and index == 0 and any(w in low for w in APPETIZER_WORDS):
        return "appetizer"
    return "entree"


def slug(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")[:40]


def is_description(line):
    """Item names are short with no commas/periods; descriptions are sentences."""
    return len(line) > 60 or line.endswith(".") or ", " in line


def finish_sentence(t):
    t = re.sub(r"\s+([.,!?;])", r"\1", t.strip())
    t = re.sub(r",\.(?=\s)", ",", t)       # OCR adds a stray period after a comma
    t = re.sub(r",\.$", ".", t)
    t = t.strip()
    return t if not t or t[-1] in ".!?" else t + "."


def build_rows(blocks, themes, start, loc):
    rows, warnings = [], []
    for i, day in enumerate(DAYS):
        date = start + dt.timedelta(days=i)
        body = list(blocks.get(day, []))
        base = dict(location_id=loc, meal_period="lunch", menu_date=date.isoformat(), day_of_week=day)
        if not body:
            warnings.append(f"{day}: nothing found in the PDF (no row created - app will show 'Menu Unavailable')")
            continue
        if body[0].upper().startswith("CLOSED"):
            title = re.sub(r"\s*-\s*", " - ", body[0].title(), count=1)
            rows.append({**base, "id": f"{loc}-{date}-closed", "name": title,
                         "description": "", "course": "entree", "is_closed": True,
                         "closed_message": join_wrapped(body[1:]), "sort_order": 0})
            continue
        theme = themes.get(day)
        if theme and body and is_description(body[0]) and not theme.endswith("!"):
            body.insert(0, theme)                    # header was "Day - Item name", description follows
            theme = None
        if not theme and len(body) > 1 and THEME_RE.search(body[0]) and len(body[0]) < LONG_LINE and not is_description(body[0]):
            theme = body.pop(0)                                  # theme on its own line
        if len(body) > 1 and is_description(body[1]):            # name line + wrapped description
            names, desc = [body[0]], join_wrapped(body[1:])
        else:                                                    # just a list of item names
            names, desc = body, ""
        desc = " ".join(x for x in [theme and finish_sentence(theme) if desc else theme, finish_sentence(desc)] if x).strip()
        for n, name in enumerate(names):
            rows.append({**base, "id": f"{loc}-{date}-{slug(name)}", "name": name,
                         "description": desc, "course": course_for(name, n, len(names)),
                         "is_closed": False, "closed_message": "", "sort_order": n})
        if len(names) > 1:
            warnings.append(f"{day}: {len(names)} items; courses guessed -> " +
                            ", ".join(f"{r['name']} ({r['course']})" for r in rows[-len(names):]))
    return rows, warnings


def q(v):
    return "null" if v in (None, "") else "'" + str(v).replace("'", "''") + "'"


def to_sql(rows, loc, start, qty):
    end = start + dt.timedelta(days=4)
    out = ["-- Generated by tools/import_weekly_menu.py -- review before running",
           "begin;",
           f"delete from public.menu_items where location_id = {q(loc)} and meal_period = 'lunch'",
           f"  and menu_date between '{start}' and '{end}';",
           "insert into public.menu_items (id, location_id, meal_period, name, description, course, menu_date, day_of_week, is_closed, closed_message, sort_order) values"]
    vals = [f"  ({q(r['id'])}, {q(r['location_id'])}, 'lunch', {q(r['name'])}, {q(r['description'])}, "
            f"{q(r['course'])}, {q(r['menu_date'])}, {q(r['day_of_week'])}, {str(r['is_closed']).lower()}, "
            f"{q(r['closed_message'])}, {r['sort_order']})" for r in rows]
    out.append(",\n".join(vals) + ";")
    ids = [r["id"] for r in rows if not r["is_closed"]]
    if ids:
        out.append("insert into public.inventory (item_id, quantity_on_hand, low_stock_threshold) values")
        out.append(",\n".join(f"  ({q(i)}, {qty}, 5)" for i in ids) + "\non conflict (item_id) do nothing;")
    out.append("commit;")
    return "\n".join(out) + "\n"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pdf", nargs="?", help="path to the menu PDF (leave out to pick it in a file window)")
    ap.add_argument("--location", default=None, help="hq or tech_center (default: guessed from the PDF title)")
    ap.add_argument("--year", type=int, default=dt.date.today().year)
    ap.add_argument("--week-start", help="Monday date YYYY-MM-DD if the PDF footer is missing")
    ap.add_argument("--ocr", action="store_true", help="force OCR even if the PDF has text")
    ap.add_argument("--qty", type=int, default=20, help="starting inventory for new items")
    ap.add_argument("--out", default=None)
    a = ap.parse_args()
    if not a.pdf:                                    # e.g. pressing Run in PyCharm with no arguments
        try:
            import tkinter
            from tkinter import filedialog
            tkinter.Tk().withdraw()
            a.pdf = filedialog.askopenfilename(title="Pick the weekly menu PDF", filetypes=[("Menu PDF or image", "*.pdf *.png *.jpg *.jpeg")])
        except Exception:
            a.pdf = input("Path to the menu PDF: ").strip().strip('"')
        if not a.pdf:
            sys.exit("No PDF chosen.")

    text = pdf_text(a.pdf, a.ocr)
    text = apply_fixes(text) if ocr_used else text
    lines, blocks, themes = parse_blocks(text)
    loc = a.location or ("tech_center" if re.search(r"tech\s*center", text, re.I) else "hq")
    start = dt.date.fromisoformat(a.week_start) if a.week_start else week_start(lines, a.year)
    if start is None:                    # many menus have no date on them -> assume the current week
        today = dt.date.today()
        start = today - dt.timedelta(days=today.weekday())
        print(f"No dates on the menu - assuming this week (Monday {start}). Use --week-start YYYY-MM-DD to change.")
    if start.weekday() != 0:
        sys.exit(f"{start} is not a Monday - check --year / --week-start.")
    rows, warnings = build_rows(blocks, themes, start, loc)
    if not rows:
        sys.exit("No menu rows found - the PDF layout may have changed.")

    out_dir = Path(a.out) if a.out else Path(a.pdf).resolve().parent
    out_dir.mkdir(parents=True, exist_ok=True)
    stem = out_dir / f"menu_{loc}_{start}"
    stem.with_suffix(".sql").write_text(to_sql(rows, loc, start, a.qty), encoding="utf-8")
    with stem.with_suffix(".csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader(); w.writerows(rows)

    print(f"Location: {loc}   Week: {start} .. {start + dt.timedelta(days=4)}   Rows: {len(rows)}")
    for r in rows:
        print(f"  {r['menu_date']} {r['day_of_week'][:3]}  {'CLOSED ' if r['is_closed'] else r['course']:<10} {r['name']}")
    for w in warnings:
        print("  CHECK:", w)
    if ocr_used:
        print("  NOTE: read from a picture with OCR - proofread names, descriptions and commas in the .csv before importing.")
    print(f"Wrote {stem}.sql and .csv")


if __name__ == "__main__":
    main()
