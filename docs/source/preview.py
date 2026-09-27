"""Dev helper: render the first N pages of a PDF to a contact sheet PNG (needs pymupdf + Pillow)."""
import sys
import pymupdf
from PIL import Image

pdf, out, start, n = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
doc = pymupdf.open(pdf)
ims = []
for i in range(start, min(start + n, len(doc))):
    pix = doc[i].get_pixmap(dpi=60)
    ims.append(Image.frombytes("RGB", (pix.width, pix.height), pix.samples))
w, h = ims[0].size
cols = 4
sheet = Image.new("RGB", (w * cols, h * ((len(ims) + cols - 1) // cols)), "white")
for i, im in enumerate(ims):
    sheet.paste(im, ((i % cols) * w, (i // cols) * h))
sheet.save(out)
print(len(doc), "pages")
