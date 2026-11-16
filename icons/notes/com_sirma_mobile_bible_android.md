# Bible (YouVersion)
Checked the earlier draft against the Play Store icon and the original: the red-brown book (gradient #C73D34 to #7C2729), cream "HOLY BIBLE" lettering, cream page edge and red ribbon all match. The book is the object (R8), so its rounded-corner body stays and nothing sits behind it. Lettering is vector paths, sharp. Kept as is.
Source: redrawn from the Play Store icon (/home/claude/refs/com_sirma_mobile_bible_android/play.png).

Fix (v6): Arch said the rounding should line up with the book insides, like the original. Cover corner radius is now 80, and the cream page block is a pill whose end curves (radius 40) share the cover's corner centres, inset 40 from the edges, so the curves sit evenly around each other as in the original (legacy.png). Right page tip kept; page right edge now curves down concentric with the cover's bottom-right corner.
