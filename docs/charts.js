export function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function renderBarChartSvg(metrics, color1 = '#4F46E5', color2 = '#0D9488', targetHours = null) {
  if (!metrics.length || metrics.every((m) => m.studyHours <= 0)) {
    return `<div class="muted" style="text-align:center;padding:34px 8px;">Complete a study session to plot your study hours.</div>`;
  }
  const maxVal = Math.max(...metrics.map((m) => m.studyHours), targetHours || 0, 1) * 1.15;
  const w = 360;
  const h = 150;
  const chartH = 122;
  const slotW = w / metrics.length;
  const barW = Math.min(28, Math.max(12, slotW * 0.5));

  const gridLines = [0.25, 0.5, 0.75, 1]
    .map((f) => {
      const y = (chartH * (1 - f)).toFixed(1);
      return `<line x1="0" y1="${y}" x2="${w}" y2="${y}" stroke="var(--border)" stroke-width="1" stroke-opacity="0.6" />`;
    })
    .join('');

  const targetLine =
    targetHours && targetHours > 0 && targetHours <= maxVal
      ? `<line x1="0" y1="${(chartH * (1 - targetHours / maxVal)).toFixed(1)}" x2="${w}" y2="${(chartH * (1 - targetHours / maxVal)).toFixed(1)}" stroke="#059669" stroke-width="1.5" stroke-dasharray="6 4" />`
      : '';

  const bars = metrics
    .map((m, idx) => {
      const ratio = Math.max(0, Math.min(1, m.studyHours / maxVal));
      const bh = m.studyHours > 0 ? Math.max(4, chartH * ratio) : 0;
      const x = idx * slotW + (slotW - barW) / 2;
      const y = chartH - bh;
      const lblX = idx * slotW + slotW / 2;
      return `
        ${bh > 0 ? `<rect x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${barW.toFixed(1)}" height="${bh.toFixed(1)}" rx="5" fill="url(#barGrad)" />` : ''}
        <text x="${lblX.toFixed(1)}" y="138" text-anchor="middle" fill="var(--text-muted)" font-size="10">${esc(m.label)}</text>
        <text x="${lblX.toFixed(1)}" y="${Math.max(11, y - 4).toFixed(1)}" text-anchor="middle" fill="var(--text)" font-size="9" font-family="var(--font-mono)">${m.studyHours > 0 ? m.studyHours.toFixed(1) + 'h' : ''}</text>
      `;
    })
    .join('');

  return `
    <svg viewBox="0 0 ${w} ${h}" style="width:100%;height:auto;display:block;">
      <defs>
        <linearGradient id="barGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="${color1}" />
          <stop offset="100%" stop-color="${color2}" />
        </linearGradient>
      </defs>
      ${gridLines}
      ${targetLine}
      ${bars}
    </svg>
  `;
}

export function renderDonutChartSvg(completed, inProgress, notStarted) {
  const total = completed + inProgress + notStarted;
  const r = 44;
  const c = 2 * Math.PI * r;
  const pct = total > 0 ? Math.round((completed * 100) / total) : 0;

  let arcs = `<circle cx="58" cy="58" r="${r}" fill="none" stroke="var(--border)" stroke-width="13" />`;
  if (total > 0) {
    const segs = [
      { count: completed, color: '#059669' },
      { count: inProgress, color: '#D97706' },
      { count: notStarted, color: '#64748B' }
    ];
    let offset = 0;
    arcs = segs
      .filter((s) => s.count > 0)
      .map((s) => {
        const len = (s.count / total) * c;
        const el = `<circle cx="58" cy="58" r="${r}" fill="none" stroke="${s.color}" stroke-width="13" stroke-dasharray="${len.toFixed(2)} ${c.toFixed(2)}" stroke-dashoffset="${(-offset).toFixed(2)}" transform="rotate(-90 58 58)" />`;
        offset += len;
        return el;
      })
      .join('');
  }

  return `
    <div style="display:flex;align-items:center;gap:18px;">
      <svg viewBox="0 0 116 116" style="width:112px;height:112px;flex-shrink:0;">
        ${arcs}
        <text x="58" y="55" text-anchor="middle" fill="var(--text)" font-size="18" font-weight="700" font-family="var(--font-mono)">${pct}%</text>
        <text x="58" y="72" text-anchor="middle" fill="var(--text-muted)" font-size="11">${completed}/${total}</text>
      </svg>
      <div style="flex:1;display:flex;flex-direction:column;gap:8px;font-size:13px;">
        <div class="row-between"><span><span class="dot" style="background:#059669;margin-right:6px;"></span>Completed</span><strong class="mono">${completed}</strong></div>
        <div class="row-between"><span><span class="dot" style="background:#D97706;margin-right:6px;"></span>In Progress</span><strong class="mono">${inProgress}</strong></div>
        <div class="row-between"><span><span class="dot" style="background:#64748B;margin-right:6px;"></span>Not Started</span><strong class="mono">${notStarted}</strong></div>
      </div>
    </div>
  `;
}

export function renderTrendLineSvg(points, lineColor = '#4F46E5', emptyMsg = 'No data yet.') {
  if (!points.length) {
    return `<div class="muted" style="text-align:center;padding:34px 8px;">${esc(emptyMsg)}</div>`;
  }
  const w = 360;
  const h = 136;
  const padX = 16;
  const padY = 14;
  const usableW = w - padX * 2;
  const usableH = h - padY * 2;

  const coords = points.map((p, idx) => {
    const x = points.length === 1 ? w / 2 : padX + (idx / (points.length - 1)) * usableW;
    const val = Math.max(0, Math.min(100, p.value));
    const y = padY + usableH * (1 - val / 100);
    return { x, y, label: p.label, value: p.value };
  });

  const grid = [0, 0.5, 1]
    .map((f) => {
      const y = (padY + usableH * (1 - f)).toFixed(1);
      return `<line x1="0" y1="${y}" x2="${w}" y2="${y}" stroke="var(--border)" stroke-width="1" stroke-opacity="0.5" />`;
    })
    .join('');

  let pathHtml = '';
  if (coords.length >= 2) {
    const dLine = coords.map((c, i) => `${i === 0 ? 'M' : 'L'} ${c.x.toFixed(1)} ${c.y.toFixed(1)}`).join(' ');
    const dArea = `${dLine} L ${coords[coords.length - 1].x.toFixed(1)} ${(padY + usableH).toFixed(1)} L ${coords[0].x.toFixed(1)} ${(padY + usableH).toFixed(1)} Z`;
    pathHtml = `
      <path d="${dArea}" fill="${lineColor}" fill-opacity="0.16" />
      <path d="${dLine}" fill="none" stroke="${lineColor}" stroke-width="2.5" stroke-linecap="round" />
    `;
  }

  const dots = coords
    .map((c) => `<circle cx="${c.x.toFixed(1)}" cy="${c.y.toFixed(1)}" r="4.5" fill="${lineColor}" stroke="var(--surface)" stroke-width="2" />`)
    .join('');

  const first = points[0];
  const last = points[points.length - 1];

  return `
    <svg viewBox="0 0 ${w} ${h}" style="width:100%;height:auto;display:block;">
      ${grid}
      ${pathHtml}
      ${dots}
    </svg>
    <div class="row-between muted mono" style="font-size:11px;margin-top:4px;">
      <span>${esc(first.label)} (${first.value.toFixed(0)}%)</span>
      ${points.length > 1 ? `<span style="color:${lineColor};font-weight:700;">Latest: ${esc(last.label)} (${last.value.toFixed(1)}%)</span>` : ''}
    </div>
  `;
}
