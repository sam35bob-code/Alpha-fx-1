package com.example.ui.components

import com.example.model.MarketCorrelationData
import java.util.Locale

object MarketCorrelationRechartsHtmlBuilder {

    fun buildHtml(data: MarketCorrelationData): String {
        // Build JSON dataset for Recharts
        val pointsJson = buildString {
            append("[")
            data.points.forEachIndexed { index, pt ->
                if (index > 0) append(",")
                append("{")
                append("\"id\":${pt.id},")
                append("\"time\":\"${pt.timeLabel}\",")
                append("\"us30\":${String.format(Locale.US, "%.2f", pt.us30Normalized)},")
                append("\"eurusd\":${String.format(Locale.US, "%.2f", pt.eurusdNormalized)},")
                append("\"gbpusd\":${String.format(Locale.US, "%.2f", pt.gbpusdNormalized)},")
                append("\"us30Price\":${String.format(Locale.US, "%.1f", pt.us30RawPrice)},")
                append("\"eurPrice\":${String.format(Locale.US, "%.5f", pt.eurusdRawPrice)},")
                append("\"gbpPrice\":${String.format(Locale.US, "%.5f", pt.gbpusdRawPrice)},")
                append("\"corrEurGbp\":${String.format(Locale.US, "%.2f", pt.rollingCorrEurGbp)},")
                append("\"corrUs30Eur\":${String.format(Locale.US, "%.2f", pt.rollingCorrUs30Eur)},")
                append("\"corrUs30Gbp\":${String.format(Locale.US, "%.2f", pt.rollingCorrUs30Gbp)}")
                append("}")
            }
            append("]")
        }

        val eurGbpMetric = data.metrics.find { it.pairA == "EUR/USD" && it.pairB == "GBP/USD" }?.coefficient ?: 0.86
        val us30EurMetric = data.metrics.find { it.pairA == "US30" && it.pairB == "EUR/USD" }?.coefficient ?: 0.54
        val us30GbpMetric = data.metrics.find { it.pairA == "US30" && it.pairB == "GBP/USD" }?.coefficient ?: 0.62

        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>Market Correlation Visualizer</title>
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
      margin-bottom: 10px;
      padding-bottom: 8px;
      border-bottom: 1px solid #1a2436;
    }
    .brand-title {
      font-size: 12px;
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
      padding: 2px 7px;
      border-radius: 4px;
    }
    .view-mode-tabs {
      display: flex;
      gap: 6px;
      margin-bottom: 10px;
    }
    .mode-btn {
      flex: 1;
      padding: 6px 4px;
      font-size: 10px;
      font-weight: 700;
      text-align: center;
      background: #0f172a;
      border: 1px solid #1e293b;
      color: #94a3b8;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .mode-btn.active {
      background: rgba(0, 243, 255, 0.15);
      border-color: #00f3ff;
      color: #00f3ff;
    }
    .chart-card {
      background: #0d131f;
      border: 1px solid #1a2436;
      border-radius: 8px;
      padding: 12px 6px 6px 2px;
      margin-bottom: 10px;
    }
    .matrix-container {
      display: grid;
      grid-template-columns: 80px repeat(3, 1fr);
      gap: 6px;
      padding: 10px;
      background: #0d131f;
      border: 1px solid #1a2436;
      border-radius: 8px;
      margin-bottom: 10px;
    }
    .matrix-header {
      font-size: 10px;
      font-weight: 700;
      color: #94a3b8;
      display: flex;
      align-items: center;
      justify-content: center;
      text-align: center;
    }
    .matrix-cell {
      padding: 10px 4px;
      border-radius: 6px;
      text-align: center;
      font-family: monospace;
      font-weight: 800;
      font-size: 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
    }
    .cell-self { background: rgba(148, 163, 184, 0.12); color: #94a3b8; border: 1px solid #334155; }
    .cell-high-pos { background: rgba(0, 230, 118, 0.18); color: #00e676; border: 1px solid rgba(0, 230, 118, 0.4); }
    .cell-mod-pos { background: rgba(0, 243, 255, 0.15); color: #00f3ff; border: 1px solid rgba(0, 243, 255, 0.4); }
    .cell-inv { background: rgba(255, 82, 82, 0.18); color: #ff5252; border: 1px solid rgba(255, 82, 82, 0.4); }
    .quick-stat-bar {
      display: flex;
      gap: 8px;
      margin-bottom: 10px;
    }
    .stat-chip {
      flex: 1;
      background: #0f172a;
      border: 1px solid #1e293b;
      border-radius: 6px;
      padding: 6px 8px;
      display: flex;
      flex-direction: column;
    }
    .stat-chip-label { font-size: 9px; color: #64748b; font-weight: 600; text-transform: uppercase; }
    .stat-chip-val { font-size: 13px; font-weight: 800; font-family: monospace; }
  </style>
</head>
<body>
  <div id="root"></div>

  <script>
    const { useState } = React;
    const {
      ResponsiveContainer,
      LineChart,
      Line,
      AreaChart,
      Area,
      XAxis,
      YAxis,
      CartesianGrid,
      Tooltip,
      Legend,
      ReferenceLine
    } = Recharts;

    const dataPoints = $pointsJson;

    function CustomTooltip({ active, payload, label, mode }) {
      if (active && payload && payload.length) {
        return React.createElement(
          'div',
          {
            style: {
              background: 'rgba(8, 12, 20, 0.95)',
              border: '1px solid #00f3ff',
              borderRadius: '6px',
              padding: '8px 10px',
              fontSize: '11px',
              boxShadow: '0 4px 14px rgba(0, 243, 255, 0.25)'
            }
          },
          React.createElement('div', { style: { fontWeight: 800, color: '#94a3b8', marginBottom: '4px' } }, 'TIME: ' + label),
          payload.map((entry, idx) => {
            let unit = mode === 'perf' ? '%' : ' r';
            let val = entry.value;
            let valFormatted = (val >= 0 ? '+' : '') + val + unit;
            return React.createElement(
              'div',
              { key: idx, style: { display: 'flex', justifyContent: 'space-between', gap: '12px', margin: '2px 0' } },
              React.createElement('span', { style: { color: entry.color, fontWeight: 700 } }, entry.name + ':'),
              React.createElement('span', { style: { fontFamily: 'monospace', fontWeight: 800, color: '#fff' } }, valFormatted)
            );
          })
        );
      }
      return null;
    }

    function CorrelationApp() {
      const [viewMode, setViewMode] = useState('perf'); // 'perf', 'rolling', 'matrix'

      return React.createElement(
        'div',
        null,
        // Header
        React.createElement(
          'div',
          { className: 'header-bar' },
          React.createElement(
            'div',
            { className: 'brand-title' },
            React.createElement('span', { style: { color: '#ffd700' } }, '●'),
            'Recharts Market Correlation Engine'
          ),
          React.createElement('span', { className: 'engine-tag' }, 'v2.12.7 CDN')
        ),

        // Quick Stats Row
        React.createElement(
          'div',
          { className: 'quick-stat-bar' },
          React.createElement(
            'div',
            { className: 'stat-chip' },
            React.createElement('span', { className: 'stat-chip-label' }, 'EUR/GBP Coeff'),
            React.createElement('span', { className: 'stat-chip-val', style: { color: '#00e676' } }, '+${String.format(Locale.US, "%.2f", eurGbpMetric)}')
          ),
          React.createElement(
            'div',
            { className: 'stat-chip' },
            React.createElement('span', { className: 'stat-chip-label' }, 'US30/EUR Coeff'),
            React.createElement('span', { className: 'stat-chip-val', style: { color: '#00f3ff' } }, '+${String.format(Locale.US, "%.2f", us30EurMetric)}')
          ),
          React.createElement(
            'div',
            { className: 'stat-chip' },
            React.createElement('span', { className: 'stat-chip-label' }, 'US30/GBP Coeff'),
            React.createElement('span', { className: 'stat-chip-val', style: { color: '#ffd700' } }, '+${String.format(Locale.US, "%.2f", us30GbpMetric)}')
          )
        ),

        // View Mode Tabs
        React.createElement(
          'div',
          { className: 'view-mode-tabs' },
          React.createElement(
            'button',
            {
              className: 'mode-btn ' + (viewMode === 'perf' ? 'active' : ''),
              onClick: () => setViewMode('perf')
            },
            'Multi-Asset Return (%)'
          ),
          React.createElement(
            'button',
            {
              className: 'mode-btn ' + (viewMode === 'rolling' ? 'active' : ''),
              onClick: () => setViewMode('rolling')
            },
            'Rolling Correlation (r)'
          ),
          React.createElement(
            'button',
            {
              className: 'mode-btn ' + (viewMode === 'matrix' ? 'active' : ''),
              onClick: () => setViewMode('matrix')
            },
            '3x3 Heatmap Matrix'
          )
        ),

        // Main Visualizer Display
        viewMode === 'perf' &&
          React.createElement(
            'div',
            { className: 'chart-card' },
            React.createElement(
              'div',
              { style: { fontSize: '11px', fontWeight: 700, color: '#94a3b8', paddingLeft: '8px', marginBottom: '8px' } },
              'NORMALIZED PRICE ACTION (% RETURN)'
            ),
            React.createElement(
              ResponsiveContainer,
              { width: '100%', height: 260 },
              React.createElement(
                LineChart,
                { data: dataPoints, margin: { top: 5, right: 10, left: -20, bottom: 5 } },
                React.createElement(CartesianGrid, { strokeDasharray: '3 3', stroke: '#1a2436' }),
                React.createElement(XAxis, { dataKey: 'time', stroke: '#64748b', fontSize: 10, tickLine: false }),
                React.createElement(YAxis, { stroke: '#64748b', fontSize: 10, tickLine: false, unit: '%' }),
                React.createElement(ReferenceLine, { y: 0, stroke: '#334155', strokeWidth: 1 }),
                React.createElement(Tooltip, { content: (props) => React.createElement(CustomTooltip, { ...props, mode: 'perf' }) }),
                React.createElement(Legend, { wrapperStyle: { fontSize: '11px', paddingTop: '6px' } }),
                React.createElement(Line, { type: 'monotone', dataKey: 'us30', name: 'US30', stroke: '#ffd700', strokeWidth: 2.2, dot: false }),
                React.createElement(Line, { type: 'monotone', dataKey: 'eurusd', name: 'EUR/USD', stroke: '#00f3ff', strokeWidth: 2.2, dot: false }),
                React.createElement(Line, { type: 'monotone', dataKey: 'gbpusd', name: 'GBP/USD', stroke: '#00e676', strokeWidth: 2.2, dot: false })
              )
            )
          ),

        viewMode === 'rolling' &&
          React.createElement(
            'div',
            { className: 'chart-card' },
            React.createElement(
              'div',
              { style: { fontSize: '11px', fontWeight: 700, color: '#94a3b8', paddingLeft: '8px', marginBottom: '8px' } },
              'ROLLING 20-PERIOD PEARSON CORRELATION (-1.0 to +1.0)'
            ),
            React.createElement(
              ResponsiveContainer,
              { width: '100%', height: 260 },
              React.createElement(
                AreaChart,
                { data: dataPoints, margin: { top: 5, right: 10, left: -20, bottom: 5 } },
                React.createElement(CartesianGrid, { strokeDasharray: '3 3', stroke: '#1a2436' }),
                React.createElement(XAxis, { dataKey: 'time', stroke: '#64748b', fontSize: 10, tickLine: false }),
                React.createElement(YAxis, { domain: [-1.0, 1.0], stroke: '#64748b', fontSize: 10, tickLine: false }),
                React.createElement(ReferenceLine, { y: 0, stroke: '#475569', strokeWidth: 1.5 }),
                React.createElement(ReferenceLine, { y: 0.8, stroke: '#00e676', strokeDasharray: '2 2' }),
                React.createElement(ReferenceLine, { y: -0.4, stroke: '#ff5252', strokeDasharray: '2 2' }),
                React.createElement(Tooltip, { content: (props) => React.createElement(CustomTooltip, { ...props, mode: 'rolling' }) }),
                React.createElement(Legend, { wrapperStyle: { fontSize: '11px', paddingTop: '6px' } }),
                React.createElement(Area, { type: 'monotone', dataKey: 'corrEurGbp', name: 'EUR/USD vs GBP/USD', stroke: '#00e676', fill: 'rgba(0, 230, 118, 0.1)', strokeWidth: 2 }),
                React.createElement(Area, { type: 'monotone', dataKey: 'corrUs30Eur', name: 'US30 vs EUR/USD', stroke: '#00f3ff', fill: 'rgba(0, 243, 255, 0.1)', strokeWidth: 2 }),
                React.createElement(Area, { type: 'monotone', dataKey: 'corrUs30Gbp', name: 'US30 vs GBP/USD', stroke: '#ffd700', fill: 'rgba(255, 215, 0, 0.08)', strokeWidth: 2 })
              )
            )
          ),

        viewMode === 'matrix' &&
          React.createElement(
            'div',
            null,
            React.createElement(
              'div',
              { className: 'matrix-container' },
              // Row 0
              React.createElement('div', { className: 'matrix-header' }, 'ASSET'),
              React.createElement('div', { className: 'matrix-header' }, 'US30'),
              React.createElement('div', { className: 'matrix-header' }, 'EUR/USD'),
              React.createElement('div', { className: 'matrix-header' }, 'GBP/USD'),

              // Row 1: US30
              React.createElement('div', { className: 'matrix-header', style: { color: '#ffd700', fontWeight: 800 } }, 'US30'),
              React.createElement('div', { className: 'matrix-cell cell-self' }, '1.00'),
              React.createElement('div', { className: 'matrix-cell cell-mod-pos' }, '+${String.format(Locale.US, "%.2f", us30EurMetric)}'),
              React.createElement('div', { className: 'matrix-cell cell-mod-pos' }, '+${String.format(Locale.US, "%.2f", us30GbpMetric)}'),

              // Row 2: EUR/USD
              React.createElement('div', { className: 'matrix-header', style: { color: '#00f3ff', fontWeight: 800 } }, 'EUR/USD'),
              React.createElement('div', { className: 'matrix-cell cell-mod-pos' }, '+${String.format(Locale.US, "%.2f", us30EurMetric)}'),
              React.createElement('div', { className: 'matrix-cell cell-self' }, '1.00'),
              React.createElement('div', { className: 'matrix-cell cell-high-pos' }, '+${String.format(Locale.US, "%.2f", eurGbpMetric)}'),

              // Row 3: GBP/USD
              React.createElement('div', { className: 'matrix-header', style: { color: '#00e676', fontWeight: 800 } }, 'GBP/USD'),
              React.createElement('div', { className: 'matrix-cell cell-mod-pos' }, '+${String.format(Locale.US, "%.2f", us30GbpMetric)}'),
              React.createElement('div', { className: 'matrix-cell cell-high-pos' }, '+${String.format(Locale.US, "%.2f", eurGbpMetric)}'),
              React.createElement('div', { className: 'matrix-cell cell-self' }, '1.00')
            ),
            React.createElement(
              'div',
              { style: { fontSize: '11px', color: '#64748b', padding: '0 4px', lineHeight: 1.4 } },
              '• Green (+0.80+): High co-movement redundancy. Halve position sizes.\\n• Cyan (+0.40 to +0.79): Moderate risk-on sympathy co-drift.'
            )
          )
      );
    }

    ReactDOM.render(React.createElement(CorrelationApp), document.getElementById('root'));
  </script>
</body>
</html>
        """.trimIndent()
    }
}
