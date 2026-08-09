import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { FlightResultsList, type ResultSection } from "./FlightResultsList";
import { CurrencyContext } from "../lib/currency";
import type { FlightSearchResponseDto } from "../types/flight";

const flight = (over: Partial<FlightSearchResponseDto> = {}): FlightSearchResponseDto => ({
  id: 1,
  flightNumber: "EI156",
  airlineName: "Aer Lingus",
  airlineCode: "EI",
  departureAirport: "DUB",
  arrivalAirport: "LHR",
  departureCity: "Dublin",
  arrivalCity: "London",
  departureTime: "2026-08-01T05:30:00",
  arrivalTime: "2026-08-01T06:55:00",
  departureTimezone: "Europe/Dublin",
  arrivalTimezone: "Europe/London",
  duration: 85,
  price: 89.99,
  availableSeats: 150,
  aircraft: "Airbus A320",
  ...over,
});

const section = (over: Partial<ResultSection> = {}): ResultSection => ({
  id: "outbound",
  title: "Outbound",
  flights: [flight()],
  selectedFlightId: null,
  ...over,
});

/** The real provider reads exchange rates from the API; this is enough to render. */
function renderList(props: Partial<Parameters<typeof FlightResultsList>[0]> = {}) {
  const currency = {
    currencies: [],
    selectedCode: "EUR",
    setSelectedCode: vi.fn(),
    formatPrice: (n: number) => `€${n.toFixed(2)}`,
  };

  return render(
    <MemoryRouter>
      <CurrencyContext.Provider value={currency as never}>
        <FlightResultsList
          status="success"
          errorMessage={null}
          sections={[section()]}
          onSelectFlight={vi.fn()}
          {...props}
        />
      </CurrencyContext.Provider>
    </MemoryRouter>,
  );
}

describe("FlightResultsList", () => {
  describe("price presentation", () => {
    it("shows the exact fare and the cabin's remaining seats when a cabin was searched", () => {
      renderList({ priceMode: "exact" });

      expect(screen.getByText("€89.99")).toBeInTheDocument();
      expect(screen.getByText(/150 seats left/)).toBeInTheDocument();
      expect(screen.queryByText("from")).not.toBeInTheDocument();
    });

    /**
     * A browse view is answered with the cheapest cabin's fare but the whole
     * aircraft's availability, so the seat count does not describe seats at
     * that price. Showing it once claimed 315 seats were available at a fare
     * only 284 of them cost.
     */
    it("shows a lead-in fare and hides the seat count on a browse view", () => {
      renderList({ priceMode: "from" });

      expect(screen.getByText("from")).toBeInTheDocument();
      expect(screen.getByText("€89.99")).toBeInTheDocument();
      expect(screen.queryByText(/seats left/)).not.toBeInTheDocument();
    });

    it("defaults to the exact reading so a caller cannot get 'from' by accident", () => {
      renderList();
      expect(screen.getByText(/150 seats left/)).toBeInTheDocument();
    });
  });

  describe("heading", () => {
    it("shows the cabin the results are for beside the heading", () => {
      renderList({ headingNote: "Business" });

      const heading = screen.getByRole("heading", { name: "Search results" });
      expect(heading).toBeInTheDocument();
      expect(screen.getByText("Business")).toBeInTheDocument();
    });

    it("omits the qualifier when none is given", () => {
      renderList();
      expect(screen.queryByText("Business")).not.toBeInTheDocument();
    });
  });

  describe("states", () => {
    it("renders nothing at all before a search has run", () => {
      const { container } = renderList({ status: "idle" });
      expect(container).toBeEmptyDOMElement();
    });

    it("reports the error rather than an empty list when the search failed", () => {
      renderList({ status: "error", errorMessage: "Network unreachable" });
      expect(screen.getByText("Network unreachable")).toBeInTheDocument();
    });

    it("explains an empty result set", () => {
      renderList({
        sections: [section({ flights: [] })],
        emptyMessage: "No flights found for this route and date.",
      });
      expect(
        screen.getByText("No flights found for this route and date."),
      ).toBeInTheDocument();
    });
  });

  describe("selection", () => {
    it("passes the chosen flight up with the section it came from", async () => {
      const onSelectFlight = vi.fn();
      renderList({ onSelectFlight });

      await userEvent.click(screen.getByRole("button", { name: "Book" }));

      expect(onSelectFlight).toHaveBeenCalledTimes(1);
      const [sectionId, chosen] = onSelectFlight.mock.calls[0];
      expect(sectionId).toBe("outbound");
      expect(chosen.flightNumber).toBe("EI156");
    });

    it("marks the selected flight rather than offering to book it again", () => {
      renderList({ sections: [section({ selectedFlightId: 1 })] });

      expect(screen.getByRole("button", { name: /Selected/ })).toBeInTheDocument();
      expect(screen.queryByRole("button", { name: "Book" })).not.toBeInTheDocument();
    });
  });

  describe("times", () => {
    it("renders each end in its own airport's zone", () => {
      renderList();
      // 05:30 UTC is 06:30 in Dublin and 06:30 in London (both UTC+1 in August).
      expect(screen.getByText("06:30 AM")).toBeInTheDocument();
      expect(screen.getByText("07:55 AM")).toBeInTheDocument();
    });

    it("marks an arrival that lands on a later day", () => {
      renderList({
        sections: [
          section({
            flights: [
              flight({
                departureTime: "2026-08-01T22:30:00",
                arrivalTime: "2026-08-02T01:15:00",
              }),
            ],
          }),
        ],
      });

      expect(screen.getByText("+1")).toBeInTheDocument();
    });

    it("does not mark a same-day arrival", () => {
      renderList();
      expect(screen.queryByText("+1")).not.toBeInTheDocument();
    });
  });

  describe("multiple legs", () => {
    it("labels each leg when an itinerary has more than one", () => {
      renderList({
        sections: [
          section({ id: "leg-0", title: "Leg 1" }),
          section({ id: "leg-1", title: "Leg 2", flights: [flight({ id: 2 })] }),
        ],
      });

      expect(screen.getByText("Leg 1")).toBeInTheDocument();
      expect(screen.getByText("Leg 2")).toBeInTheDocument();
    });

    it("keeps each leg's selection separate", async () => {
      const onSelectFlight = vi.fn();
      renderList({
        onSelectFlight,
        sections: [
          section({ id: "leg-0", title: "Leg 1" }),
          section({ id: "leg-1", title: "Leg 2", flights: [flight({ id: 2 })] }),
        ],
      });

      const buttons = screen.getAllByRole("button", { name: "Select" });
      await userEvent.click(buttons[1]);

      expect(onSelectFlight.mock.calls[0][0]).toBe("leg-1");
    });
  });

  describe("filters and toolbars", () => {
    it("renders a toolbar between the heading and the results", () => {
      renderList({ toolbar: <input aria-label="Filter" /> });
      expect(screen.getByLabelText("Filter")).toBeInTheDocument();
    });

    it("shows the departure date only when asked", () => {
      const { rerender } = renderList({ showDates: false });
      expect(screen.queryByText(/Aug 1/)).not.toBeInTheDocument();

      rerender(
        <MemoryRouter>
          <CurrencyContext.Provider
            value={
              {
                currencies: [],
                selectedCode: "EUR",
                setSelectedCode: vi.fn(),
                formatPrice: (n: number) => `€${n.toFixed(2)}`,
              } as never
            }
          >
            <FlightResultsList
              status="success"
              errorMessage={null}
              sections={[section()]}
              onSelectFlight={vi.fn()}
              showDates
            />
          </CurrencyContext.Provider>
        </MemoryRouter>,
      );

      expect(within(document.body).getByText(/Aug 1/)).toBeInTheDocument();
    });
  });
});
