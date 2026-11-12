import { Legend, PolarAngleAxis, PolarGrid, PolarRadiusAxis, Radar, RadarChart, ResponsiveContainer, Tooltip } from 'recharts'
import type { DomainResult } from '../../api/types'
import { fmt1, fmt2, levelNames } from '../../lib/format'
import { useChartColors } from './useChartColors'

interface Point {
  code: string
  name: string
  actual: number
  target: number
  evaluated: boolean
}

/**
 * Radar « niveau actuel / niveau cible » par domaine.
 * Deux séries seulement : bleu plein (actuel) et orange en pointillés (cible) — le trait
 * pointillé distingue les séries même sans percevoir les couleurs. Légende + libellés directs.
 */
export function MaturityRadar({ domains }: { domains: DomainResult[] }) {
  const c = useChartColors()
  const data: Point[] = domains.map((d) => ({
    code: d.code,
    name: d.name,
    actual: d.score?.value ?? 0,
    target: d.targetLevel,
    evaluated: d.score != null,
  }))

  return (
    <div className="h-[360px] w-full" role="img"
      aria-label={`Radar de maturité : ${data.map((d) => `${d.code} ${d.evaluated ? fmt1(d.actual) : 'non évalué'} sur cible ${d.target}`).join(', ')}`}>
      <ResponsiveContainer>
        <RadarChart data={data} outerRadius="72%" margin={{ top: 8, right: 24, bottom: 8, left: 24 }}>
          <PolarGrid stroke={c.grid} />
          <PolarAngleAxis
            dataKey="code"
            tick={({ x, y, payload, textAnchor }) => {
              const point = data.find((d) => d.code === payload.value)
              return (
                <text x={x} y={y} textAnchor={textAnchor} dominantBaseline="central" fontSize={12} fill={c.ink2}>
                  <tspan fontWeight={600} fill={c.ink}>{payload.value}</tspan>
                  <tspan dx={4}>{point?.evaluated ? fmt1(point.actual) : 'N/A'}</tspan>
                </text>
              )
            }}
          />
          {/* Graduations placées entre deux axes (67,5°) pour ne pas chevaucher le libellé du premier domaine */}
          <PolarRadiusAxis domain={[0, 5]} tickCount={6} angle={90 - 180 / Math.max(data.length, 1)}
            tick={{ fontSize: 10, fill: c.ink3 }} axisLine={false} />
          <Radar name="Niveau actuel" dataKey="actual" stroke={c.actual} strokeWidth={2}
            fill={c.actual} fillOpacity={0.18} dot={{ r: 3, fill: c.actual, stroke: c.surface, strokeWidth: 2 }}
            isAnimationActive={false} />
          <Radar name="Niveau cible" dataKey="target" stroke={c.target} strokeWidth={2}
            strokeDasharray="6 4" fillOpacity={0} isAnimationActive={false} />
          {/* Le trait porte la couleur de la série ; le texte reste dans la couleur d'encre normale */}
          <Legend wrapperStyle={{ fontSize: 12 }} iconType="plainline"
            formatter={(value: string) => <span style={{ color: c.ink2 }}>{value}</span>} />
          <Tooltip content={<RadarTooltip />} />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  )
}

function RadarTooltip({ active, payload }: { active?: boolean; payload?: { payload: Point }[] }) {
  if (!active || !payload?.length) return null
  const p = payload[0].payload
  return (
    <div className="rounded-md border border-line bg-surface px-3 py-2 text-xs text-ink shadow-sm">
      <div className="font-semibold">{p.code} — {p.name}</div>
      <div>Actuel : {p.evaluated ? `${fmt2(p.actual)} (${levelNames[Math.max(0, Math.floor(p.actual) - 1)]})` : 'non évalué'}</div>
      <div>Cible : niveau {p.target}</div>
    </div>
  )
}
