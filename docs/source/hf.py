"""
Head First style PDF toolkit (reportlab).

Every document in /docs is generated from Python so it can be regenerated after edits:
    cd docs/source && python3 build_all.py

Head First signature elements implemented here:
  Brain Power, There are no Dumb Questions, Sharpen your pencil, Watch it!,
  BULLET POINTS, Fireside Chat, Real-life example, Interview tip, "In the app",
  handwritten annotations and simple diagrams.
"""
import re
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, CondPageBreak, Flowable, Frame, KeepTogether, PageBreak,
                                PageTemplate, Paragraph, Preformatted, Spacer, Table, TableStyle)
from reportlab.graphics.shapes import Drawing, Rect, String, Line, Polygon, Circle
from reportlab.platypus.tableofcontents import TableOfContents

SUP = "/System/Library/Fonts/Supplemental/"
pdfmetrics.registerFont(TTFont("Body", SUP + "Georgia.ttf"))
pdfmetrics.registerFont(TTFont("Body-Bold", SUP + "Georgia Bold.ttf"))
pdfmetrics.registerFont(TTFont("Body-Italic", SUP + "Georgia Italic.ttf"))
pdfmetrics.registerFont(TTFont("Body-BoldItalic", SUP + "Georgia Bold Italic.ttf"))
pdfmetrics.registerFontFamily("Body", normal="Body", bold="Body-Bold", italic="Body-Italic", boldItalic="Body-BoldItalic")
pdfmetrics.registerFont(TTFont("Head", SUP + "Arial Black.ttf"))
pdfmetrics.registerFont(TTFont("Sans", SUP + "Trebuchet MS.ttf"))
pdfmetrics.registerFont(TTFont("Sans-Bold", SUP + "Trebuchet MS Bold.ttf"))
pdfmetrics.registerFontFamily("Sans", normal="Sans", bold="Sans-Bold", italic="Sans", boldItalic="Sans-Bold")
pdfmetrics.registerFont(TTFont("Hand", SUP + "Bradley Hand Bold.ttf"))
pdfmetrics.registerFont(TTFont("Mono", SUP + "Courier New.ttf"))
pdfmetrics.registerFont(TTFont("Mono-Bold", SUP + "Courier New Bold.ttf"))
pdfmetrics.registerFont(TTFont("Uni", SUP + "Arial Unicode.ttf"))

# Palette
INK = colors.HexColor("#1F2937")
MUTED = colors.HexColor("#6B7280")
ACCENT = colors.HexColor("#0E7490")      # teal
HAND = colors.HexColor("#1D4ED8")        # annotation blue
RED = colors.HexColor("#B91C1C")
BOX = {
    "brain": ("BRAIN POWER", colors.HexColor("#FEF3C7"), colors.HexColor("#B45309")),
    "dumb": ("THERE ARE NO DUMB QUESTIONS", colors.HexColor("#EEF2FF"), colors.HexColor("#4338CA")),
    "pencil": ("SHARPEN YOUR PENCIL", colors.HexColor("#F3F4F6"), colors.HexColor("#111827")),
    "watch": ("WATCH IT!", colors.HexColor("#FEE2E2"), colors.HexColor("#B91C1C")),
    "bullets": ("BULLET POINTS", colors.HexColor("#ECFEFF"), colors.HexColor("#0E7490")),
    "real": ("REAL LIFE", colors.HexColor("#DCFCE7"), colors.HexColor("#15803D")),
    "interview": ("INTERVIEW TIP", colors.HexColor("#E0F2FE"), colors.HexColor("#0369A1")),
    "app": ("SEE IT MOVE: IN THE APP", colors.HexColor("#F5F3FF"), colors.HexColor("#6D28D9")),
    "fireside": ("FIRESIDE CHAT", colors.HexColor("#FFF7ED"), colors.HexColor("#C2410C")),
    "relax": ("RELAX", colors.HexColor("#F0FDF4"), colors.HexColor("#166534")),
    "source": ("SOURCE", colors.HexColor("#F9FAFB"), colors.HexColor("#374151")),
}

_BODY_CMAP = set(pdfmetrics.getFont("Body").face.charToGlyph.keys())


def fx(text: str) -> str:
    """Wrap every non-ASCII glyph that Georgia lacks in the Arial Unicode font."""
    out = []
    for ch in text:
        if ord(ch) > 127 and ord(ch) not in _BODY_CMAP:
            out.append(f'<font name="Uni">{ch}</font>')
        else:
            out.append(ch)
    return "".join(out)


S = {
    "body": ParagraphStyle("body", fontName="Body", fontSize=10.5, leading=15.5, textColor=INK, spaceAfter=6),
    "small": ParagraphStyle("small", fontName="Body", fontSize=9, leading=12.5, textColor=INK, spaceAfter=3),
    "h1": ParagraphStyle("h1", fontName="Head", fontSize=24, leading=28, textColor=INK, spaceAfter=4),
    "h2": ParagraphStyle("h2", fontName="Head", fontSize=15, leading=19, textColor=ACCENT, spaceBefore=10, spaceAfter=5),
    "h3": ParagraphStyle("h3", fontName="Sans-Bold", fontSize=12, leading=15, textColor=INK, spaceBefore=6, spaceAfter=3),
    "hand": ParagraphStyle("hand", fontName="Hand", fontSize=13, leading=16, textColor=HAND, spaceAfter=4),
    "handc": ParagraphStyle("handc", fontName="Hand", fontSize=13, leading=16, textColor=HAND, alignment=TA_CENTER),
    "boxtitle": ParagraphStyle("boxtitle", fontName="Head", fontSize=10.5, leading=13, spaceAfter=4),
    "code": ParagraphStyle("code", fontName="Mono", fontSize=8.6, leading=11, textColor=colors.HexColor("#0F172A")),
    "cell": ParagraphStyle("cell", fontName="Body", fontSize=8.8, leading=11.5, textColor=INK),
    "cellb": ParagraphStyle("cellb", fontName="Sans-Bold", fontSize=9, leading=11.5, textColor=colors.white),
    "toc1": ParagraphStyle("toc1", fontName="Sans-Bold", fontSize=11, leading=16, leftIndent=10),
    "toc2": ParagraphStyle("toc2", fontName="Sans", fontSize=9.5, leading=13, leftIndent=26, textColor=MUTED),
    "q": ParagraphStyle("q", fontName="Body-Bold", fontSize=10, leading=14, textColor=colors.HexColor("#4338CA"), spaceAfter=2),
    "a": ParagraphStyle("a", fontName="Body", fontSize=10, leading=14, textColor=INK, spaceAfter=6),
}


def P(text, style="body"):
    return Paragraph(fx(text), S[style] if isinstance(style, str) else style)


def hand(text):
    return P(text, "hand")


def bullets(items, style="body"):
    return [P("&bull;&nbsp;&nbsp;" + i, ParagraphStyle("bl", parent=S[style], leftIndent=12, firstLineIndent=-10)) for i in items]


def code(text):
    t = Preformatted(text.strip("\n"), S["code"])
    tbl = Table([[t]], colWidths=[170 * mm])
    tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F1F5F9")),
        ("BOX", (0, 0), (-1, -1), 0.6, colors.HexColor("#94A3B8")),
        ("LEFTPADDING", (0, 0), (-1, -1), 8), ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return tbl


def box(kind, content, title=None, width=170 * mm):
    """A coloured Head First sidebar. content: str | list of flowables/str."""
    label, bg, fg = BOX[kind]
    items = [Paragraph(fx(title or label), ParagraphStyle("bt", parent=S["boxtitle"], textColor=fg))]
    if isinstance(content, str):
        content = [content]
    for c in content:
        items.append(P(c) if isinstance(c, str) else c)
    t = Table([[items]], colWidths=[width])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), bg),
        ("LINEBEFORE", (0, 0), (0, -1), 4, fg),
        ("BOX", (0, 0), (-1, -1), 0.5, fg),
        ("LEFTPADDING", (0, 0), (-1, -1), 10), ("RIGHTPADDING", (0, 0), (-1, -1), 10),
        ("TOPPADDING", (0, 0), (-1, -1), 7), ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
    ]))
    return [Spacer(1, 4), t, Spacer(1, 8)]


def dumb(qas):
    """There are no Dumb Questions: list of (q, a)."""
    content = []
    for q, a in qas:
        content.append(P("<b>Q:</b> " + q, "q"))
        content.append(P("<b>A:</b> " + a, "a"))
    return box("dumb", content)


def fireside(title, lines):
    """lines: list of (speaker, text)."""
    content = [P("<i>Tonight's talk:</i> " + title, "h3")]
    for who, text in lines:
        content.append(P(f"<b>{who}:</b> {text}", "small"))
    return box("fireside", content)


def table(rows, widths, header=True, zebra=True, head_bg=ACCENT):
    data = []
    for i, r in enumerate(rows):
        st = "cellb" if header and i == 0 else "cell"
        data.append([P(str(c), st) if not isinstance(c, Flowable) else c for c in r])
    t = Table(data, colWidths=[w * mm for w in widths], repeatRows=1 if header else 0)
    style = [
        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#CBD5E1")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]
    if header:
        style.append(("BACKGROUND", (0, 0), (-1, 0), head_bg))
    if zebra:
        for i in range(1 if header else 0, len(rows)):
            if i % 2 == 0:
                style.append(("BACKGROUND", (0, i), (-1, i), colors.HexColor("#F8FAFC")))
    t.setStyle(TableStyle(style))
    return [t, Spacer(1, 8)]


# ---------------------------------------------------------------- diagrams

def _box(d, x, y, w, h, fill, label, fs=9, font="Sans-Bold", fc=colors.white, stroke=None):
    d.add(Rect(x, y, w, h, rx=6, ry=6, fillColor=fill, strokeColor=stroke or fill, strokeWidth=1))
    lines = label.split("\n")
    widest = max(pdfmetrics.stringWidth(ln, font, fs) for ln in lines)
    if widest > w - 6:  # shrink text to fit the box
        fs = max(5.5, fs * (w - 6) / widest)
    total = len(lines) * (fs + 2)
    for i, ln in enumerate(lines):
        d.add(String(x + w / 2, y + h / 2 + total / 2 - (i + 1) * (fs + 2) + 3, ln, fontName=font, fontSize=fs,
                     fillColor=fc, textAnchor="middle"))


def _arrow(d, x1, y1, x2, y2, color=INK, width=1.4):
    d.add(Line(x1, y1, x2, y2, strokeColor=color, strokeWidth=width))
    import math
    ang = math.atan2(y2 - y1, x2 - x1)
    s = 6
    p1 = (x2 - s * math.cos(ang - 0.4), y2 - s * math.sin(ang - 0.4))
    p2 = (x2 - s * math.cos(ang + 0.4), y2 - s * math.sin(ang + 0.4))
    d.add(Polygon([x2, y2, p1[0], p1[1], p2[0], p2[1]], fillColor=color, strokeColor=color))


PALETTE = [colors.HexColor(c) for c in
           ["#0E7490", "#1D4ED8", "#6D28D9", "#BE185D", "#B45309", "#15803D", "#374151", "#B91C1C"]]


def stack_diagram(layers, notes=None, width=170 * mm, row_h=26, caption=None):
    """Vertical layered architecture; notes = handwritten comments on the right."""
    n = len(layers)
    h = n * (row_h + 8) + 10
    d = Drawing(width, h)
    bw = width * 0.55
    for i, name in enumerate(layers):
        y = h - (i + 1) * (row_h + 8)
        _box(d, 0, y, bw, row_h, PALETTE[i % len(PALETTE)], name, fs=9.5)
        if i < n - 1:
            _arrow(d, bw / 2, y, bw / 2, y - 7, MUTED, 1)
        if notes and i < len(notes) and notes[i]:
            d.add(Line(bw + 4, y + row_h / 2, bw + 18, y + row_h / 2, strokeColor=HAND, strokeWidth=0.8,
                       strokeDashArray=[2, 2]))
            d.add(String(bw + 22, y + row_h / 2 - 4, notes[i], fontName="Hand", fontSize=10.5, fillColor=HAND))
    out = [Spacer(1, 4), d]
    if caption:
        out.append(P(caption, "handc"))
    out.append(Spacer(1, 8))
    return out


def flow_diagram(nodes, width=170 * mm, box_h=34, labels=None, caption=None, color_offset=0):
    """Horizontal boxes joined by arrows; labels = text over each arrow."""
    n = len(nodes)
    gap = 18
    bw = (width - gap * (n - 1)) / n
    h = box_h + 26
    d = Drawing(width, h)
    for i, name in enumerate(nodes):
        x = i * (bw + gap)
        _box(d, x, 4, bw, box_h, PALETTE[(i + color_offset) % len(PALETTE)], name, fs=8)
        if i < n - 1:
            _arrow(d, x + bw + 1, 4 + box_h / 2, x + bw + gap - 1, 4 + box_h / 2)
            if labels and i < len(labels) and labels[i]:
                d.add(String(x + bw + gap / 2, box_h + 12, labels[i], fontName="Hand", fontSize=9,
                             fillColor=HAND, textAnchor="middle"))
    out = [Spacer(1, 4), d]
    if caption:
        out.append(P(caption, "handc"))
    out.append(Spacer(1, 6))
    return out


def bus_diagram(ecus, width=170 * mm, caption=None):
    """CAN bus with ECUs hanging off it. ecus = [(name, id_text)]"""
    h = 110
    d = Drawing(width, h)
    y1, y2 = 58, 50
    d.add(Line(10, y1, width - 10, y1, strokeColor=colors.HexColor("#B45309"), strokeWidth=2.5))
    d.add(Line(10, y2, width - 10, y2, strokeColor=colors.HexColor("#1D4ED8"), strokeWidth=2.5))
    d.add(String(12, y1 + 4, "CAN_H", fontName="Sans", fontSize=7, fillColor=INK))
    d.add(String(12, y2 - 10, "CAN_L", fontName="Sans", fontSize=7, fillColor=INK))
    for x in (4, width - 10):
        d.add(Rect(x, y2 - 4, 6, 16, fillColor=MUTED, strokeColor=MUTED))
    d.add(String(width - 40, y2 - 16, "120 Ω", fontName="Uni", fontSize=7, fillColor=INK))
    step = (width - 60) / len(ecus)
    for i, (name, ident) in enumerate(ecus):
        x = 40 + i * step
        top = i % 2 == 0
        by = 80 if top else 2
        _box(d, x, by, step - 14, 26, PALETTE[i % len(PALETTE)], f"{name}\n{ident}", fs=7.5)
        cx = x + (step - 14) / 2
        d.add(Line(cx, by if top else by + 26, cx, y1 if top else y2, strokeColor=INK, strokeWidth=1))
    out = [Spacer(1, 4), d]
    if caption:
        out.append(P(caption, "handc"))
    out.append(Spacer(1, 6))
    return out


def state_diagram(states, transitions, width=170 * mm, cols=4, caption=None):
    """Grid of states; transitions drawn as arrows between centres with handwritten labels."""
    rows = (len(states) + cols - 1) // cols
    bw, bh = (width - 20) / cols - 24, 30
    h = rows * 78 + 10
    d = Drawing(width, h)
    pos = {}
    for i, st in enumerate(states):
        r, c = divmod(i, cols)
        x = 10 + c * ((width - 20) / cols) + 12
        y = h - (r + 1) * 78 + 30
        pos[st] = (x, y)
        _box(d, x, y, bw, bh, PALETTE[i % len(PALETTE)], st, fs=7.5)
    for a, b, label in transitions:
        (x1, y1), (x2, y2) = pos[a], pos[b]
        sx, sy = x1 + bw / 2, y1 + bh / 2
        ex, ey = x2 + bw / 2, y2 + bh / 2
        # shorten to box edges
        import math
        L = math.hypot(ex - sx, ey - sy) or 1
        ux, uy = (ex - sx) / L, (ey - sy) / L
        k1 = min(bw / 2 / (abs(ux) + 1e-6), bh / 2 / (abs(uy) + 1e-6))
        _arrow(d, sx + ux * k1, sy + uy * k1, ex - ux * k1, ey - uy * k1, HAND, 1)
        d.add(String((sx + ex) / 2, (sy + ey) / 2 + 6, label, fontName="Hand", fontSize=8.5, fillColor=HAND,
                     textAnchor="middle"))
    out = [Spacer(1, 4), d]
    if caption:
        out.append(P(caption, "handc"))
    out.append(Spacer(1, 6))
    return out


def bits_diagram(fields, width=170 * mm, caption=None):
    """fields = [(label, bits_text, relative_width)]"""
    total = sum(f[2] for f in fields)
    h = 60
    d = Drawing(width, h)
    x = 0
    for i, (label, bits, w) in enumerate(fields):
        bw = width * w / total
        _box(d, x, 20, bw - 3, 30, PALETTE[i % len(PALETTE)], bits, fs=8.5, font="Mono-Bold")
        d.add(String(x + bw / 2, 6, label, fontName="Hand", fontSize=10, fillColor=HAND, textAnchor="middle"))
        x += bw
    out = [Spacer(1, 4), d]
    if caption:
        out.append(P(caption, "handc"))
    out.append(Spacer(1, 6))
    return out


# ---------------------------------------------------------------- document

class HFDoc(BaseDocTemplate):
    def __init__(self, path, title, subtitle, **kw):
        super().__init__(path, pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm, topMargin=20 * mm,
                         bottomMargin=18 * mm, title=title, author="Automotive Academy", subject=subtitle, **kw)
        self.doc_title = title
        frame = Frame(self.leftMargin, self.bottomMargin, self.width, self.height, id="f")
        self.addPageTemplates([
            PageTemplate("cover", [frame], onPage=self._cover_bg),
            PageTemplate("normal", [frame], onPage=self._decor),
        ])

    def _cover_bg(self, c, doc):
        w, h = A4
        c.saveState()
        c.setFillColor(colors.HexColor("#0B1220"))
        c.rect(0, h - 95 * mm, w, 95 * mm, fill=1, stroke=0)
        c.setFillColor(ACCENT)
        c.rect(0, h - 98 * mm, w, 3 * mm, fill=1, stroke=0)
        c.restoreState()

    def _decor(self, c, doc):
        w, h = A4
        c.saveState()
        c.setFillColor(ACCENT)
        c.rect(0, 0, 6 * mm, h, fill=1, stroke=0)
        c.setFont("Sans", 8)
        c.setFillColor(MUTED)
        c.drawString(20 * mm, 10 * mm, self.doc_title)
        c.drawRightString(w - 20 * mm, 10 * mm, f"page {doc.page}")
        c.setStrokeColor(colors.HexColor("#E5E7EB"))
        c.line(20 * mm, 14 * mm, w - 20 * mm, 14 * mm)
        c.restoreState()

    def afterFlowable(self, f):
        if isinstance(f, Paragraph):
            name = f.style.name
            if name == "h1":
                self.notify("TOCEntry", (0, f.getPlainText(), self.page))
            elif name == "h2":
                self.notify("TOCEntry", (1, f.getPlainText(), self.page))


def cover(title, subtitle, tagline, notes):
    """Cover page flowables. notes = handwritten call-outs."""
    from reportlab.platypus import NextPageTemplate
    story = [Spacer(1, 18 * mm),
             Paragraph(fx(title), ParagraphStyle("ct", fontName="Head", fontSize=34, leading=40, textColor=colors.white)),
             Spacer(1, 4 * mm),
             Paragraph(fx(subtitle), ParagraphStyle("cs", fontName="Sans", fontSize=14, leading=18,
                                                    textColor=colors.HexColor("#67E8F9"))),
             Spacer(1, 36 * mm),
             Paragraph(fx(tagline), ParagraphStyle("tg", fontName="Body-Italic", fontSize=13, leading=19, textColor=INK)),
             Spacer(1, 8 * mm)]
    for n in notes:
        story.append(Paragraph(fx("→ " + n), ParagraphStyle("cn", parent=S["hand"], fontSize=15, leading=21)))
    story += [Spacer(1, 12 * mm),
              P("A Head First-style learning guide that goes with the <b>Automotive Academy</b> sample app "
                "(Android Automotive OS, Kotlin, Jetpack Compose). Generated September 2026.", "small"),
              NextPageTemplate("normal"), PageBreak()]
    return story


def toc():
    t = TableOfContents()
    t.levelStyles = [S["toc1"], S["toc2"]]
    return [Paragraph("What's in this book", ParagraphStyle("tochead", parent=S["h2"])), t, PageBreak()]


def chapter(num, title, subtitle=None):
    out = [CondPageBreak(120 * mm)]
    out.append(Paragraph(fx(f"{num}  {title}") if num else fx(title), S["h1"]))
    if subtitle:
        out.append(P(subtitle, "hand"))
    out.append(Spacer(1, 4))
    return out


def build(path, title, subtitle, story):
    # keep h2/h3 headings with what follows them
    fixed = []
    for f in story:
        if isinstance(f, Paragraph) and f.style.name in ("h2", "h3"):
            fixed.append(CondPageBreak(45 * mm if f.style.name == "h2" else 30 * mm))
        fixed.append(f)
    story = fixed
    doc = HFDoc(path, title, subtitle)
    doc.multiBuild(story)


__all__ = ["P", "hand", "bullets", "code", "box", "dumb", "fireside", "table", "stack_diagram", "flow_diagram",
           "bus_diagram", "state_diagram", "bits_diagram", "cover", "toc", "chapter", "build", "Spacer", "PageBreak",
           "KeepTogether", "CondPageBreak", "mm", "S"]
