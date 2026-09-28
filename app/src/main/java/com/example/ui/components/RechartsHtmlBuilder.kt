package com.example.ui.components

import com.example.model.AccuracyDashboardData
import java.text.SimpleDateFormat
import java.util.*

object RechartsHtmlBuilder {

    fun buildHtml(data: AccuracyDashboardData): String {
        val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
        
        // Build JSON dataset for Recharts
        val chartDataJson = buildString {
            append("[")
            data.accuracyOverTime.forEachIndexed { index, point ->
                if (index > 0) append(",")
                append("{")
                append("\"id\":${point.id},")
                append("\"date\":\"${point.dateLabel}\",")
                append("\"pair\":\"${point.pairSymbol}\",")
                append("\"action\":\"${point.action}\",")
                append("\"status\":\"${point.status}\",")
                append("\"isWin\":${point.isWin},")
                append("\"accuracy\":${String.format(Locale.US, "%.1f", point.cumulativeAccuracyPercent)},")
                append("\"rolling\":${String.format(Locale.US, "%.1f", point.rollingAccuracyPercent)},")
                append("\"target\":${point.cumulativeAccuracyPercent.let { 70.0 }},")
                append("\"pnl\":${String.format(Locale.US, "%.2f", point.pnl)},")
                append("\"cumulativePnl\":${String.format(Locale.US, "%.2f", point.cumulativePnl)}")
                append("}")
            }
            append("]")
        }

        val assetDataJson = buildString {
            append("[")
            data.assetBreakdown.forEachIndexed { index, asset ->
                if (index > 0) append(",")
                append("{")
                append("\"symbol\":\"${asset.symbol}\",")
                append("\"total\":${asset.totalTrades},")
                append("\"won\":${asset.wonTrades},")
                append("\"winRate\":${String.format(Locale.US, "%.1f", asset.winRatePercent)},")
                append("\"pnl\":${String.format(Locale.US, "%.2f", asset.netPnl)}")
                append("}")
            }
            append("]")
        }

        val overallAccuracyStr = String.format(Locale.US, "%.1f%%", data.overallAccuracyPercent)
        val profitFactorStr = String.format(Locale.US, "%.2f", data.profitFactor)
        val netPnlStr = if (data.netPnl >= 0) "+$${String.format(Locale.US, "%,.2f", data.netPnl)}" else "-$${String.format(Locale.US, "%,.2f", Math.abs(data.netPnl))}"

        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>Recharts AI Accuracy Dashboard</title>
  <!-- Load React, ReactDOM and Recharts from CDN -->
  <script crossorigin src="https://unpkg.com/react@18.3.1/umd/react.production.min.js"></script>
  <script crossorigin src="https://unpkg.com/react-dom@18.3.1/umd/react-dom.production.min.js"></script>
  <script crossorigin src="https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js"></script>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
    body {
      background-color: #080c14;
      color: #e2e8f0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      padding: 10px;
      margin: 0;
      font-size: 13px;
      overflow-x: hidden;
    }
    .header-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 1px solid #1a2436;
    }
    .brand-title {
      font-size: 13px;
      font-weight: 800;
      letter-spacing: 0.08em;
      color: #00f3ff;
      text-transform: uppercase;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .engine-tag {
      background: rgba(0, 243, 255, 0.12);
      border: 1px solid rgba(0, 243, 255, 0.35);
      color: #00f3ff;
      font-size: 9px;
      font-weight: 700;
      padding: 3px 8px;
      border-radius: 4px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .kpi-row {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;
      margin-bottom: 12px;
    }
    .kpi-card {
      background: #101726;
      border: 1px solid #1d283c;
      border-radius: 8px;
      padding: 8px 10px;
      text-align: center;
    }
    .kpi-label {
      font-size: 9px;
      font-weight: 600;
      color: #8da2c0;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      margin-bottom: 4px;
    }
    .kpi-value {
      font-size: 16px;
      font-weight: 800;
      font-family: monospace, monospace;
    }
    .val-cyan { color: #00f3ff; text-shadow: 0 0 10px rgba(0, 243, 255, 0.3); }
    .val-green { color: #10b981; }
    .val-amber { color: #f59e0b; }
    
    .chart-container {
      background: #101726;
      border: 1px solid #1d283c;
      border-radius: 10px;
      padding: 12px 6px 6px 0px;
      margin-bottom: 12px;
    }
    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0 10px 8px 10px;
    }
    .chart-title {
      font-size: 11px;
      font-weight: 700;
      color: #cbd5e1;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .legend-indicator {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 10px;
      color: #94a3b8;
    }
    .legend-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      display: inline-block;
    }
    .custom-tooltip {
      background: #0f172a;
      border: 1px solid #00f3ff;
      border-radius: 6px;
      padding: 8px 10px;
      box-shadow: 0 6px 20px rgba(0,0,0,0.6);
      font-size: 11px;
    }
    .tooltip-date { font-weight: 700; color: #f8fafc; margin-bottom: 4px; font-size: 11px; }
    .tooltip-row { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 2px; }
    .tooltip-val { font-weight: 700; font-family: monospace; }
  </style>
</head>
<body>

  <div class="header-bar">
    <div class="brand-title">
      <span>⚡</span> Recharts Visualizer
    </div>
    <div class="engine-tag">Room DB Live Data</div>
  </div>

  <div class="kpi-row">
    <div class="kpi-card">
      <div class="kpi-label">AI Accuracy</div>
      <div class="kpi-value val-cyan">$overallAccuracyStr</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-label">Win / Loss</div>
      <div class="kpi-value val-green">${data.wonCount}W - ${data.lostCount}L</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-label">Net Gain</div>
      <div class="kpi-value val-amber">$netPnlStr</div>
    </div>
  </div>

  <!-- Recharts Root Mount -->
  <div id="recharts-accuracy-root">
    <!-- Fallback Responsive SVG Chart rendered if Recharts CDN is initializing or offline -->
    <div class="chart-container" id="fallback-container">
      <div class="chart-header">
        <div class="chart-title">AI Accuracy Trend Over Time</div>
        <div class="legend-indicator">
          <span><span class="legend-dot" style="background:#00f3ff"></span> Accuracy %</span>
          <span><span class="legend-dot" style="background:#eab308"></span> 70% Target</span>
        </div>
      </div>
      <div id="svg-chart-mount" style="width:100%; height:230px; position:relative;"></div>
    </div>
  </div>

  <div id="recharts-asset-root">
    <div class="chart-container" id="asset-fallback-container">
      <div class="chart-header">
        <div class="chart-title">Accuracy By Currency Pair / Asset</div>
        <div class="legend-indicator">
          <span><span class="legend-dot" style="background:#10b981"></span> Win Rate %</span>
        </div>
      </div>
      <div id="svg-asset-mount" style="width:100%; height:160px; position:relative;"></div>
    </div>
  </div>

  <script>
    const tradeData = $chartDataJson;
    const assetData = $assetDataJson;

    // Render immediate high-fidelity responsive SVG chart so view never lags
    function renderImmediateSvg() {
      const mount = document.getElementById('svg-chart-mount');
      if (!mount || tradeData.length === 0) return;
      
      const width = mount.clientWidth || 340;
      const height = 210;
      const padL = 36;
      const padR = 12;
      const padT = 16;
      const padB = 26;
      const plotW = width - padL - padR;
      const plotH = height - padT - padB;

      // Coordinate mapping (0 to 100% Y)
      const getY = (acc) => padT + (1 - (acc / 100)) * plotH;
      const getX = (idx) => padL + (idx / Math.max(1, tradeData.length - 1)) * plotW;

      // Build Area Path and Line Path
      let pathD = "";
      let areaD = "";
      tradeData.forEach((pt, i) => {
        const x = getX(i);
        const y = getY(pt.accuracy);
        if (i === 0) {
          pathD += `M ${'$'}{x.toFixed(1)} ${'$'}{y.toFixed(1)}`;
          areaD += `M ${'$'}{x.toFixed(1)} ${'$'}{getY(0).toFixed(1)} L ${'$'}{x.toFixed(1)} ${'$'}{y.toFixed(1)}`;
        } else {
          // Smooth curve using cubic bezier control points
          const prevX = getX(i - 1);
          const prevY = getY(tradeData[i - 1].accuracy);
          const cx1 = prevX + (x - prevX) / 2;
          const cx2 = prevX + (x - prevX) / 2;
          pathD += ` C ${'$'}{cx1.toFixed(1)} ${'$'}{prevY.toFixed(1)}, ${'$'}{cx2.toFixed(1)} ${'$'}{y.toFixed(1)}, ${'$'}{x.toFixed(1)} ${'$'}{y.toFixed(1)}`;
          areaD += ` C ${'$'}{cx1.toFixed(1)} ${'$'}{prevY.toFixed(1)}, ${'$'}{cx2.toFixed(1)} ${'$'}{y.toFixed(1)}, ${'$'}{x.toFixed(1)} ${'$'}{y.toFixed(1)}`;
        }
      });
      areaD += ` L ${'$'}{getX(tradeData.length - 1).toFixed(1)} ${'$'}{getY(0).toFixed(1)} Z`;

      const targetY = getY(70);

      let gridLines = '';
      [0, 25, 50, 70, 100].forEach(val => {
        const y = getY(val);
        const isTarget = val === 70;
        gridLines += `<line x1="${'$'}{padL}" y1="${'$'}{y}" x2="${'$'}{width - padR}" y2="${'$'}{y}" stroke="${'$'}{isTarget ? '#eab308' : '#1e293b'}" stroke-dasharray="${'$'}{isTarget ? '4 4' : '2 2'}" stroke-width="${'$'}{isTarget ? '1.5' : '1'}" />`;
        gridLines += `<text x="${'$'}{padL - 6}" y="${'$'}{y + 3}" fill="${'$'}{isTarget ? '#eab308' : '#64748b'}" font-size="9" text-anchor="end" font-family="monospace">${'$'}{val}%</text>`;
      });

      // Dots on data points
      let dots = '';
      tradeData.forEach((pt, i) => {
        const x = getX(i);
        const y = getY(pt.accuracy);
        const dotColor = pt.isWin ? '#10b981' : '#ef4444';
        dots += `<circle cx="${'$'}{x}" cy="${'$'}{y}" r="3.5" fill="${'$'}{dotColor}" stroke="#0b0f19" stroke-width="1.5" />`;
      });

      // X Axis Labels (sample 4-5 labels)
      let xLabels = '';
      const step = Math.max(1, Math.floor(tradeData.length / 4));
      tradeData.forEach((pt, i) => {
        if (i % step === 0 || i === tradeData.length - 1) {
          const x = getX(i);
          xLabels += `<text x="${'$'}{x}" y="${'$'}{height - 6}" fill="#64748b" font-size="9" text-anchor="middle" font-family="monospace">${'$'}{pt.date}</text>`;
        }
      });

      mount.innerHTML = `
        <svg width="100%" height="100%" viewBox="0 0 ${'$'}{width} ${'$'}{height}" style="overflow:visible;">
          <defs>
            <linearGradient id="rechartsCyanGlow" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#00f3ff" stop-opacity="0.38"/>
              <stop offset="60%" stop-color="#00f3ff" stop-opacity="0.10"/>
              <stop offset="100%" stop-color="#00f3ff" stop-opacity="0.0"/>
            </linearGradient>
            <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="2" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>
          ${'$'}{gridLines}
          <path d="${'$'}{areaD}" fill="url(#rechartsCyanGlow)" />
          <path d="${'$'}{pathD}" fill="none" stroke="#00f3ff" stroke-width="2.5" filter="url(#glow)" stroke-linecap="round" />
          ${'$'}{dots}
          ${'$'}{xLabels}
        </svg>
      `;
    }

    function renderImmediateAssetSvg() {
      const mount = document.getElementById('svg-asset-mount');
      if (!mount || assetData.length === 0) return;
      const width = mount.clientWidth || 340;
      const height = 150;
      const barH = 18;
      const gap = 8;
      const startY = 10;
      const labelW = 60;
      const barMaxW = width - labelW - 55;

      let html = `<div style="display:flex; flex-direction:column; gap:8px; padding:6px 12px;">`;
      assetData.forEach(asset => {
        const fillW = Math.max(8, (asset.winRate / 100) * barMaxW);
        const barColor = asset.winRate >= 70 ? '#10b981' : (asset.winRate >= 50 ? '#00f3ff' : '#f59e0b');
        html += `
          <div style="display:flex; align-items:center; font-size:11px;">
            <div style="width:${'$'}{labelW}px; font-weight:700; color:#cbd5e1; font-family:monospace;">${'$'}{asset.symbol}</div>
            <div style="flex:1; background:#1b2434; border-radius:4px; height:14px; overflow:hidden; position:relative;">
              <div style="width:${'$'}{fillW}px; height:100%; background:${'$'}{barColor}; border-radius:4px; transition:width 0.4s ease;"></div>
            </div>
            <div style="width:48px; text-align:right; font-weight:800; color:${'$'}{barColor}; font-family:monospace;">${'$'}{asset.winRate.toFixed(0)}%</div>
          </div>
        `;
      });
      html += `</div>`;
      mount.innerHTML = html;
    }

    renderImmediateSvg();
    renderImmediateAssetSvg();

    // Check if Recharts is loaded from CDN and mount React Recharts components
    function tryMountRecharts() {
      if (window.React && window.ReactDOM && window.Recharts) {
        try {
          const { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine, BarChart, Bar, Cell } = window.Recharts;
          
          // Custom Tooltip component
          const CustomTooltip = ({ active, payload, label }) => {
            if (active && payload && payload.length) {
              const d = payload[0].payload;
              return React.createElement('div', { className: 'custom-tooltip' },
                React.createElement('div', { className: 'tooltip-date' }, `${'$'}{d.date} • ${'$'}{d.pair}`),
                React.createElement('div', { className: 'tooltip-row' },
                  React.createElement('span', { style: { color: '#94a3b8' } }, 'Action:'),
                  React.createElement('span', { className: 'tooltip-val', style: { color: d.action === 'BUY' ? '#10b981' : '#f43f5e' } }, d.action)
                ),
                React.createElement('div', { className: 'tooltip-row' },
                  React.createElement('span', { style: { color: '#94a3b8' } }, 'Result:'),
                  React.createElement('span', { className: 'tooltip-val', style: { color: d.isWin ? '#10b981' : '#ef4444' } }, d.status)
                ),
                React.createElement('div', { className: 'tooltip-row' },
                  React.createElement('span', { style: { color: '#94a3b8' } }, 'Cumulative Acc:'),
                  React.createElement('span', { className: 'tooltip-val val-cyan' }, `${'$'}{d.accuracy}%`)
                ),
                React.createElement('div', { className: 'tooltip-row' },
                  React.createElement('span', { style: { color: '#94a3b8' } }, 'P&L:'),
                  React.createElement('span', { className: 'tooltip-val', style: { color: d.pnl >= 0 ? '#10b981' : '#ef4444' } }, `${'$'}{d.pnl >= 0 ? '+' : ''}$${'$'}{d.pnl}`)
                )
              );
            }
            return null;
          };

          // Main Accuracy Area Chart
          const AccuracyChartApp = () => {
            return React.createElement('div', { className: 'chart-container' },
              React.createElement('div', { className: 'chart-header' },
                React.createElement('div', { className: 'chart-title' }, 'AI Accuracy Trend Over Time (Recharts)'),
                React.createElement('div', { className: 'legend-indicator' },
                  React.createElement('span', null,
                    React.createElement('span', { className: 'legend-dot', style: { background: '#00f3ff' } }),
                    ' Accuracy %'
                  ),
                  React.createElement('span', null,
                    React.createElement('span', { className: 'legend-dot', style: { background: '#eab308' } }),
                    ' 70% Target'
                  )
                )
              ),
              React.createElement(ResponsiveContainer, { width: '100%', height: 230 },
                React.createElement(AreaChart, { data: tradeData, margin: { top: 10, right: 12, left: -16, bottom: 0 } },
                  React.createElement('defs', null,
                    React.createElement('linearGradient', { id: 'cyanRechartsGlow', x1: '0', y1: '0', x2: '0', y2: '1' },
                      React.createElement('stop', { offset: '5%', stopColor: '#00f3ff', stopOpacity: 0.45 }),
                      React.createElement('stop', { offset: '95%', stopColor: '#00f3ff', stopOpacity: 0.0 })
                    )
                  ),
                  React.createElement(CartesianGrid, { strokeDasharray: '3 3', stroke: '#1e293b' }),
                  React.createElement(XAxis, { dataKey: 'date', stroke: '#64748b', tick: { fill: '#8da2c0', fontSize: 10 }, interval: 'preserveStartEnd' }),
                  React.createElement(YAxis, { domain: [0, 100], stroke: '#64748b', tick: { fill: '#8da2c0', fontSize: 10 }, unit: '%' }),
                  React.createElement(Tooltip, { content: React.createElement(CustomTooltip) }),
                  React.createElement(ReferenceLine, { y: 70, stroke: '#eab308', strokeDasharray: '4 4', strokeWidth: 1.5 }),
                  React.createElement(Area, {
                    type: 'monotone',
                    dataKey: 'accuracy',
                    stroke: '#00f3ff',
                    strokeWidth: 2.5,
                    fillOpacity: 1,
                    fill: 'url(#cyanRechartsGlow)'
                  })
                )
              )
            );
          };

          // Secondary Bar Chart per Asset
          const AssetChartApp = () => {
            return React.createElement('div', { className: 'chart-container' },
              React.createElement('div', { className: 'chart-header' },
                React.createElement('div', { className: 'chart-title' }, 'Win Rate By Asset (Recharts)'),
                React.createElement('div', { className: 'legend-indicator' },
                  React.createElement('span', null,
                    React.createElement('span', { className: 'legend-dot', style: { background: '#10b981' } }),
                    ' Win Rate %'
                  )
                )
              ),
              React.createElement(ResponsiveContainer, { width: '100%', height: 160 },
                React.createElement(BarChart, { data: assetData, margin: { top: 10, right: 12, left: -16, bottom: 0 } },
                  React.createElement(CartesianGrid, { strokeDasharray: '3 3', stroke: '#1e293b' }),
                  React.createElement(XAxis, { dataKey: 'symbol', stroke: '#64748b', tick: { fill: '#8da2c0', fontSize: 10 } }),
                  React.createElement(YAxis, { domain: [0, 100], stroke: '#64748b', tick: { fill: '#8da2c0', fontSize: 10 }, unit: '%' }),
                  React.createElement(Tooltip, {
                    formatter: (value) => [`${'$'}{value}%`, 'Win Rate'],
                    contentStyle: { background: '#0f172a', borderColor: '#10b981', borderRadius: '6px', fontSize: '11px' }
                  }),
                  React.createElement(Bar, { dataKey: 'winRate', fill: '#10b981', radius: [4, 4, 0, 0] },
                    assetData.map((entry, index) => 
                      React.createElement(Cell, {
                        key: `cell-${'$'}{index}`,
                        fill: entry.winRate >= 70 ? '#10b981' : (entry.winRate >= 50 ? '#00f3ff' : '#f59e0b')
                      })
                    )
                  )
                )
              )
            );
          };

          const root1 = ReactDOM.createRoot(document.getElementById('recharts-accuracy-root'));
          root1.render(React.createElement(AccuracyChartApp));

          const root2 = ReactDOM.createRoot(document.getElementById('recharts-asset-root'));
          root2.render(React.createElement(AssetChartApp));

        } catch (err) {
          console.warn("Recharts mount error, using standalone high-fidelity SVG fallback:", err);
        }
      }
    }

    // Try mounting Recharts immediately and poll until CDN bundle loads
    tryMountRecharts();
    let attempts = 0;
    const interval = setInterval(() => {
      attempts++;
      if (window.Recharts || attempts > 20) {
        clearInterval(interval);
        tryMountRecharts();
      }
    }, 250);

    window.addEventListener('resize', () => {
      renderImmediateSvg();
      renderImmediateAssetSvg();
    });
  </script>
</body>
</html>
        """.trimIndent()
    }
}
