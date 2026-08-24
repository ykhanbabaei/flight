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

function App(): React.ReactElement {
  const [expandedBookingId, setExpandedBookingId] = useState<string | null>("ABC123");
  const [bookings, setBookings] = useState<FlightBooking[]>([]);
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
            const jsonData = JSON.parse(event.data) as FlightBooking[];
            setBookings(jsonData);
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
      <h1>Flight Bookings</h1>
    <button onClick={() => loadBookingsWithAlternatives()} disabled={alternativeButtonLoading}>
      {alternativeButtonLoading ? "Loading..." : "Find Alternative flights"}
    </button>
    <div className="booking-list">
        {bookings.map((booking) => {
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

export default App;