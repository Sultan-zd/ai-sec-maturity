import { Bar, BarChart, CartesianGrid, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { DomainResult } from '../../api/types'
import { fmt2 } from '../../lib/format'
import { useChartColors } from './useChartColors'

/**
 * Écart à la cible par domaine, du plus grand au plus petit.
 * Une seule série (magnitude) : une couleur, pas de légende — le titre de la carte la nomme.
 */
export function GapBars({ domains }: { domains: DomainResult[] }) {
  const c = useChartColors()
  const data = domains
    .filter((d) => d.gap != null)
    .map((d) => ({ code: d.code, name: d.name, gap: d.gap ?? 0, target: d.targetLevel, score: d.score?.value }))
    .sort((a, b) => b.gap - a.gap)

  if (data.length === 0) {
    return <p className="text-sm text-ink-2">Aucun domaine évalué pour le moment.</p>
  }
  if (data.every((d) => d.gap === 0)) {
    return <p className="text-sm text-ink-2">Tous les domaines évalués atteignent leur niveau cible.</p>
  }

  return (
    <div className="w-full" style={{ height: 40 + data.length * 34 }}>
      <ResponsiveContainer>
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 48, bottom: 4, left: 8 }} barCategoryGap={6}>
          <CartesianGrid horizontal={false} stroke={c.grid} />
          <XAxis type="number" domain={[0, 4]} ticks={[0, 1, 2, 3, 4]} tick={{ fontSize: 11, fill: c.ink3 }}
            axisLine={false} tickLine={false} />
          <YAxis type="category" dataKey="code" width={64} tick={{ fontSize: 12, fill: c.ink }} axisLine={false} tickLine={false} />
          <Tooltip
            cursor={{ fill: c.grid, opacity: 0.4 }}
            content={({ active, payload }) => {
              if (!active || !payload?.length) return null
              const d = payload[0].payload as (typeof data)[number]
              return (
                <div className="rounded-md border border-line bg-surface px-3 py-2 text-xs text-ink shadow-sm">
                  <div className="font-semibold">{d.code} — {d.name}</div>
                  <div>Score {fmt2(d.score)} · cible {d.target}</div>
                  <div>Écart : {fmt2(d.gap)} niveau(x)</div>
                </div>
              )
            }}
          />
          <Bar dataKey="gap" fill={c.actual} radius={[0, 4, 4, 0]} maxBarSize={18} isAnimationActive={false}>
            <LabelList dataKey="gap" position="right" formatter={(v: unknown) => fmt2(Number(v))}
              style={{ fontSize: 11, fill: c.ink2 }} />
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
