import { useState } from "react";
import "./App.css";

type AlternativeFlight = {
  id: string;
  airline: string;
  trainOperator: string;
  from: string;
  to: string;
  time: string;
  duration: string;
  price: string;
  routeType: string;
};

type FlightBooking = {
  id: string;
  passengerCount: number;
  flight: string;
  route: string;
  departure: string;
  status: "Disrupted" | "Pending";
  alternatives: AlternativeFlight[];
};

type FlightDisruption = {
  flight: string;
  origin: string;
  destination: string;
  scheduledDeparture: string; // ISO 8601 datetime string
  scheduledArrival: string;   // ISO 8601 datetime string
  status: string;
  reason: string;
  cancelledAt: string | null; // ISO 8601 datetime string or null
  affectedBookings: number;
  affectedPassengers: number;
  flightBookings: FlightBooking[];
};

function App(): React.ReactElement {
  const [expandedBookingId, setExpandedBookingId] = useState<string | null>("ABC123");
  const [flightDisruption, setFlightDisruption] = useState<FlightDisruption>();
  const [error, setError] = useState<string | null>(null);
  const [alternativeButtonLoading, setAlternativeButtonLoading] = useState(false);


  const loadBookingsWithAlternatives = async (): Promise<void> => {

    const flightNumber = "EW 4711";
    const scheduledDeparture = "2026-07-21T18:35:00+02:00";

    const url =
      `/api/v1/flights/stream-alternatives/${encodeURIComponent(flightNumber)}` +
      `?${new URLSearchParams({
        scheduledDeparture,
      }).toString()}`;

      const eventSource = new EventSource(url);
      setAlternativeButtonLoading(true);


      const handleMessage = (event: MessageEvent<string>): void => {
          try {
            const jsonData = JSON.parse(event.data) as FlightDisruption;
            setFlightDisruption(jsonData);
          } catch (error) {
                  setError(
                    error instanceof Error
                      ? error.message
                      : "Something went wrong"
                  );
          }
      };

      eventSource.addEventListener("alternatives-update", handleMessage);
      eventSource.addEventListener('complete', () => {
          setAlternativeButtonLoading(false);
          eventSource.close();
      });

      eventSource.addEventListener('error', () => {
          eventSource.close();
          setAlternativeButtonLoading(false);
      });


      eventSource.onerror = () => {
          eventSource.close();
          setAlternativeButtonLoading(false);
      };

    };

  const toggleBooking = (bookingId: string): void => {
    setExpandedBookingId((currentId) =>
      currentId === bookingId ? null : bookingId
    );
  };

  const handleSelectFlight = (
    bookingId: string,
    alternative: AlternativeFlight
  ): void => {
    console.log(
      `Selected ${alternative.airline} for booking ${bookingId}`
    );
  };

  if (error) {
    return (
      <div>
        <p>Error: {error}</p>
        <button onClick={() => loadBookingsWithAlternatives()}>
          Try again
        </button>
      </div>
    );
  }

  return (
    <main className="container">
    <button onClick={() => loadBookingsWithAlternatives()} disabled={alternativeButtonLoading}>
      {alternativeButtonLoading ? "Loading..." : "Find Alternative flights"}
    </button>
    {flightDisruption && (
        <FlightDisruptionCard
            key={flightDisruption.flight}
            disruption={flightDisruption}
        />
    )}
    <div className="booking-list">
        {flightDisruption?.flightBookings?.map((booking) => {
          const isExpanded =
            expandedBookingId === booking.id;

          return (
            <section
              className="booking-card"
              key={booking.id}
            >
              <button
                type="button"
                className="booking-header"
                onClick={() => toggleBooking(booking.id)}
              >
                <strong className="booking-id">
                  {booking.id}
                </strong>

                <span className="passenger-count">{booking.passengerCount}</span>

                <div className="flight">

                  <span className="route">
                    {booking.route}
                  </span>
                </div>

                <span>{booking.departure}</span>

                <span className="arrow">
                  {isExpanded ? "⌃" : "⌄"}
                </span>
              </button>

              {isExpanded && (
                <div className="alternatives">
                  <h2>Alternative Flights</h2>

                  {booking.alternatives.length === 0 ? (
                    <p>No alternative flights available.</p>
                  ) : (
                    booking.alternatives.map(
                      (alternative) => (
                        <AlternativeFlightCard
                          key={alternative.id}
                          alternative={alternative}
                          onSelect={() =>
                            handleSelectFlight(
                              booking.id,
                              alternative
                            )
                          }
                        />
                      )
                    )
                  )}
                </div>
              )}
            </section>
          );
        })}
      </div>
    </main>
  );
}

type AlternativeFlightCardProps = {
  alternative: AlternativeFlight;
  onSelect: () => void;
};

function AlternativeFlightCard({
  alternative,
}: AlternativeFlightCardProps): React.ReactElement {
  return (
    <div
      className={`alternative-card`}
    >
      <div className="flight-info">
        <strong>{alternative.airline}</strong>

        <span>{alternative.trainOperator}</span>

        <small>★ {alternative.routeType}</small>
      </div>

      <div>{alternative.from}  {'→'} {alternative.to}</div>

      <div className="flight-time">
        <strong>{alternative.time}</strong>

        <span>{alternative.duration}</span>
      </div>

      <span>{alternative.price}€</span>

    </div>
  );
}


type FlightDisruptionCardProps = {
  disruption: FlightDisruption;
};

export function FlightDisruptionCard({
   disruption,
 }: FlightDisruptionCardProps) {
  const formatDateTime = (date: string) =>
      new Intl.DateTimeFormat("en-DE", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(date));

  return (
      <div className="flight-disruption-card">
        {/* Line 1: Flight and route */}
        <div className="flight-disruption-card__header">
          <strong>{disruption.flight}</strong>

          <span className="flight-disruption-card__route">
          {disruption.origin}
            <span className="flight-disruption-card__arrow">→</span>
            {disruption.destination}
        </span>

          <span
              className={`flight-disruption-card__status flight-disruption-card__status--${disruption.status.toLowerCase()}`}
          >
          {disruption.status}
        </span>
        </div>

        {/* Line 2: Schedule */}
        <div className="flight-disruption-card__schedule">
        <span>
          Departure: {formatDateTime(disruption.scheduledDeparture)}
        </span>

          <span>
          Arrival: {formatDateTime(disruption.scheduledArrival)}
        </span>
        </div>

        {/* Line 3: Disruption information */}
        <div className="flight-disruption-card__details">
        <span className="flight-disruption-card__reason">
          {disruption.reason}
        </span>

          <span>
          {disruption.affectedBookings} bookings ·{" "}
            {disruption.affectedPassengers} passengers
        </span>
        </div>
      </div>
  );
}

export default App;