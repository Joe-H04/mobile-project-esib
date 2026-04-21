from pathlib import Path
from xml.sax.saxutils import escape
from zipfile import ZIP_DEFLATED, ZipFile


OUT = Path("presentation/BetNow_2_Minute_Pitch.pptx")

SLIDE_W = 12192000
SLIDE_H = 6858000


def emu(x_in):
    return int(x_in * 914400)


def xml(text):
    return escape(text, {'"': "&quot;"})


def text_shape(shape_id, name, x, y, w, h, paragraphs, align="l"):
    ps = []
    for para in paragraphs:
        text = para["text"] if isinstance(para, dict) else str(para)
        size = para.get("size", 24) if isinstance(para, dict) else 24
        bold = para.get("bold", False) if isinstance(para, dict) else False
        color = para.get("color", "111827") if isinstance(para, dict) else "111827"
        space_after = para.get("space_after", 0) if isinstance(para, dict) else 0
        ps.append(
            f"""
            <a:p>
              <a:pPr algn="{align}" spcAft="{space_after}"/>
              <a:r>
                <a:rPr lang="en-US" sz="{size * 100}" b="{1 if bold else 0}">
                  <a:solidFill><a:srgbClr val="{color}"/></a:solidFill>
                </a:rPr>
                <a:t>{xml(text)}</a:t>
              </a:r>
              <a:endParaRPr lang="en-US" sz="{size * 100}"/>
            </a:p>
            """
        )
    return f"""
    <p:sp>
      <p:nvSpPr>
        <p:cNvPr id="{shape_id}" name="{xml(name)}"/>
        <p:cNvSpPr txBox="1"/>
        <p:nvPr/>
      </p:nvSpPr>
      <p:spPr>
        <a:xfrm>
          <a:off x="{x}" y="{y}"/>
          <a:ext cx="{w}" cy="{h}"/>
        </a:xfrm>
        <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
        <a:noFill/>
        <a:ln><a:noFill/></a:ln>
      </p:spPr>
      <p:txBody>
        <a:bodyPr wrap="square" lIns="0" tIns="0" rIns="0" bIns="0" anchor="t"/>
        <a:lstStyle/>
        {''.join(ps)}
      </p:txBody>
    </p:sp>
    """


def rect(shape_id, name, x, y, w, h, fill, line=None):
    line_xml = (
        f'<a:ln><a:solidFill><a:srgbClr val="{line}"/></a:solidFill></a:ln>'
        if line
        else "<a:ln><a:noFill/></a:ln>"
    )
    return f"""
    <p:sp>
      <p:nvSpPr>
        <p:cNvPr id="{shape_id}" name="{xml(name)}"/>
        <p:cNvSpPr/>
        <p:nvPr/>
      </p:nvSpPr>
      <p:spPr>
        <a:xfrm>
          <a:off x="{x}" y="{y}"/>
          <a:ext cx="{w}" cy="{h}"/>
        </a:xfrm>
        <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
        <a:solidFill><a:srgbClr val="{fill}"/></a:solidFill>
        {line_xml}
      </p:spPr>
    </p:sp>
    """


def slide(title, subtitle, bullets, cue, accent="10B981"):
    shapes = []
    sid = 2
    shapes.append(rect(sid, "Background", 0, 0, SLIDE_W, SLIDE_H, "F8FAFC"))
    sid += 1
    shapes.append(rect(sid, "Accent", 0, 0, emu(0.18), SLIDE_H, accent))
    sid += 1
    shapes.append(
        text_shape(
            sid,
            "Title",
            emu(0.75),
            emu(0.55),
            emu(11.6),
            emu(0.75),
            [{"text": title, "size": 36, "bold": True, "color": "111827"}],
        )
    )
    sid += 1
    if subtitle:
        shapes.append(
            text_shape(
                sid,
                "Subtitle",
                emu(0.78),
                emu(1.35),
                emu(11.2),
                emu(0.45),
                [{"text": subtitle, "size": 18, "color": "4B5563"}],
            )
        )
        sid += 1
    body = [{"text": f"- {item}", "size": 25, "color": "111827", "space_after": 5000} for item in bullets]
    shapes.append(text_shape(sid, "Bullets", emu(1.0), emu(2.15), emu(10.9), emu(2.9), body))
    sid += 1
    shapes.append(rect(sid, "Cue box", emu(0.78), emu(5.82), emu(11.7), emu(0.75), "E5E7EB"))
    sid += 1
    shapes.append(
        text_shape(
            sid,
            "Speaker cue",
            emu(1.0),
            emu(6.0),
            emu(11.2),
            emu(0.35),
            [{"text": f"Say: {cue}", "size": 14, "color": "374151"}],
        )
    )

    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
       xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:cNvPr id="1" name=""/>
        <p:cNvGrpSpPr/>
        <p:nvPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr>
        <a:xfrm>
          <a:off x="0" y="0"/>
          <a:ext cx="0" cy="0"/>
          <a:chOff x="0" y="0"/>
          <a:chExt cx="0" cy="0"/>
        </a:xfrm>
      </p:grpSpPr>
      {''.join(shapes)}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>
"""


def write_pptx():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    slides = [
        slide(
            "BetNow",
            "A native Android app for prediction markets",
            [
                "Create an account",
                "Browse live YES/NO markets",
                "Place bets and track results",
            ],
            "BetNow turns prediction markets into a simple mobile flow: discover, decide, bet, and track.",
            "10B981",
        ),
        slide(
            "The User Flow",
            "One complete path from login to portfolio",
            [
                "Search, filter, and sort markets",
                "Open details and preview shares",
                "Watchlist, My Bets, Profile, Leaderboard",
            ],
            "The app is not just one betting screen; it covers the full user journey around a market.",
            "2563EB",
        ),
        slide(
            "How It Works",
            "Fragment -> ViewModel -> Repository -> Retrofit/Socket -> Backend",
            [
                "Kotlin, XML, Material UI, LiveData",
                "RecyclerView, DiffUtil, Glide",
                "Express backend, JWT, Socket.io",
            ],
            "We separated UI, state, data access, and backend logic so the code is easier to explain and maintain.",
            "F59E0B",
        ),
        slide(
            "Why It Matters",
            "A real mobile-development course project",
            [
                "Lifecycle-aware screens and sockets",
                "Loading, error, and empty states",
                "Demo: login -> markets -> detail -> bet",
            ],
            "The value is that it demonstrates practical Android architecture, not only a static interface.",
            "EF4444",
        ),
    ]

    content_types = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  <Override PartName="/ppt/presProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presProps+xml"/>
  <Override PartName="/ppt/viewProps.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.viewProps+xml"/>
  <Override PartName="/ppt/tableStyles.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.tableStyles+xml"/>
  {''.join(f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>' for i in range(1, len(slides) + 1))}
</Types>
"""

    root_rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>
"""

    slide_ids = "\n".join(
        f'<p:sldId id="{255 + i}" r:id="rId{i + 1}"/>'
        for i in range(1, len(slides) + 1)
    )
    presentation = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
                xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
                xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst>
    <p:sldMasterId id="2147483648" r:id="rId1"/>
  </p:sldMasterIdLst>
  <p:sldIdLst>{slide_ids}</p:sldIdLst>
  <p:sldSz cx="{SLIDE_W}" cy="{SLIDE_H}" type="wide"/>
  <p:notesSz cx="6858000" cy="9144000"/>
  <p:defaultTextStyle/>
</p:presentation>
"""

    pres_rels = [
        '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>'
    ]
    for i in range(1, len(slides) + 1):
        pres_rels.append(
            f'<Relationship Id="rId{i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide{i}.xml"/>'
        )
    pres_rels_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  {' '.join(pres_rels)}
</Relationships>
"""

    slide_master = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
    </p:spTree>
  </p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
  <p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles>
</p:sldMaster>
"""

    slide_master_rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>
"""

    slide_layout = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
             xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
             xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
             type="blank" preserve="1">
  <p:cSld name="Blank">
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>
"""

    slide_layout_rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>
"""

    theme = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="BetNow">
  <a:themeElements>
    <a:clrScheme name="BetNow">
      <a:dk1><a:srgbClr val="111827"/></a:dk1>
      <a:lt1><a:srgbClr val="F8FAFC"/></a:lt1>
      <a:dk2><a:srgbClr val="1F2937"/></a:dk2>
      <a:lt2><a:srgbClr val="E5E7EB"/></a:lt2>
      <a:accent1><a:srgbClr val="10B981"/></a:accent1>
      <a:accent2><a:srgbClr val="2563EB"/></a:accent2>
      <a:accent3><a:srgbClr val="F59E0B"/></a:accent3>
      <a:accent4><a:srgbClr val="EF4444"/></a:accent4>
      <a:accent5><a:srgbClr val="14B8A6"/></a:accent5>
      <a:accent6><a:srgbClr val="64748B"/></a:accent6>
      <a:hlink><a:srgbClr val="2563EB"/></a:hlink>
      <a:folHlink><a:srgbClr val="7C3AED"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="BetNow">
      <a:majorFont><a:latin typeface="Aptos Display"/></a:majorFont>
      <a:minorFont><a:latin typeface="Aptos"/></a:minorFont>
    </a:fontScheme>
    <a:fmtScheme name="BetNow">
      <a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst>
      <a:lnStyleLst><a:ln w="9525"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst>
      <a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst>
      <a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst>
    </a:fmtScheme>
  </a:themeElements>
</a:theme>
"""

    pres_props = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:presentationPr xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"/>"""
    view_props = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:viewPr xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"/>"""
    table_styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><a:tblStyleLst xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" def="{5C22544A-7EE6-4342-B048-85BDC9FD1C3A}"/>"""
    slide_rel = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>
"""

    with ZipFile(OUT, "w", ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types)
        z.writestr("_rels/.rels", root_rels)
        z.writestr("ppt/presentation.xml", presentation)
        z.writestr("ppt/_rels/presentation.xml.rels", pres_rels_xml)
        z.writestr("ppt/slideMasters/slideMaster1.xml", slide_master)
        z.writestr("ppt/slideMasters/_rels/slideMaster1.xml.rels", slide_master_rels)
        z.writestr("ppt/slideLayouts/slideLayout1.xml", slide_layout)
        z.writestr("ppt/slideLayouts/_rels/slideLayout1.xml.rels", slide_layout_rels)
        z.writestr("ppt/theme/theme1.xml", theme)
        z.writestr("ppt/presProps.xml", pres_props)
        z.writestr("ppt/viewProps.xml", view_props)
        z.writestr("ppt/tableStyles.xml", table_styles)
        for i, s in enumerate(slides, start=1):
            z.writestr(f"ppt/slides/slide{i}.xml", s)
            z.writestr(f"ppt/slides/_rels/slide{i}.xml.rels", slide_rel)


if __name__ == "__main__":
    write_pptx()
