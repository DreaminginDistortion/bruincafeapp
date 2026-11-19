#!/usr/bin/env python3
"""
Turn the weekly Bruin Café lunch-menu PDF (the one emailed in Outlook) into Supabase rows.

    python3 tools/import_weekly_menu.py menu.pdf --location hq --year 2026
    -> writes menu_<location>_<start-date>.sql and .csv next to the PDF (or --out DIR)

Paste the .sql into the Supabase SQL editor (or have Claude run it). It is safe to re-run:
it first removes that location's lunch rows for the week, then inserts the new ones, and
creates inventory rows (default 20) for the new items.

Needs `pdftotext` (poppler). ALWAYS read the printed summary: PDF layouts vary, and the
course (appetizer/entree/dessert) for multi-item days is a best guess.
"""
import argparse, csv, datetime as dt, re, subprocess, sys
from pathlib import Path

DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"]
MONTHS = {m: i for i, m in enumerate(
    ["January", "February", "March", "April", "May", "June", "July",
     "August", "September", "October", "November", "December"], 1)}
THEME_RE = re.compile(r"\b(day|week|night|fest|festival|special|celebration)\b", re.I)
DESSERT_WORDS = ("tiramisu", "cake", "pie", "cookie", "brownie", "gelato", "ice cream",
                 "cheesecake", "pudding", "cannoli", "flan", "mousse", "tart")
APPETIZER_WORDS = ("cups", "bruschetta", "antipasto", "dip", "wings", "bites", "starter", "salad")
LONG_LINE = 75   # description lines are wrapped near 100 chars; item names are short


def pdf_text(path):
    return subprocess.run(["pdftotext", "-layout", str(path), "-"],
                          capture_output=True, text=True, check=True).stdout


def clean(s):
    return (s.replace("’", "'").replace("—", "-").strip())


def week_start(lines, year):
    """Finds a footer like 'September 7-11' (any dash) and returns the Monday date."""
    for ln in lines:
        m = re.search(r"([A-Z][a-z]+)\s+(\d{1,2})\s*[–—-]\s*(?:[A-Z][a-z]+\s+)?(\d{1,2})", ln)
        if m and m.group(1) in MONTHS:
            return dt.date(year, MONTHS[m.group(1)], int(m.group(2)))
    sys.exit("Couldn't find the week range (e.g. 'September 7-11') in the PDF. Pass --week-start YYYY-MM-DD.")


def join_wrapped(parts):
    out = ""
    for p in parts:
        out = out[:-1] + p if out.endswith("-") and out[-2:-1].isalpha() else (out + " " + p if out else p)
    return out.strip()


def parse_blocks(text):
    lines = [clean(l) for l in text.splitlines()]
    blocks, current = {}, None
    for ln in lines:
        if ln in DAYS:
            current = ln
            blocks[current] = []
        elif current and ln:
            if re.fullmatch(r"[A-Z][a-z]+\s+\d{1,2}\s*[–—-]\s*(?:[A-Z][a-z]+\s+)?\d{1,2}", ln):
                current = None          # footer date range
            else:
                blocks[current].append(ln)
    return lines, blocks


def course_for(name, index, total):
    low = name.lower()
    if any(w in low for w in DESSERT_WORDS):
        return "dessert"
    if total >= 3 and index == 0 and any(w in low for w in APPETIZER_WORDS):
        return "appetizer"
    return "entree"


def slug(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")[:40]


def build_rows(blocks, start, loc):
    rows, warnings = [], []
    for i, day in enumerate(DAYS):
        date = start + dt.timedelta(days=i)
        body = blocks.get(day, [])
        base = dict(location_id=loc, meal_period="lunch", menu_date=date.isoformat(), day_of_week=day)
        if not body:
            warnings.append(f"{day}: nothing found in the PDF (no row created - app will show 'Menu Unavailable')")
            continue
        if body[0].upper().startswith("CLOSED"):
            title = body[0].title().replace("Closed -", "Closed -")
            rows.append({**base, "id": f"{loc}-{date}-closed", "name": title,
                         "description": "", "course": "entree", "is_closed": True,
                         "closed_message": join_wrapped(body[1:]), "sort_order": 0})
            continue
        theme = body.pop(0) if THEME_RE.search(body[0]) and len(body) > 1 and len(body[0]) < LONG_LINE else None
        if len(body) > 1 and len(body[1]) > LONG_LINE:          # name line + wrapped description
            names, desc = [body[0]], join_wrapped(body[1:])
        else:                                                   # just a list of item names
            names, desc = body, theme or ""
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
    ap.add_argument("pdf")
    ap.add_argument("--location", default=None, help="hq or tech_center (default: guessed from the PDF title)")
    ap.add_argument("--year", type=int, default=dt.date.today().year)
    ap.add_argument("--week-start", help="Monday date YYYY-MM-DD if the PDF footer is missing")
    ap.add_argument("--qty", type=int, default=20, help="starting inventory for new items")
    ap.add_argument("--out", default=None)
    a = ap.parse_args()

    text = pdf_text(a.pdf)
    lines, blocks = parse_blocks(text)
    loc = a.location or ("tech_center" if re.search(r"tech\s*center", text, re.I) else "hq")
    start = dt.date.fromisoformat(a.week_start) if a.week_start else week_start(lines, a.year)
    if start.weekday() != 0:
        sys.exit(f"{start} is not a Monday - check --year / --week-start.")
    rows, warnings = build_rows(blocks, start, loc)
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
    print(f"Wrote {stem}.sql and .csv")


if __name__ == "__main__":
    main()
