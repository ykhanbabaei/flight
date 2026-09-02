# Flight Disruption — Alternative Options Finder

A prototype that takes a cancelled flight, searches three alternative-transport sources
(Lufthansa Group flights, other airlines, and trains) **in parallel**, ranks the options, and
recommends one alternative journey per affected booking — streamed to a React UI as results
come in, so the service team never has to wait for the slowest source before seeing anything.


## Tech stack

- Java 21, Spring Boot 4.1.0, Maven
- React + Vite + TypeScript
- Docker / Docker Compose

## Project structure

```
src/main/java   backend application
src/main/resources
  data/         mock source responses (disruption + 3 alternative-transport sources)
web-frontend/   React/Vite frontend
Dockerfile      multi-stage build (frontend build → backend build → runtime)
docker-compose.yml
```

## Prerequisites

- Docker + Docker Compose (recommended — no local Java/Node setup needed)

Optional, for local development without Docker:
- JDK 21, Maven
- Node.js 22+, npm

## Run with Docker

```bash
docker-compose up --build
```

Then open **http://localhost:8080** — the backend serves the built frontend as static
resources, so there's a single origin and no CORS configuration needed in this mode.

## Run locally (development)

Run backend and frontend separately so the frontend hot-reloads:

```bash
# Terminal 1 — backend, port 8080
mvn spring-boot:run

# Terminal 2 — frontend dev server, port 5173
cd web-frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The Vite dev server proxies `/api/**` to `localhost:8080`
(see `vite.config.ts`), and the backend's CORS config explicitly allows `localhost:5173` as an
origin for this mode.

## Using it

The prototype is scoped to one seeded scenario: flight **EW 4711** (Cologne/Bonn → Berlin,
today's date in the mock data), cancelled with 48 affected bookings. Click **"Find Alternative
flights"** in the UI — no other input is needed for this prototype.

### API

```
GET /api/v1/flights/stream-alternatives/{flightNumber}?scheduledDeparture={ISO-8601 offset date-time}
```

Returns a `text/event-stream` (Server-Sent Events):

- `alternatives-update` — fired once per data source as it responds, each time carrying the
  *cumulative* set of bookings with the best alternative found so far across all sources that
  have returned so far. This is what lets the UI show partial, improving results instead of a
  single blocking spinner.
- `complete` — fired once all three sources have either returned or failed.

If the flight number / departure combination doesn't match the seeded disruption, the endpoint
returns `404` with a JSON error body instead of opening a stream.

## How it works

1. **Parallel search.** All three sources (`InternalRoutes`, `ExternalRoutes`, `TrainRoutes`) are
   queried concurrently via `CompletableFuture`. Each source's mock response has a different
   simulated latency, and a failure or slow response from one source never blocks the other two —
   the SSE stream keeps emitting updates as each source completes.
2. **Normalization.** Each source's own response shape (internal Lufthansa Group JSON, external
   airline JSON, train JSON) is mapped into one shared `Route` model with a common
   departure/arrival, seat count, price, and carrier, regardless of source.
3. **Recommendation.** For each booking, `findAndMapRecommendedRoutes` runs a cascading search
   that tries to disturb the passenger's existing itinerary as little as possible, widening scope
   only when a narrower option isn't available:
    1. **Replace just the cancelled leg.** Search only from the cancelled segment's origin to its
       destination. If the booking has an onward connection that will be missed, use *that
       connecting flight's own departure time* as a deadline — i.e. first check whether the
       passenger can still make their own already-booked onward flight.
    2. **Extend forward through missed connections**, one at a time in booking order, if step 1
       finds nothing — searching from the cancelled leg's origin out to each missed segment's
       destination in turn, using the next missed segment's departure (if any) as the new deadline.
    3. **Extend backward through not-yet-departed legs**, if still nothing — walking backward from
       the not-yet-departed segment closest to the cancellation, to reuse as much of the confirmed
       itinerary as possible.
    4. **Whole journey, no deadline**, as a last resort.
    5. If none of the above finds anything, the booking gets an empty alternatives list rather than
       an error.

   Within each of these searches, `RouteFinder` runs a label-setting Dijkstra variant over the
   normalized routes, filtering by seat availability, respecting a minimum connection time, and
   minimizing a weighted score of price, total time (flight + layover), and number of legs.
