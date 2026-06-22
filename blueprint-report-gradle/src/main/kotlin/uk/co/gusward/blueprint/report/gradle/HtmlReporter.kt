package uk.co.gusward.blueprint.report.gradle

import java.io.File
import java.util.Base64

object HtmlReporter {

    fun generate(captureDir: File, entries: List<ManifestEntry>): String {
        val byPkg = entries.groupBy { it.pkg }.toSortedMap()
        val topLevelPackages = byPkg.keys
            .map { it.split(".").take(3).joinToString(".") }
            .distinct()
            .sorted()

        // Sequential index in display order (matches visual order in HTML)
        val entryIndex = mutableMapOf<String, Int>()
        var counter = 0
        topLevelPackages.forEach { topPkg ->
            val subPkgs = byPkg.keys.filter { it == topPkg || it.startsWith("$topPkg.") }.sorted()
            subPkgs.forEach { pkg ->
                byPkg[pkg]?.forEach { entry -> entryIndex[entry.sourceFqn] = ++counter }
            }
        }

        val sectionsHtml = topLevelPackages.joinToString("\n") { topPkg ->
            val subPkgs = byPkg.keys.filter { it == topPkg || it.startsWith("$topPkg.") }.sorted()
            val totalCount = subPkgs.sumOf { byPkg[it]?.size ?: 0 }
            buildTopLevelSection(topPkg, subPkgs, byPkg, captureDir, totalCount, entryIndex)
        }

        val sidebarItems = topLevelPackages.joinToString("\n") { topPkg ->
            val subPkgs = byPkg.keys.filter { it == topPkg || it.startsWith("$topPkg.") }.sorted()
            buildSidebarItems(topPkg, subPkgs, byPkg, entryIndex)
        }

        return buildHtml(sectionsHtml, sidebarItems, entries.size)
    }

    private fun buildTopLevelSection(
        topPkg: String,
        subPkgs: List<String>,
        byPkg: Map<String, List<ManifestEntry>>,
        captureDir: File,
        totalCount: Int,
        entryIndex: Map<String, Int>
    ): String {
        val subSections = subPkgs.joinToString("\n") { pkg ->
            val pkgEntries = byPkg[pkg] ?: return@joinToString ""
            val shortName = if (pkg == topPkg) pkg else pkg.removePrefix("$topPkg.")
            val rows = pkgEntries.joinToString("\n") { entry ->
                buildPreviewRow(entry, captureDir, entryIndex[entry.sourceFqn] ?: 0)
            }
            """
<div class="sub-pkg-block">
  <div class="sub-pkg-header" onclick="toggleSection(this)">
    <span class="chevron">&#9660;</span>
    <span class="sub-pkg-name">$shortName</span>
    <span class="sub-pkg-count">${pkgEntries.size}</span>
  </div>
  <div class="sub-pkg-body">$rows</div>
</div>"""
        }
        return """
<div class="pkg-block">
  <div class="pkg-header" onclick="toggleSection(this)">
    <span class="chevron pkg-chevron">&#9660;</span>
    <span class="pkg-name-main">$topPkg</span>
    <span class="pkg-count-badge">$totalCount</span>
  </div>
  <div class="pkg-body">$subSections</div>
</div>"""
    }

    private fun buildPreviewRow(entry: ManifestEntry, captureDir: File, index: Int): String {
        val pkgPath  = entry.pkg.replace('.', '/')
        val funcName = entry.sourceFqn.substringAfterLast('.')
        val plainPng = captureDir.resolve("$pkgPath/${funcName}_plain.png")
        val bpPng    = captureDir.resolve("$pkgPath/${funcName}_blueprint.png")

        val plainSrc = if (plainPng.exists()) "data:image/png;base64,${encode(plainPng)}" else ""
        val bpSrc    = if (bpPng.exists())    "data:image/png;base64,${encode(bpPng)}"    else ""

        val plainBlock = if (plainSrc.isNotEmpty())
            """<img class="ss-img" src="$plainSrc" alt="Preview">"""
        else
            """<div class="ss-missing">&#8212;</div>"""

        val bpBlock = if (bpSrc.isNotEmpty())
            """<img class="ss-img" src="$bpSrc" alt="Blueprint">"""
        else
            """<div class="ss-missing">&#8212;</div>"""

        val displayName = if (entry.previewName.isNotEmpty()) "${entry.previewName} ($funcName)" else funcName
        val safeName = displayName
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

        return """
<div class="preview-card" id="preview-$index" data-name="$safeName">
  <div class="preview-card-header">
    <span class="preview-num">#$index</span>
    <span class="preview-name">$safeName</span>
  </div>
  <div class="preview-shots">
    <div class="shot-block">
      $plainBlock
      <div class="shot-caption">Preview</div>
    </div>
    <div class="shot-block">
      $bpBlock
      <div class="shot-caption">Blueprint</div>
    </div>
  </div>
</div>"""
    }

    private fun buildSidebarItems(
        topPkg: String,
        subPkgs: List<String>,
        byPkg: Map<String, List<ManifestEntry>>,
        entryIndex: Map<String, Int>
    ): String {
        val total = subPkgs.sumOf { byPkg[it]?.size ?: 0 }
        val children = subPkgs.joinToString("\n") { pkg ->
            val pkgEntries = byPkg[pkg] ?: return@joinToString ""
            val count = pkgEntries.size
            val shortName = if (pkg == topPkg) pkg else pkg.removePrefix("$topPkg.")
            val previews = pkgEntries.joinToString("\n") { entry ->
                val idx = entryIndex[entry.sourceFqn] ?: 0
                val funcName = entry.sourceFqn.substringAfterLast('.')
                val displayName = if (entry.previewName.isNotEmpty()) "${entry.previewName} ($funcName)" else funcName
                val safeName = displayName.replace("&", "&amp;").replace("<", "&lt;")
                """<div class="tree-l2" onclick="scrollToPreview($idx)"><span class="tree-idx">#$idx</span><span class="tree-l2-label">$safeName</span></div>"""
            }
            """
<div class="tree-l1-group">
  <div class="tree-l1" onclick="toggleSidebarGroup(this)">
    <span class="side-chevron">&#9660;</span><span class="tree-l1-name">$shortName</span><span class="tree-count">$count</span>
  </div>
  <div class="tree-l1-body">$previews</div>
</div>"""
        }
        return """
<div class="tree-l0-group">
  <div class="tree-l0" onclick="toggleSidebarGroup(this)">
    <span class="side-chevron">&#9660;</span><span class="tree-l0-name">$topPkg</span><span class="tree-count">$total</span>
  </div>
  <div class="tree-l0-body">$children</div>
</div>"""
    }

    private fun encode(file: File): String = Base64.getEncoder().encodeToString(file.readBytes())

    @Suppress("LongMethod")
    private fun buildHtml(sections: String, sidebarItems: String, total: Int): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Blueprint Report</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#F5F7FA;display:flex;height:100vh;overflow:hidden;color:#1a1a2e}
.sidebar{width:280px;min-width:160px;max-width:520px;background:#0D1B2A;display:flex;flex-direction:column;height:100vh;overflow:hidden;flex-shrink:0}
.resize-handle{width:4px;background:#1A2B3C;cursor:col-resize;flex-shrink:0;transition:background .15s}
.resize-handle:hover,.resize-handle.dragging{background:#1565C0}
.sidebar-header{padding:20px 16px 16px;border-bottom:1px solid rgba(255,255,255,.08)}
.sidebar-logo{display:flex;align-items:center;gap:10px;margin-bottom:14px}
.logo-icon{width:28px;height:28px;background:#1565C0;border-radius:6px;position:relative;flex-shrink:0}
.logo-icon::before{content:'';position:absolute;inset:4px;border:1.5px solid rgba(255,255,255,.9)}
.logo-text{font-size:15px;font-weight:700;color:#fff;letter-spacing:.3px}
.logo-sub{font-size:11px;color:#546E7A;font-family:'Courier New',monospace}
.search-box{width:100%;background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.1);border-radius:6px;padding:7px 10px;color:#90CAF9;font-size:13px;outline:none;font-family:inherit}
.search-box::placeholder{color:#4a5568}
.sidebar-tree{flex:1;overflow-y:auto;padding:8px 0}
.sidebar-tree::-webkit-scrollbar{width:4px}
.sidebar-tree::-webkit-scrollbar-thumb{background:rgba(255,255,255,.1);border-radius:2px}
.tree-section-label{padding:4px 16px 8px;font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:.8px;color:#37474F}
.tree-l0-group,.tree-l1-group{margin-bottom:1px}
.tree-l0{padding:8px 14px;font-size:13px;font-family:'Courier New',monospace;cursor:pointer;display:flex;align-items:center;gap:6px;color:#90CAF9;font-weight:700;user-select:none}
.tree-l0:hover{background:rgba(255,255,255,.06)}
.tree-l0-body,.tree-l1-body{overflow:hidden}
.tree-l1{padding:6px 14px 6px 26px;font-size:12px;font-family:'Courier New',monospace;cursor:pointer;display:flex;align-items:center;gap:5px;color:#78909C;user-select:none}
.tree-l1:hover{background:rgba(255,255,255,.04);color:#90CAF9}
.tree-l2{padding:5px 14px 5px 40px;font-size:11px;cursor:pointer;display:flex;align-items:baseline;gap:5px;color:#546E7A;user-select:none}
.tree-l2:hover{background:rgba(255,255,255,.04);color:#7CB9E8}
.tree-l0-name,.tree-l1-name{flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.tree-l2-label{flex:1;line-height:1.4}
.side-chevron{font-size:14px;flex-shrink:0;display:inline-block;color:#546E7A;width:14px;text-align:center}
.tree-count{margin-left:auto;background:rgba(255,255,255,.07);border-radius:10px;padding:1px 6px;font-size:10px;color:#546E7A;font-family:sans-serif;flex-shrink:0}
.tree-idx{font-size:10px;color:#37474F;flex-shrink:0;min-width:26px;font-family:'Courier New',monospace}
.sidebar-footer{padding:12px 16px;border-top:1px solid rgba(255,255,255,.06);font-size:11px;color:#37474F;font-family:'Courier New',monospace}
.main{flex:1;overflow-y:auto;padding:24px 28px}
.main::-webkit-scrollbar{width:6px}
.main::-webkit-scrollbar-thumb{background:#CBD5E0;border-radius:3px}
.pkg-block{margin-bottom:16px}
.pkg-header{display:flex;align-items:center;gap:10px;padding:11px 16px;background:#fff;border:1px solid #E0E8F0;border-radius:10px;cursor:pointer;user-select:none}
.pkg-header:hover{background:#F0F4F8}
.chevron{font-size:16px;flex-shrink:0;color:#546E7A;width:16px;text-align:center;display:inline-block}
.pkg-chevron{color:#1565C0;font-size:18px}
.pkg-name-main{font-family:'Courier New',monospace;font-size:14px;font-weight:700;color:#1565C0;flex:1}
.pkg-count-badge{font-size:12px;color:#9e9e9e;background:#EEF2FF;border-radius:10px;padding:2px 10px;flex-shrink:0}
.pkg-body{padding-left:16px;margin-top:8px;border-left:2px solid #E0E8F0;margin-left:12px}
.sub-pkg-block{margin-bottom:10px}
.sub-pkg-header{display:flex;align-items:center;gap:8px;padding:9px 14px;background:#F8FAFC;border:1px solid #E8EDF2;border-radius:8px;cursor:pointer;user-select:none}
.sub-pkg-header:hover{background:#EEF2F7}
.sub-pkg-name{font-family:'Courier New',monospace;font-size:13px;font-weight:700;color:#1976D2;flex:1}
.sub-pkg-count{font-size:11px;color:#9e9e9e;background:#EEF2FF;border-radius:10px;padding:1px 8px;flex-shrink:0}
.sub-pkg-body{padding:8px 0 4px;display:flex;flex-direction:column;gap:10px}
.preview-card{background:#fff;border:1px solid #E8EDF2;border-radius:10px;overflow:hidden;transition:box-shadow .15s}
.preview-card:hover{box-shadow:0 4px 16px rgba(13,27,42,.08)}
.preview-card.highlight{outline:3px solid #1565C0;outline-offset:-1px}
.preview-card-header{display:flex;align-items:center;gap:10px;padding:12px 16px;border-bottom:1px solid #F0F4F8;background:#FAFBFF}
.preview-num{font-family:'Courier New',monospace;font-size:12px;color:#90CAF9;font-weight:700;background:#0D1B2A;padding:2px 8px;border-radius:4px;flex-shrink:0}
.preview-name{font-family:'Courier New',monospace;font-size:13px;font-weight:700;color:#1a1a2e}
.preview-shots{display:flex;gap:20px;padding:16px;justify-content:flex-start;flex-wrap:wrap}
.shot-block{display:flex;flex-direction:column;align-items:center;gap:8px}
.ss-img{max-height:400px;max-width:320px;width:auto;height:auto;border:1px solid #E0E0E0;border-radius:8px;background:#FAFAFA;display:block;cursor:pointer;transition:opacity .15s}
.ss-img:hover{opacity:.82;border-color:#90CAF9}
.ss-missing{height:100px;width:160px;background:#F5F5F5;border:1px dashed #DDD;border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:19px;color:#CCC}
.shot-caption{font-size:11px;text-transform:uppercase;letter-spacing:.7px;color:#9e9e9e;font-weight:600}
.modal-overlay{display:none;position:fixed;inset:0;background:rgba(0,0,0,.82);z-index:1000;align-items:center;justify-content:center}
.modal-overlay.open{display:flex}
.modal-box{background:#1A2332;border-radius:14px;width:min(94vw,1100px);max-height:94vh;display:flex;flex-direction:column;overflow:hidden;box-shadow:0 32px 80px rgba(0,0,0,.7)}
.modal-header{display:flex;align-items:center;gap:12px;padding:14px 20px;border-bottom:1px solid rgba(255,255,255,.08);flex-shrink:0}
.modal-title{font-family:'Courier New',monospace;font-size:14px;font-weight:700;color:#90CAF9;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.modal-close{background:none;border:none;color:#546E7A;font-size:21px;cursor:pointer;padding:2px 8px;border-radius:4px;line-height:1;flex-shrink:0}
.modal-close:hover{background:rgba(255,255,255,.08);color:#fff}
.modal-shots{display:flex;gap:16px;padding:16px;flex:1;overflow:hidden;min-height:0}
.modal-shot{flex:1;display:flex;flex-direction:column;gap:8px;min-width:0}
.modal-img-wrap{flex:1;display:flex;align-items:center;justify-content:center;background:#0D1B2A;border-radius:8px;overflow:hidden;cursor:crosshair;min-height:200px}
.modal-img-wrap img{max-width:100%;max-height:100%;object-fit:contain;transform-origin:50% 50%;user-select:none;display:block}
.modal-shot .shot-caption{color:#546E7A;flex-shrink:0;text-align:center;width:100%}
.modal-footer{display:flex;align-items:center;justify-content:flex-end;padding:10px 18px;border-top:1px solid rgba(255,255,255,.08);flex-shrink:0}
.zoom-hint{font-size:13px;color:#546E7A;font-family:'Courier New',monospace;background:rgba(255,255,255,.05);border:1px solid rgba(255,255,255,.08);border-radius:6px;padding:5px 12px;user-select:none}
</style>
</head>
<body>
<aside class="sidebar">
  <div class="sidebar-header">
    <div class="sidebar-logo">
      <div class="logo-icon"></div>
      <div><div class="logo-text">Blueprint Report</div><div class="logo-sub">uk.co.gusward</div></div>
    </div>
    <input class="search-box" placeholder="Filter previews&#x2026;" type="text">
  </div>
  <nav class="sidebar-tree">
    <div class="tree-section-label">Packages</div>
    $sidebarItems
  </nav>
  <div class="sidebar-footer">$total previews total</div>
</aside>
<div class="resize-handle" id="resize-handle"></div>
<main class="main">$sections</main>

<div id="modal" class="modal-overlay">
  <div class="modal-box">
    <div class="modal-header">
      <span id="modal-title" class="modal-title"></span>
      <button class="modal-close" onclick="closeModal()">&#x2715;</button>
    </div>
    <div class="modal-shots">
      <div class="modal-shot">
        <div class="modal-img-wrap" id="wrap-plain"><img id="modal-plain" src="data:," alt="Preview"></div>
        <div class="shot-caption">Preview</div>
      </div>
      <div class="modal-shot">
        <div class="modal-img-wrap" id="wrap-bp"><img id="modal-bp" src="data:," alt="Blueprint"></div>
        <div class="shot-caption">Blueprint</div>
      </div>
    </div>
    <div class="modal-footer">
      <span id="zoom-hint" class="zoom-hint">Pinch to zoom &nbsp; 100%</span>
    </div>
  </div>
</div>

<script>
var zoomLevel = 1;
var panX = 0, panY = 0;
var isDragging = false, wasDragging = false, dragStartX = 0, dragStartY = 0, panStartX = 0, panStartY = 0;

function applyTransform() {
  document.querySelectorAll('.modal-img-wrap img').forEach(function(img) {
    img.style.transformOrigin = '50% 50%';
    img.style.transform = 'translate(' + panX + 'px,' + panY + 'px) scale(' + zoomLevel + ')';
  });
  var cur = zoomLevel > 1 ? 'grab' : 'crosshair';
  document.querySelectorAll('.modal-img-wrap').forEach(function(w) { w.style.cursor = cur; });
}

function toggleSidebarGroup(el) {
  var body = el.nextElementSibling;
  var ch = el.querySelector('.side-chevron');
  if (!body) return;
  var open = body.style.display !== 'none';
  body.style.display = open ? 'none' : '';
  if (ch) ch.innerHTML = open ? '&#9654;' : '&#9660;';
}

function toggleSection(el) {
  var block = el.parentElement;
  var body = block.querySelector('.pkg-body,.sub-pkg-body');
  var ch = el.querySelector('.chevron');
  if (!body) return;
  var open = body.style.display !== 'none';
  body.style.display = open ? 'none' : '';
  if (ch) ch.innerHTML = open ? '&#9654;' : '&#9660;';
}

function scrollToPreview(idx) {
  var el = document.getElementById('preview-' + idx);
  if (!el) return;
  // Expand any collapsed ancestor sections in main content
  var node = el.parentElement;
  while (node) {
    if ((node.classList.contains('pkg-body') || node.classList.contains('sub-pkg-body'))
        && node.style.display === 'none') {
      node.style.display = '';
      var header = node.previousElementSibling;
      if (header) {
        var ch = header.querySelector('.chevron');
        if (ch) ch.innerHTML = '&#9660;';
      }
    }
    node = node.parentElement;
  }
  el.scrollIntoView({behavior: 'smooth', block: 'start'});
  el.classList.add('highlight');
  setTimeout(function() { el.classList.remove('highlight'); }, 1400);
}

function openModal(plainSrc, bpSrc, name) {
  document.getElementById('modal-plain').src = plainSrc || 'data:,';
  document.getElementById('modal-bp').src = bpSrc || 'data:,';
  document.getElementById('modal-title').textContent = name;
  document.getElementById('modal').classList.add('open');
  resetZoom();
}

function closeModal() {
  document.getElementById('modal').classList.remove('open');
}

function resetZoom() {
  zoomLevel = 1; panX = 0; panY = 0; isDragging = false;
  applyTransform();
  updateZoomHint();
}

function updateZoomHint() {
  var h = document.getElementById('zoom-hint');
  if (h) h.textContent = 'Pinch to zoom   ' + Math.round(zoomLevel * 100) + '%';
}

document.addEventListener('DOMContentLoaded', function() {
  // Sidebar resize
  var sidebar = document.querySelector('.sidebar');
  var handle = document.getElementById('resize-handle');
  var resizing = false, resizeStartX = 0, resizeStartW = 0;
  handle.addEventListener('mousedown', function(e) {
    resizing = true; resizeStartX = e.clientX; resizeStartW = sidebar.offsetWidth;
    handle.classList.add('dragging');
    document.body.style.userSelect = 'none';
    document.body.style.cursor = 'col-resize';
    e.preventDefault();
  });
  document.addEventListener('mousemove', function(e) {
    if (!resizing) return;
    var w = Math.max(160, Math.min(520, resizeStartW + (e.clientX - resizeStartX)));
    sidebar.style.width = w + 'px';
  });
  document.addEventListener('mouseup', function() {
    if (!resizing) return;
    resizing = false;
    handle.classList.remove('dragging');
    document.body.style.userSelect = '';
    document.body.style.cursor = '';
  });

  // Image click -> modal
  document.addEventListener('click', function(e) {
    var img = e.target.closest ? e.target.closest('.ss-img') : null;
    if (!img) return;
    var card = img.closest('.preview-card');
    if (!card) return;
    var imgs = card.querySelectorAll('.ss-img');
    openModal(
      imgs[0] ? imgs[0].src : '',
      imgs[1] ? imgs[1].src : '',
      card.dataset.name || ''
    );
  });

  // Close on backdrop click — but not when releasing a pan drag
  document.getElementById('modal').addEventListener('click', function(e) {
    if (wasDragging) { wasDragging = false; return; }
    if (e.target === this) closeModal();
  });

  // Escape key
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeModal();
  });

  // Scroll-wheel zoom on modal image wrappers
  document.querySelectorAll('.modal-img-wrap').forEach(function(wrap) {
    wrap.addEventListener('wheel', function(e) {
      e.preventDefault();
      var newZoom = Math.max(1, Math.min(8, zoomLevel * (e.deltaY < 0 ? 1.10 : 0.81)));
      var rect = wrap.getBoundingClientRect();
      // Cursor relative to wrap center (where transform-origin lives)
      var dx = e.clientX - rect.left - rect.width / 2;
      var dy = e.clientY - rect.top - rect.height / 2;
      // Image-space point under cursor
      var ix = (dx - panX) / zoomLevel;
      var iy = (dy - panY) / zoomLevel;
      // New pan keeps that image point under the cursor
      panX = dx - ix * newZoom;
      panY = dy - iy * newZoom;
      zoomLevel = newZoom;
      if (zoomLevel === 1) { panX = 0; panY = 0; }
      applyTransform();
      updateZoomHint();
    }, {passive: false});

    wrap.addEventListener('mousedown', function(e) {
      if (zoomLevel <= 1) return;
      isDragging = true;
      dragStartX = e.clientX; dragStartY = e.clientY;
      panStartX = panX; panStartY = panY;
      wrap.style.cursor = 'grabbing';
      e.preventDefault();
    });
  });

  document.addEventListener('mousemove', function(e) {
    if (!isDragging) return;
    panX = panStartX + (e.clientX - dragStartX);
    panY = panStartY + (e.clientY - dragStartY);
    applyTransform();
  });

  document.addEventListener('mouseup', function() {
    if (!isDragging) return;
    isDragging = false;
    wasDragging = true;  // suppress the click that fires after mouseup
    document.querySelectorAll('.modal-img-wrap').forEach(function(w) {
      w.style.cursor = zoomLevel > 1 ? 'grab' : 'crosshair';
    });
  });

  // Filter
  var sb = document.querySelector('.search-box');
  if (sb) sb.addEventListener('input', function() {
    var q = this.value.toLowerCase();

    // Main content cards
    document.querySelectorAll('.preview-card').forEach(function(card) {
      var match = !q || (card.dataset.name || '').toLowerCase().indexOf(q) !== -1;
      card.style.display = match ? '' : 'none';
    });

    if (!q) {
      // Clear filter: restore all sidebar items to visible (collapse state resets to open)
      document.querySelectorAll('.tree-l0-group,.tree-l1-group,.tree-l2,.tree-l0-body,.tree-l1-body').forEach(function(el) {
        el.style.display = '';
      });
      return;
    }

    // Sidebar l2 items: match on label text
    document.querySelectorAll('.tree-l2').forEach(function(item) {
      var match = item.textContent.toLowerCase().indexOf(q) !== -1;
      item.style.display = match ? '' : 'none';
    });

    // l1 groups: visible if any child l2 matches; expand body so matches show
    document.querySelectorAll('.tree-l1-group').forEach(function(grp) {
      var visible = Array.from(grp.querySelectorAll('.tree-l2')).some(function(el) {
        return el.style.display !== 'none';
      });
      grp.style.display = visible ? '' : 'none';
      var body = grp.querySelector('.tree-l1-body');
      if (body && visible) body.style.display = '';
    });

    // l0 groups: visible if any child l1 group matches; expand body
    document.querySelectorAll('.tree-l0-group').forEach(function(grp) {
      var visible = Array.from(grp.querySelectorAll('.tree-l1-group')).some(function(el) {
        return el.style.display !== 'none';
      });
      grp.style.display = visible ? '' : 'none';
      var body = grp.querySelector('.tree-l0-body');
      if (body && visible) body.style.display = '';
    });
  });
});
</script>
</body>
</html>
""".trimIndent()
}
