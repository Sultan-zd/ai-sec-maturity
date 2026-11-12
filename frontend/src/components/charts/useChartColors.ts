import { useEffect, useState } from 'react'

/**
 * Lit les couleurs des graphiques depuis les variables CSS (index.css), et se met à jour
 * quand le système bascule entre mode clair et mode sombre. Les graphiques SVG reçoivent
 * ainsi des couleurs concrètes, toujours alignées sur le thème.
 */
export interface ChartColors {
  actual: string
  target: string
  grid: string
  ink: string
  ink2: string
  ink3: string
  surface: string
}

function read(): ChartColors {
  const style = getComputedStyle(document.documentElement)
  const v = (name: string) => style.getPropertyValue(name).trim()
  return {
    actual: v('--series-actual'),
    target: v('--series-target'),
    grid: v('--grid'),
    ink: v('--ink'),
    ink2: v('--ink-2'),
    ink3: v('--ink-3'),
    surface: v('--surface'),
  }
}

export function useChartColors(): ChartColors {
  const [colors, setColors] = useState(read)
  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const update = () => setColors(read())
    media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [])
  return colors
}
