import { motion } from "framer-motion";
import { EUROPE_LAND, EUROPE_LAND_BOUNDS } from "./europeLand";
import { WORLD_LAND } from "./worldLand";

/**
 * A minimal route map: the two airports projected onto a local equirectangular
 * plane, joined by a curved arc with the aircraft placed along it by progress,
 * over simplified coastlines drawn through the same projection.
 *
 * Deliberately not a tile map — a raster basemap would be light-only and clash
 * with the dark theme, and would need a key/attribution. The coastlines are
 * plain vector rings coloured with currentColor, so it still themes cleanly and
 * ships no dependencies.
 *
 * Two coastline sets are carried: a detailed European one for routes that stay
 * inside its clip, and a coarse world one for everything else. Which applies is
 * decided by whether the visible window fits the regional bounds, since a short
 * hop can still sit well outside them.
 */

const VIEW_WIDTH = 600;
const VIEW_HEIGHT = 260;
const PADDING_X = 70;
const PADDING_Y = 70;

/**
 * Never zoom in past this many degrees of longitude. A short hop like DUB–LHR
 * spans under 6 degrees, and framed to itself it would fill the card with open
 * water; holding a floor keeps recognisable land in shot. Long routes exceed it
 * on their own and frame exactly as they did before.
 */
const MIN_SPAN_DEGREES = 12;

/**
 * The floor when the frame falls back to world geometry. That set is simplified
 * ten times more coarsely, so a short hop outside Europe — SYD–MEL, HND–ICN —
 * has to sit further back for the coastline to stay believable.
 */
const MIN_SPAN_DEGREES_WORLD = 30;

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

  // Unwrap the arrival past the antimeridian when that is the shorter way
  // round. SYD is at +151 and LAX at -118: taken literally that is 270 degrees
  // eastward across Eurasia, when the flight actually crosses the Pacific.
  const arrivalLongitudeUnwrapped =
    Math.abs(arrivalLongitude - departureLongitude) > 180
      ? arrivalLongitude + (arrivalLongitude < departureLongitude ? 360 : -360)
      : arrivalLongitude;

  const xs = [departureLongitude * lonScale, arrivalLongitudeUnwrapped * lonScale];
  const ys = [-departureLatitude, -arrivalLatitude];

  const centreX = (Math.min(...xs) + Math.max(...xs)) / 2;
  const centreY = (Math.min(...ys) + Math.max(...ys)) / 2;

  // Preserve aspect ratio so short hops don't get blown up to fill the frame.
  const usableWidth = VIEW_WIDTH - PADDING_X * 2;
  const usableHeight = VIEW_HEIGHT - PADDING_Y * 2;

  /**
   * Builds the projection for a given zoom floor, along with the geographic
   * window it ends up showing. The floor is applied on the x axis and carried
   * to y by the frame's aspect ratio, so raising it zooms out evenly rather
   * than stretching the route. The epsilon guards a zero-size span (same
   * airport, or a due N/S route).
   */
  const frameFor = (minSpanDegrees: number) => {
    const minHalfX = (minSpanDegrees * lonScale) / 2;
    const halfX = Math.max((Math.max(...xs) - Math.min(...xs)) / 2, minHalfX, 0.0001);
    const halfY = Math.max(
      (Math.max(...ys) - Math.min(...ys)) / 2,
      (minHalfX * usableHeight) / usableWidth,
      0.0001,
    );
    const scale = Math.min(usableWidth / (halfX * 2), usableHeight / (halfY * 2));

    // Centre on the route's midpoint rather than the corner of its bounding
    // box, so the extra room a floored span buys is shared evenly around it.
    const project = (lon: number, lat: number) => ({
      x: (lon * lonScale - centreX) * scale + VIEW_WIDTH / 2,
      y: (-lat - centreY) * scale + VIEW_HEIGHT / 2,
    });

    // Invert the projection at the frame's corners to learn what the viewer
    // actually sees, which is what decides whether regional data covers it.
    const lonAt = (x: number) => ((x - VIEW_WIDTH / 2) / scale + centreX) / lonScale;
    const latAt = (y: number) => -((y - VIEW_HEIGHT / 2) / scale + centreY);

    return {
      project,
      window: {
        minLon: lonAt(0),
        maxLon: lonAt(VIEW_WIDTH),
        minLat: latAt(VIEW_HEIGHT),
        maxLat: latAt(0),
      },
    };
  };

  // Regional data is clipped to a box, so the test that matters is whether the
  // visible window fits inside it — not how long the route is. A short hop over
  // Cairo spans few degrees but sits outside the European clip entirely.
  const regional = frameFor(MIN_SPAN_DEGREES);
  const useRegional =
    regional.window.minLon >= EUROPE_LAND_BOUNDS.minLon &&
    regional.window.maxLon <= EUROPE_LAND_BOUNDS.maxLon &&
    regional.window.minLat >= EUROPE_LAND_BOUNDS.minLat &&
    regional.window.maxLat <= EUROPE_LAND_BOUNDS.maxLat;

  const frame = useRegional ? regional : frameFor(MIN_SPAN_DEGREES_WORLD);
  const { project, window: view } = frame;
  const land = useRegional ? EUROPE_LAND : WORLD_LAND;

  // Once the route is unwrapped the window can run past +/-180, so the same
  // landmass may need drawing at a shifted longitude to appear on both sides of
  // the seam. Only the copies that actually intersect the window are built.
  const lonOffsets = [-360, 0, 360].filter(
    (offset) => view.minLon <= 180 + offset && view.maxLon >= -180 + offset,
  );

  // One path for every landmass; evenodd makes inland water read as holes.
  const landPath = lonOffsets
    .flatMap((offset) =>
      land.map((ring) => {
        let d = "";
        for (let i = 0; i < ring.length; i++) {
          const { x, y } = project(ring[i][0] + offset, ring[i][1]);
          d += `${i === 0 ? "M" : "L"}${x.toFixed(1)} ${y.toFixed(1)}`;
        }
        return `${d}Z`;
      }),
    )
    .join("");

  const from = project(departureLongitude, departureLatitude);
  const to = project(arrivalLongitudeUnwrapped, arrivalLatitude);

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

        {/* Landmasses, over the grid so the grid reads as open water */}
        <path
          d={landPath}
          fillRule="evenodd"
          className="fill-zinc-200/70 stroke-zinc-300/80 dark:fill-white/[0.05] dark:stroke-white/10"
          strokeWidth="0.75"
          strokeLinejoin="round"
        />

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
