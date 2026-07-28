import { motion } from "framer-motion";

/**
 * A minimal route map: the two airports projected onto a local equirectangular
 * plane, joined by a curved arc with the aircraft placed along it by progress.
 *
 * Deliberately not a tile map — a raster basemap would be light-only and clash
 * with the dark theme, and would need a key/attribution. This renders from the
 * airport coordinates alone, so it themes cleanly and ships no dependencies.
 */

const VIEW_WIDTH = 600;
const VIEW_HEIGHT = 260;
const PADDING_X = 70;
const PADDING_Y = 70;

interface RouteMapProps {
  departureCode: string;
  arrivalCode: string;
  departureLatitude: number | null;
  departureLongitude: number | null;
  arrivalLatitude: number | null;
  arrivalLongitude: number | null;
  progressPercentage: number;
}

export function RouteMap({
  departureCode,
  arrivalCode,
  departureLatitude,
  departureLongitude,
  arrivalLatitude,
  arrivalLongitude,
  progressPercentage,
}: RouteMapProps) {
  const hasCoordinates =
    departureLatitude !== null &&
    departureLongitude !== null &&
    arrivalLatitude !== null &&
    arrivalLongitude !== null;

  if (!hasCoordinates) return null;

  // Equirectangular projection scaled to the pair of points. Longitude is
  // compressed by cos(mean latitude) so the route keeps a realistic shape at
  // European latitudes rather than being stretched horizontally.
  const meanLatRad = (((departureLatitude + arrivalLatitude) / 2) * Math.PI) / 180;
  const lonScale = Math.cos(meanLatRad);

  const xs = [departureLongitude * lonScale, arrivalLongitude * lonScale];
  const ys = [-departureLatitude, -arrivalLatitude];

  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const minY = Math.min(...ys);
  const maxY = Math.max(...ys);

  // Guard against a zero-size span (same airport, or a due N/S route).
  const spanX = Math.max(maxX - minX, 0.0001);
  const spanY = Math.max(maxY - minY, 0.0001);

  // Preserve aspect ratio so short hops don't get blown up to fill the frame.
  const usableWidth = VIEW_WIDTH - PADDING_X * 2;
  const usableHeight = VIEW_HEIGHT - PADDING_Y * 2;
  const scale = Math.min(usableWidth / spanX, usableHeight / spanY);

  const offsetX = (VIEW_WIDTH - spanX * scale) / 2;
  const offsetY = (VIEW_HEIGHT - spanY * scale) / 2;

  const project = (lon: number, lat: number) => ({
    x: (lon * lonScale - minX) * scale + offsetX,
    y: (-lat - minY) * scale + offsetY,
  });

  const from = project(departureLongitude, departureLatitude);
  const to = project(arrivalLongitude, arrivalLatitude);

  // Quadratic arc bowing "north" of the direct line, mimicking a great circle.
  const midX = (from.x + to.x) / 2;
  const midY = (from.y + to.y) / 2;
  const dx = to.x - from.x;
  const dy = to.y - from.y;
  const distance = Math.hypot(dx, dy) || 1;
  const bow = Math.min(distance * 0.22, 70);
  // Perpendicular offset, flipped so the arc always bows upward on screen.
  const normalX = -dy / distance;
  const normalY = dx / distance;
  const direction = normalY > 0 ? -1 : 1;
  const controlX = midX + normalX * bow * direction;
  const controlY = midY + normalY * bow * direction;

  const path = `M ${from.x} ${from.y} Q ${controlX} ${controlY} ${to.x} ${to.y}`;

  // Point on the quadratic Bézier at t, for placing the aircraft.
  const t = Math.min(1, Math.max(0, progressPercentage / 100));
  const oneMinusT = 1 - t;
  const planeX =
    oneMinusT * oneMinusT * from.x + 2 * oneMinusT * t * controlX + t * t * to.x;
  const planeY =
    oneMinusT * oneMinusT * from.y + 2 * oneMinusT * t * controlY + t * t * to.y;
  // Derivative gives the tangent, so the aircraft points along the path.
  const tangentX = 2 * oneMinusT * (controlX - from.x) + 2 * t * (to.x - controlX);
  const tangentY = 2 * oneMinusT * (controlY - from.y) + 2 * t * (to.y - controlY);
  const headingDeg = (Math.atan2(tangentY, tangentX) * 180) / Math.PI;

  return (
    <div className="relative w-full overflow-hidden rounded-xl border border-zinc-200 bg-gradient-to-b from-zinc-50 to-white dark:border-white/10 dark:from-white/[0.04] dark:to-transparent">
      <svg
        viewBox={`0 0 ${VIEW_WIDTH} ${VIEW_HEIGHT}`}
        className="h-auto w-full"
        role="img"
        aria-label={`Route from ${departureCode} to ${arrivalCode}`}
      >
        <defs>
          <pattern
            id="route-grid"
            width="30"
            height="30"
            patternUnits="userSpaceOnUse"
          >
            <path
              d="M 30 0 L 0 0 0 30"
              fill="none"
              stroke="currentColor"
              strokeWidth="0.5"
              className="text-zinc-200 dark:text-white/[0.06]"
            />
          </pattern>
          <linearGradient id="route-arc" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0%" stopColor="currentColor" stopOpacity="0.35" />
            <stop offset="100%" stopColor="currentColor" stopOpacity="1" />
          </linearGradient>
        </defs>

        <rect width={VIEW_WIDTH} height={VIEW_HEIGHT} fill="url(#route-grid)" />

        {/* Full route, faint */}
        <path
          d={path}
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeDasharray="4 5"
          className="text-zinc-300 dark:text-white/20"
        />

        {/* Flown portion, drawn proportionally to progress */}
        <motion.path
          d={path}
          fill="none"
          stroke="url(#route-arc)"
          strokeWidth="2.5"
          strokeLinecap="round"
          className="text-accent"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: t }}
          transition={{ duration: 1.4, ease: "easeOut" }}
        />

        {/* Endpoints */}
        {[
          { point: from, code: departureCode, anchor: "start" as const },
          { point: to, code: arrivalCode, anchor: "end" as const },
        ].map(({ point, code, anchor }) => (
          <g key={code}>
            <circle
              cx={point.x}
              cy={point.y}
              r="9"
              className="fill-accent/15"
            />
            <circle
              cx={point.x}
              cy={point.y}
              r="4"
              className="fill-white stroke-accent dark:fill-obsidian-raised"
              strokeWidth="2"
            />
            <text
              x={point.x}
              y={point.y - 18}
              textAnchor={anchor === "start" ? "middle" : "middle"}
              className="fill-zinc-900 text-[15px] font-bold dark:fill-zinc-100"
              style={{ fontFamily: "var(--font-mono)" }}
            >
              {code}
            </text>
          </g>
        ))}

        {/* Aircraft, positioned along the arc by progress */}
        <motion.g
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.6, duration: 0.4 }}
        >
          <g transform={`translate(${planeX} ${planeY}) rotate(${headingDeg})`}>
            <circle r="11" className="fill-white dark:fill-obsidian-raised" />
            <circle
              r="11"
              className="fill-none stroke-zinc-200 dark:stroke-white/10"
              strokeWidth="1"
            />
            {/* Nose-right aircraft glyph so rotation matches the tangent */}
            <path
              d="M -6 0 L 3 0 M 3 0 L 6 0 M -2 -4 L 2 0 L -2 4 M -6 -3 L -6 3"
              className="stroke-accent"
              strokeWidth="1.8"
              strokeLinecap="round"
              fill="none"
            />
          </g>
        </motion.g>
      </svg>
    </div>
  );
}
