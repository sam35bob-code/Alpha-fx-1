package com.example.ui.components

import com.example.model.WeeklyHeatmapData
import java.util.Locale

object WeeklyHeatmapRechartsHtmlBuilder {

    fun buildHtml(data: WeeklyHeatmapData): String {
        // Build JSON dataset for Day of Week Bar Chart
        val dayDataJson = buildString {
            append("[")
            data.daySummaries.forEachIndexed { index, day ->
                if (index > 0) append(",")
                append("{")
                append("\"day\":\"${day.dayName}\",")
                append("\"winRate\":${String.format(Locale.US, "%.1f", day.winRatePercent)},")
                append("\"total\":${day.totalTrades},")
                append("\"wins\":${day.wonTrades},")
                append("\"losses\":${day.lostTrades},")
                append("\"benchmark\":70.0,")
                append("\"pnl\":${String.format(Locale.US, "%.2f", day.netPnl)}")
                append("}")
            }
            append("]")
        }

        // Build JSON dataset for Hourly Area Chart (0..23)
        val hourlyDataJson = buildString {
            append("[")
            data.hourSummaries.forEachIndexed { index, h ->
                if (index > 0) append(",")
                append("{")
                append("\"hour\":\"${h.hourLabel}\",")
                append("\"h\":${h.hour},")
                append("\"winRate\":${String.format(Locale.US, "%.1f", h.winRatePercent)},")
                append("\"total\":${h.totalTrades},")
                append("\"session\":\"${h.session}\",")
                append("\"benchmark\":70.0")
                append("}")
            }
            append("]")
        }

        // Build JSON dataset for Heatmap Grid Cells
        val heatmapCellsJson = buildString {
            append("[")
            data.cells.forEachIndexed { index, c ->
                if (index > 0) append(",")
                append("{")
                append("\"day\":\"${c.dayName}\",")
                append("\"hour\":${c.hour},")
                append("\"hourLabel\":\"${c.hourLabel}\",")
                append("\"winRate\":${String.format(Locale.US, "%.1f", c.winRatePercent)},")
                append("\"total\":${c.totalTrades},")
                append("\"wins\":${c.wonTrades},")
                append("\"losses\":${c.lostTrades},")
                append("\"session\":\"${c.session}\",")
                append("\"pnl\":${String.format(Locale.US, "%.2f", c.netPnl)}")
                append("}")
            }
            append("]")
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>Recharts Weekly Performance Heatmap</title>
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
      font-size: 12px;
      overflow-x: hidden;
    }
    .header-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;
      padding-bottom: 6px;
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
    .badge {
      background: rgba(0, 243, 255, 0.12);
      border: 1px solid rgba(0, 243, 255, 0.35);
      color: #00f3ff;
      font-size: 9px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;
    }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 6px;
      margin-bottom: 12px;
    }
    .stat-card {
      background: #0f172a;
      border: 1px solid #1e293b;
      border-radius: 8px;
      padding: 8px;
      text-align: center;
    }
    .stat-label {
      font-size: 9px;
      color: #94a3b8;
      text-transform: uppercase;
      font-weight: 600;
      margin-bottom: 2px;
    }
    .stat-val {
      font-size: 13px;
      font-weight: 800;
      color: #f8fafc;
      font-family: monospace;
    }
    .stat-sub {
      font-size: 9px;
      color: #00f3ff;
      font-weight: 600;
    }
    .section-title {
      font-size: 11px;
      font-weight: 700;
      color: #94a3b8;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin: 10px 0 6px 0;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .chart-container {
      background: #0f172a;
      border: 1px solid #1e293b;
      border-radius: 8px;
      padding: 8px;
      margin-bottom: 12px;
    }
    /* Heatmap Grid Table */
    .heatmap-wrapper {
      overflow-x: auto;
      background: #0f172a;
      border: 1px solid #1e293b;
      border-radius: 8px;
      padding: 10px;
      margin-bottom: 12px;
    }
    .heatmap-table {
      width: 100%;
      border-collapse: collapse;
      min-width: 520px;
    }
    .heatmap-th {
      font-size: 9px;
      color: #64748b;
      padding: 4px;
      text-align: center;
      font-weight: 600;
      border-bottom: 1px solid #1e293b;
    }
    .heatmap-td-day {
      font-size: 10px;
      color: #f1f5f9;
      font-weight: 700;
      padding: 4px 6px;
      text-align: left;
      border-right: 1px solid #1e293b;
      white-space: nowrap;
    }
    .heatmap-cell {
      padding: 4px;
      text-align: center;
      border: 1px solid #080c14;
      border-radius: 3px;
      font-family: monospace;
      font-size: 9px;
      cursor: pointer;
      transition: transform 0.1s ease;
    }
    .heatmap-cell:hover {
      transform: scale(1.15);
      z-index: 10;
      box-shadow: 0 0 8px rgba(0, 243, 255, 0.6);
    }
    .legend-bar {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 8px;
      font-size: 9px;
      color: #64748b;
      margin-top: 6px;
    }
    .legend-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .legend-box {
      width: 10px;
      height: 10px;
      border-radius: 2px;
    }
    .golden-banner {
      background: linear-gradient(90deg, rgba(0,243,255,0.12), rgba(0,230,118,0.12));
      border: 1px solid rgba(0,243,255,0.3);
      border-radius: 8px;
      padding: 10px;
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
    }
  </style>
</head>
<body>
  <div id="root"></div>

  <script>
    const { useState } = React;
    const {
      ResponsiveContainer, BarChart, Bar, AreaChart, Area,
      XAxis, YAxis, Tooltip, CartesianGrid, ReferenceLine
    } = Recharts;

    const dayData = $dayDataJson;
    const hourlyData = $hourlyDataJson;
    const heatmapCells = $heatmapCellsJson;

    // Group cells by Day
    const days = ["Mon", "Tue", "Wed", "Thu", "Fri"];
    // 2-hour slots for cleaner compact display: 00-01, 02-03, ... 22-23 (12 cols)
    const hourBlocks = [
      { label: "00h", hours: [0, 1] },
      { label: "02h", hours: [2, 3] },
      { label: "04h", hours: [4, 5] },
      { label: "06h", hours: [6, 7] },
      { label: "08h", hours: [8, 9] },
      { label: "10h", hours: [10, 11] },
      { label: "12h", hours: [12, 13] },
      { label: "14h", hours: [14, 15] },
      { label: "16h", hours: [16, 17] },
      { label: "18h", hours: [18, 19] },
      { label: "20h", hours: [20, 21] },
      { label: "22h", hours: [22, 23] }
    ];

    function getCellColor(winRate, total) {
      if (total === 0) return "#131a29";
      if (winRate >= 80) return "#00f3ff"; // Cyan highlight
      if (winRate >= 70) return "#00e676"; // Emerald green
      if (winRate >= 60) return "#26a69a"; // Teal
      if (winRate >= 50) return "#f59e0b"; // Amber
      return "#ef4444"; // Red
    }

    function getCellTextColor(winRate, total) {
      if (total === 0) return "#475569";
      if (winRate >= 70) return "#000000";
      return "#ffffff";
    }

    function App() {
      const [selectedCell, setSelectedCell] = useState(null);

      return React.createElement("div", null,
        // Header Bar
        React.createElement("div", { className: "header-bar" },
          React.createElement("div", { className: "brand-title" },
            "WEEKLY PERFORMANCE HEATMAP"
          ),
          React.createElement("span", { className: "badge" },
            "${data.totalTradesAnalyzed} ROOM DB TRADES"
          )
        ),

        // Golden Trading Window Banner
        React.createElement("div", { className: "golden-banner" },
          React.createElement("div", { style: { fontSize: "18px" } }, "⚡"),
          React.createElement("div", null,
            React.createElement("div", { style: { fontWeight: "800", color: "#00f3ff", fontSize: "11px" } },
              "GOLDEN TRADING WINDOW: ${data.bestDay.uppercase(Locale.US)} @ ${data.bestHour}"
            ),
            React.createElement("div", { style: { color: "#94a3b8", fontSize: "10px" } },
              "Institutional confluence produces peak ${data.bestDayWinRate}% win rate during the ${data.bestSession}."
            )
          )
        ),

        // Metric Cards Strip
        React.createElement("div", { className: "stats-grid" },
          React.createElement("div", { className: "stat-card" },
            React.createElement("div", { className: "stat-label" }, "Best Day"),
            React.createElement("div", { className: "stat-val", style: { color: "#00e676" } }, "${data.bestDay}"),
            React.createElement("div", { className: "stat-sub" }, "${data.bestDayWinRate}% Win")
          ),
          React.createElement("div", { className: "stat-card" },
            React.createElement("div", { className: "stat-label" }, "Best Hour"),
            React.createElement("div", { className: "stat-val", style: { color: "#00f3ff" } }, "${data.bestHour}"),
            React.createElement("div", { className: "stat-sub" }, "${data.bestHourWinRate}% Win")
          ),
          React.createElement("div", { className: "stat-card" },
            React.createElement("div", { className: "stat-label" }, "Peak Session"),
            React.createElement("div", { className: "stat-val", style: { fontSize: "10px" } }, "${data.bestSession}"),
            React.createElement("div", { className: "stat-sub" }, "${data.bestSessionWinRate}%")
          ),
          React.createElement("div", { className: "stat-card" },
            React.createElement("div", { className: "stat-label" }, "Avoid Times"),
            React.createElement("div", { className: "stat-val", style: { color: "#ef4444" } }, "${data.worstDay}"),
            React.createElement("div", { className: "stat-sub", style: { color: "#ef4444" } }, "${data.worstDayWinRate}% Win")
          )
        ),

        // Heatmap Matrix
        React.createElement("div", { className: "section-title" },
          React.createElement("span", null, "Weekly Trading Hours Matrix (UTC)"),
          React.createElement("span", { style: { fontSize: "9px", color: "#00f3ff" } },
            selectedCell ? selectedCell.day + " " + selectedCell.hourLabel + " (" + selectedCell.winRate + "% Win / " + selectedCell.total + " trades)" : "Tap any cell"
          )
        ),
        React.createElement("div", { className: "heatmap-wrapper" },
          React.createElement("table", { className: "heatmap-table" },
            React.createElement("thead", null,
              React.createElement("tr", null,
                React.createElement("th", { className: "heatmap-th", style: { textAlign: "left", width: "45px" } }, "Day"),
                hourBlocks.map((b, i) =>
                  React.createElement("th", { key: i, className: "heatmap-th" }, b.label)
                )
              )
            ),
            React.createElement("tbody", null,
              days.map((dayName, dIdx) => {
                return React.createElement("tr", { key: dIdx },
                  React.createElement("td", { className: "heatmap-td-day" }, dayName),
                  hourBlocks.map((b, bIdx) => {
                    const matchedCells = heatmapCells.filter(c => c.day === dayName && b.hours.includes(c.hour));
                    const totalT = matchedCells.reduce((acc, c) => acc + c.total, 0);
                    const wonT = matchedCells.reduce((acc, c) => acc + c.wins, 0);
                    const avgRate = totalT > 0 ? Math.round((wonT / totalT) * 1000) / 10 : 0;
                    const bg = getCellColor(avgRate, totalT);
                    const textColor = getCellTextColor(avgRate, totalT);

                    return React.createElement("td", {
                      key: bIdx,
                      className: "heatmap-cell",
                      style: { backgroundColor: bg, color: textColor, fontWeight: totalT > 0 ? "800" : "normal" },
                      onClick: () => setSelectedCell({
                        day: dayName,
                        hourLabel: b.label + " Block",
                        winRate: avgRate,
                        total: totalT
                      })
                    }, totalT > 0 ? avgRate + "%" : "·");
                  })
                );
              })
            )
          ),
          React.createElement("div", { className: "legend-bar" },
            React.createElement("span", null, "Win Rate:"),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-box", style: { background: "#00f3ff" } }),
              React.createElement("span", null, "≥80%")
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-box", style: { background: "#00e676" } }),
              React.createElement("span", null, "70-79%")
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-box", style: { background: "#f59e0b" } }),
              React.createElement("span", null, "50-69%")
            ),
            React.createElement("div", { className: "legend-item" },
              React.createElement("div", { className: "legend-box", style: { background: "#ef4444" } }),
              React.createElement("span", null, "<50%")
            )
          )
        ),

        // Recharts Day of Week Bar Chart
        React.createElement("div", { className: "section-title" },
          "Day of Week Win Rate vs 70% Benchmark"
        ),
        React.createElement("div", { className: "chart-container", style: { height: "180px" } },
          React.createElement(ResponsiveContainer, { width: "100%", height: "100%" },
            React.createElement(BarChart, { data: dayData, margin: { top: 10, right: 10, left: -25, bottom: 0 } },
              React.createElement(CartesianGrid, { strokeDasharray: "3 3", stroke: "#1e293b", vertical: false }),
              React.createElement(XAxis, { dataKey: "day", stroke: "#64748b", fontSize: 10, tickLine: false }),
              React.createElement(YAxis, { domain: [0, 100], stroke: "#64748b", fontSize: 10, tickLine: false, tickFormatter: v => v + "%" }),
              React.createElement(Tooltip, {
                contentStyle: { backgroundColor: "#0f172a", borderColor: "#334155", borderRadius: "6px", fontSize: "11px" },
                formatter: (val, name) => [val + "%", name === "winRate" ? "Win Rate" : "Target"]
              }),
              React.createElement(ReferenceLine, { y: 70, stroke: "#00f3ff", strokeDasharray: "3 3", label: { value: "70% Goal", fill: "#00f3ff", fontSize: 9 } }),
              React.createElement(Bar, { dataKey: "winRate", fill: "#00e676", radius: [4, 4, 0, 0] })
            )
          )
        ),

        // Recharts Hourly Area Chart
        React.createElement("div", { className: "section-title" },
          "Hourly Win Rate Distribution (24-Hour Cycle)"
        ),
        React.createElement("div", { className: "chart-container", style: { height: "180px" } },
          React.createElement(ResponsiveContainer, { width: "100%", height: "100%" },
            React.createElement(AreaChart, { data: hourlyData, margin: { top: 10, right: 10, left: -25, bottom: 0 } },
              React.createElement(CartesianGrid, { strokeDasharray: "3 3", stroke: "#1e293b", vertical: false }),
              React.createElement(XAxis, { dataKey: "hour", stroke: "#64748b", fontSize: 9, tickLine: false, interval: 3 }),
              React.createElement(YAxis, { domain: [0, 100], stroke: "#64748b", fontSize: 10, tickLine: false, tickFormatter: v => v + "%" }),
              React.createElement(Tooltip, {
                contentStyle: { backgroundColor: "#0f172a", borderColor: "#334155", borderRadius: "6px", fontSize: "11px" },
                formatter: (val) => [val + "%", "Win Rate"]
              }),
              React.createElement(ReferenceLine, { y: 70, stroke: "#38bdf8", strokeDasharray: "2 2" }),
              React.createElement(Area, { type: "monotone", dataKey: "winRate", stroke: "#00f3ff", strokeWidth: 2, fill: "rgba(0, 243, 255, 0.15)" })
            )
          )
        )
      );
    }

    ReactDOM.render(React.createElement(App), document.getElementById("root"));
  </script>
</body>
</html>
        """.trimIndent()
    }
}
