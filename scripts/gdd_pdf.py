# -*- coding: utf-8 -*-
"""Génère la version PDF des GDD : docs/gdd-*.md → docs/gdd-*.pdf.

Le .md reste la source, relue ligne par ligne dans les PR ; le PDF sert à lire et à présenter
le GDD. Après chaque modification d'un GDD, relancer le script et commiter le PDF avec le .md.

Prérequis : Python 3 et Microsoft Edge ou Google Chrome, qui imprime la page en PDF.

Usage :
    python scripts/gdd_pdf.py                       # tous les docs/gdd-*.md
    python scripts/gdd_pdf.py docs/gdd-regles.md    # un seul GDD
"""
import base64
import html
import mimetypes
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BROWSERS = [
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    os.path.expandvars(r"%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"),
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
    "/Applications/Chromium.app/Contents/MacOS/Chromium",
]
# Cases de la table d'efficacité des types → classe CSS
EFFICACITE = {"2": "x2", "½": "x05", "0": "x0", "·": "x1"}
HEX = re.compile(r"#[0-9A-Fa-f]{6}")
LIST_ITEM = re.compile(r"^(\s*)([-*]|\d+\.)\s+(.*)$")
IMAGE = re.compile(r"^!\[([^\]]*)\]\(([^)\s]+)\)$")

CSS = """
@page { size: A4; margin: 15mm 14mm 16mm 14mm;
  @bottom-left { content: "%(footer)s"; font: 8pt 'Segoe UI', sans-serif; color: #898781; }
  @bottom-right { content: counter(page) " / " counter(pages); font: 8pt 'Segoe UI', sans-serif; color: #898781; }
}
:root { --ink: #0b0b0b; --ink-2: #52514e; --muted: #898781; --hair: #e1e0d9; --base: #c3c2b7; --plane: #f6f5f2; }
* { box-sizing: border-box; }
html { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
body { margin: 0; background: #fff; color: var(--ink); font: 9.6pt/1.45 'Segoe UI', -apple-system, system-ui, sans-serif; }
.kicker { font-size: 8.6pt; letter-spacing: .14em; text-transform: uppercase; color: var(--muted); margin: 0 0 1.5mm; }
h1 { font-size: 24pt; line-height: 1.15; margin: 0 0 4mm; font-weight: 700; letter-spacing: -0.01em; }
h2 { font-size: 14.5pt; margin: 8mm 0 3mm; padding-bottom: 1.5mm; border-bottom: 1.5px solid var(--ink); font-weight: 700; }
h2 .n { color: var(--muted); font-weight: 600; margin-right: 2mm; }
h3 { font-size: 11pt; margin: 5mm 0 2mm; font-weight: 700; }
h2, h3, h2 + p, h3 + p, p.intro { break-after: avoid; }
p { margin: 0 0 2.5mm; }
ul, ol { margin: 0 0 2.5mm; padding-left: 5mm; }
.court { break-inside: avoid; }
li { margin-bottom: 1mm; break-inside: avoid; }
li > ul, li > ol { margin: 1mm 0 0; }
a { color: inherit; text-decoration-color: var(--base); }
code { font-family: 'Cascadia Mono', Consolas, Menlo, monospace; font-size: 8.4pt; }
pre { background: var(--plane); border: 1px solid var(--hair); border-radius: 4px; padding: 3mm 4mm; margin: 1mm 0 4mm;
      font-family: 'Cascadia Mono', Consolas, Menlo, monospace; font-size: 8pt; line-height: 1.45; break-inside: avoid; }
.meta { border: 1px solid var(--hair); border-left: 3px solid var(--ink-2); border-radius: 4px; background: var(--plane);
        padding: 2.5mm 4mm; margin: 0 0 4mm; color: var(--ink-2); }
table { border-collapse: collapse; width: 100%%; margin: 1mm 0 4mm; font-size: 8.7pt; }
th { text-align: left; font-weight: 600; color: var(--ink-2); border-bottom: 1.5px solid var(--base); padding: 1.6mm 2mm; vertical-align: bottom; }
td { border-bottom: 1px solid var(--hair); padding: 1.6mm 2mm; vertical-align: top; }
tr { break-inside: avoid; }
table.wide th, table.wide td:not(:last-child), .nw { white-space: nowrap; }
table.wide { font-size: 8.3pt; }
table.wide th, table.wide td { padding: 1.4mm 1.5mm; }
table.eff th, table.eff td { text-align: center; padding: 1.4mm 1mm; }
table.eff th:first-child, table.eff td:first-child { text-align: left; }
td.x2 { background: #dcefd8; color: #1d5e17; }
td.x05 { background: #fbe4dc; color: #9a3412; }
td.x0 { background: #52514e; color: #fff; font-weight: 700; }
td.x1 { color: var(--muted); }
.sw { display: inline-block; width: 8px; height: 8px; border-radius: 2px; margin-right: 1.2mm; border: 1px solid rgba(0, 0, 0, .15); }
section:has(> figure) { break-inside: avoid; }
figure { margin: 1mm 0 4mm; break-inside: avoid; }
figure img { display: block; width: 100%%; max-width: 140mm; margin: 0 auto; border: 1px solid var(--hair); border-radius: 4px; }
figcaption { font-size: 8.2pt; color: var(--muted); margin-top: 1.5mm; text-align: center; }
"""


def fail(msg):
    sys.exit(f"Erreur : {msg}")


def link(m):
    text, href = m.group(1), m.group(2)
    if re.match(r"https?://", href):
        return f'<a href="{href}">{text}</a>'
    # Un lien vers un autre .md n'a pas de sens dans le PDF : on cite le PDF correspondant
    return re.sub(r"\.md\b", ".pdf", text)


def inline(text):
    """Gras, italique, code et liens d'une ligne de Markdown."""
    out = []
    for part in re.split(r"(`[^`]+`)", text):
        if len(part) > 1 and part.startswith("`") and part.endswith("`"):
            code = part[1:-1]
            tag = f"<code>{html.escape(code)}</code>"
            if HEX.fullmatch(code):
                tag = f'<span class="nw"><i class="sw" style="background:{code}"></i>{tag}</span>'
            out.append(tag)
            continue
        s = html.escape(part, quote=False)
        s = re.sub(r"\[([^\]]+)\]\(([^)\s]+)\)", link, s)
        s = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", s)
        s = re.sub(r"(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])", r"<em>\1</em>", s)
        out.append(s)
    return "".join(out)


def cells(line):
    return [c.strip() for c in line.strip()[1:-1].split("|")]


def table(rows):
    head, body = cells(rows[0]), [cells(r) for r in rows[2:]]
    eff = "↓" in head[0]
    classes = " ".join(c for c, on in (("wide", len(head) >= 6), ("eff", eff)) if on)
    out = [f'<table class="{classes}">' if classes else "<table>", "<thead><tr>"]
    out += [f"<th>{inline(c)}</th>" for c in head]
    out.append("</tr></thead><tbody>")
    for row in body:
        out.append("<tr>")
        for i, c in enumerate(row):
            cls = EFFICACITE.get(c.strip("*")) if eff and i else None
            out.append(f'<td class="{cls}">' if cls else "<td>")
            out.append(f"{inline(c)}</td>")
        out.append("</tr>")
    out.append("</tbody></table>")
    return "".join(out)


def bullets(items):
    """Listes à puces ou numérotées, imbriquées selon l'indentation. Une liste courte n'est pas coupée entre deux pages."""
    out, stack = [], []
    keep = ' class="court"' if len(items) <= 4 else ""
    for indent, ordered, text in items:
        tag = "ol" if ordered else "ul"
        while stack and indent < stack[-1][0]:
            out.append(f"</li></{stack.pop()[1]}>")
        if stack and indent == stack[-1][0]:
            out.append("</li>")
        else:
            out.append(f"<{tag}{'' if stack else keep}>")
            stack.append((indent, tag))
        out.append(f"<li>{inline(text)}")
    while stack:
        out.append(f"</li></{stack.pop()[1]}>")
    return "".join(out)


def figure(md, alt, src, heading):
    path = md.parent / src
    if not path.is_file():
        fail(f"image introuvable : {path}")
    mime = mimetypes.guess_type(path.name)[0] or "image/png"
    data = base64.b64encode(path.read_bytes()).decode()
    # Pas de légende quand elle répète le titre de la section
    caption = f"<figcaption>{html.escape(alt)}</figcaption>" if alt and alt.lower() not in heading.lower() else ""
    return f'<figure><img src="data:{mime};base64,{data}" alt="{html.escape(alt)}">{caption}</figure>'


def to_html(md):
    """Convertit le Markdown des GDD (titres, listes, tableaux, code, images) en page HTML."""
    lines = md.read_text(encoding="utf-8").splitlines()
    body, title, heading, para, i = [], md.stem, "", [], 0
    section = False  # chaque titre de niveau 3 ouvre une <section>, pour garder un écran et sa maquette ensemble

    def flush():
        if para:
            text = " ".join(para)
            body.append(f'<p class="intro">{inline(text)}</p>' if text.endswith(":") else f"<p>{inline(text)}</p>")
            para.clear()

    while i < len(lines):
        line = lines[i]
        if not line.strip():
            flush()
            i += 1
        elif line.startswith("```"):
            flush()
            end = next((j for j in range(i + 1, len(lines)) if lines[j].startswith("```")), len(lines))
            body.append(f"<pre>{html.escape(chr(10).join(lines[i + 1:end]))}</pre>")
            i = end + 1
        elif m := re.match(r"^(#{1,3})\s+(.*)$", line):
            flush()
            level, heading = len(m.group(1)), m.group(2)
            if section:
                body.append("</section>")
            section = level == 3
            if section:
                body.append("<section>")
            if level == 1:
                title = heading
                body.append(f'<p class="kicker">Fakemon Arena · Game Design Document</p>'
                            f"<h1>{inline(re.sub(r'^GDD · ', '', heading))}</h1>")
            elif level == 2 and (n := re.match(r"^(\d+)\.\s+(.*)$", heading)):
                body.append(f'<h2><span class="n">{n.group(1)}</span>{inline(n.group(2))}</h2>')
            else:
                body.append(f"<h{level}>{inline(heading)}</h{level}>")
            i += 1
        elif line.startswith(">"):
            flush()
            quote = []
            while i < len(lines) and lines[i].startswith(">"):
                quote.append(inline(lines[i].lstrip("> ").strip()))
                i += 1
            body.append(f'<div class="meta">{"<br>".join(quote)}</div>')
        elif line.startswith("|") and i + 1 < len(lines) and re.match(r"^\|[\s|:-]+\|$", lines[i + 1].strip()):
            flush()
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                rows.append(lines[i])
                i += 1
            body.append(table(rows))
        elif m := IMAGE.match(line.strip()):
            flush()
            body.append(figure(md, m.group(1), m.group(2), heading))
            i += 1
        elif LIST_ITEM.match(line):
            flush()
            items = []
            while i < len(lines) and (m := LIST_ITEM.match(lines[i])):
                items.append((len(m.group(1)), m.group(2)[0].isdigit(), m.group(3)))
                i += 1
            body.append(bullets(items))
        else:
            para.append(line.strip())
            i += 1
    flush()
    if section:
        body.append("</section>")

    full_title = f"Fakemon Arena · {title}"
    footer = full_title.replace("\\", "\\\\").replace('"', '\\"')
    return (f'<!doctype html><html lang="fr"><head><meta charset="utf-8">'
            f"<title>{html.escape(full_title)}</title><style>{CSS % {'footer': footer}}</style></head>"
            f"<body>{''.join(body)}</body></html>")


def find_browser():
    for path in BROWSERS:
        if os.path.isfile(path):
            return path
    for name in ("msedge", "google-chrome", "chromium", "chromium-browser"):
        if found := shutil.which(name):
            return found
    fail("ni Microsoft Edge ni Google Chrome n'est installé.")


def print_pdf(browser, page, pdf):
    tmp = Path(tempfile.mkdtemp(prefix="gdd-pdf-"))
    try:
        source = tmp / "gdd.html"
        source.write_text(page, encoding="utf-8")
        pdf.unlink(missing_ok=True)
        subprocess.run([browser, "--headless=new", "--disable-gpu", "--no-pdf-header-footer", "--no-first-run",
                        "--disable-extensions", "--disable-component-update", "--disable-background-networking",
                        f"--user-data-dir={tmp / 'profil'}", f"--print-to-pdf={pdf}", source.as_uri()],
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    if not pdf.is_file():
        fail(f"le navigateur n'a pas produit {pdf.name}.")


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    sources = [Path(a).resolve() for a in sys.argv[1:]] or sorted((ROOT / "docs").glob("gdd-*.md"))
    if not sources:
        fail("aucun fichier docs/gdd-*.md.")
    browser = find_browser()
    for md in sources:
        if not md.is_file():
            fail(f"fichier introuvable : {md}")
        pdf = md.with_suffix(".pdf")
        print_pdf(browser, to_html(md), pdf)
        print(f"{md.name} → {pdf.name}")


if __name__ == "__main__":
    main()
