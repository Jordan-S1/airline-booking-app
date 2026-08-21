import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AssistantChat } from "./AssistantChat";
import { CurrencyContext } from "../lib/currency";
import type { AssistantResponseDto } from "../types/assistant";
import type { FlightSearchResponseDto } from "../types/flight";

vi.mock("../api/assistant", () => ({
  askAssistant: vi.fn(),
  getAssistantStatus: vi.fn(),
}));

const { askAssistant, getAssistantStatus } = await import("../api/assistant");

const REAL_ROW: FlightSearchResponseDto = {
  id: 1,
  flightNumber: "EI512",
  airlineName: "Aer Lingus",
  airlineCode: "EI",
  departureAirport: "DUB",
  arrivalAirport: "CDG",
  departureCity: "Dublin",
  arrivalCity: "Paris",
  departureTime: "2026-08-28T09:15:00",
  arrivalTime: "2026-08-28T11:05:00",
  departureTimezone: "Europe/Dublin",
  arrivalTimezone: "Europe/Paris",
  duration: 110,
  price: 89.99,
  availableSeats: 42,
  aircraft: "Airbus A320",
};

const answer = (over: Partial<AssistantResponseDto> = {}): AssistantResponseDto => ({
  reply: "There is one flight to Paris that day.",
  flights: [REAL_ROW],
  interpretedAs: {
    departureAirport: "DUB",
    arrivalAirport: "CDG",
    departureDate: "2026-08-28",
    returnDate: null,
    passengers: 1,
    seatClass: "ECONOMY",
    directFlightsOnly: false,
  },
  needsMoreInfo: false,
  ...over,
});

function renderChat(onBookFlight = vi.fn()) {
  const currency = {
    currencies: [],
    selectedCode: "EUR",
    setSelectedCode: vi.fn(),
    formatPrice: (n: number) => `€${n.toFixed(2)}`,
  };

  render(
    <CurrencyContext.Provider value={currency as never}>
      <AssistantChat onBookFlight={onBookFlight} />
    </CurrencyContext.Provider>,
  );

  return { onBookFlight };
}

async function ask(text = "cheapest flight to Paris next Friday") {
  const input = await screen.findByLabelText("Describe your trip");
  await userEvent.type(input, text);
  await userEvent.click(screen.getByRole("button", { name: "Send" }));
}

describe("AssistantChat", () => {
  beforeEach(() => {
    // Call counts are asserted, and the config does not clear mocks between
    // tests — without this a later "was never called" reads the earlier tests'
    // calls and fails.
    vi.clearAllMocks();
    vi.mocked(getAssistantStatus).mockResolvedValue({ available: true });
    vi.mocked(askAssistant).mockResolvedValue(answer());
  });

  describe("availability", () => {
    it("renders nothing when the backend has no API key", async () => {
      vi.mocked(getAssistantStatus).mockResolvedValue({ available: false });
      const { container } = render(<AssistantChat onBookFlight={vi.fn()} />);

      await waitFor(() => expect(getAssistantStatus).toHaveBeenCalled());
      expect(container).toBeEmptyDOMElement();
    });

    /**
     * A status endpoint that cannot be reached is treated as off. The
     * alternative — showing the box and failing on the first message — spends
     * the traveller's attention to deliver the same "no".
     */
    it("renders nothing when the status check fails", async () => {
      vi.mocked(getAssistantStatus).mockRejectedValue(new Error("network"));
      const { container } = render(<AssistantChat onBookFlight={vi.fn()} />);

      await waitFor(() => expect(getAssistantStatus).toHaveBeenCalled());
      expect(container).toBeEmptyDOMElement();
    });

    it("shows the composer once the backend reports it is configured", async () => {
      renderChat();
      expect(await screen.findByLabelText("Describe your trip")).toBeInTheDocument();
    });
  });

  describe("answers", () => {
    /**
     * The frontend half of the grounding guarantee: the rows on screen come
     * from `flights`, so a reply describing something else cannot put a
     * bookable flight in front of anyone.
     */
    it("renders the returned rows, not flights named only in the prose", async () => {
      vi.mocked(askAssistant).mockResolvedValue(
        answer({
          reply: "Ryanair FR9999 at €19.99 is your cheapest option.",
        }),
      );
      renderChat();
      await ask();

      expect(await screen.findByText("EI512")).toBeInTheDocument();
      expect(screen.getByText("Aer Lingus")).toBeInTheDocument();
      expect(screen.getByText("€89.99")).toBeInTheDocument();
      // The prose is shown as written — but it produced no row of its own.
      expect(screen.queryByText("FR9999")).not.toBeInTheDocument();
      expect(screen.queryByText("€19.99")).not.toBeInTheDocument();
      expect(screen.getAllByRole("button", { name: "Book" })).toHaveLength(1);
    });

    it("shows how the sentence was read", async () => {
      renderChat();
      await ask();

      expect(await screen.findByText("DUB → CDG")).toBeInTheDocument();
      expect(screen.getByText("1 passenger")).toBeInTheDocument();
      expect(screen.getByText("Economy")).toBeInTheDocument();
    });

    it("shows a question on its own when nothing was searched", async () => {
      vi.mocked(askAssistant).mockResolvedValue(
        answer({
          reply: "Which airport are you flying from?",
          flights: [],
          interpretedAs: null,
          needsMoreInfo: true,
        }),
      );
      renderChat();
      await ask("flight to Paris");

      expect(
        await screen.findByText("Which airport are you flying from?"),
      ).toBeInTheDocument();
      expect(screen.queryByRole("button", { name: "Book" })).not.toBeInTheDocument();
    });

    it("books through the ordinary flow with the cabin the assistant searched", async () => {
      vi.mocked(askAssistant).mockResolvedValue(
        answer({
          interpretedAs: {
            departureAirport: "DUB",
            arrivalAirport: "CDG",
            departureDate: "2026-08-28",
            returnDate: null,
            passengers: 2,
            seatClass: "BUSINESS",
            directFlightsOnly: false,
          },
        }),
      );
      const onBookFlight = vi.fn();
      renderChat(onBookFlight);
      await ask();

      await userEvent.click(await screen.findByRole("button", { name: "Book" }));

      expect(onBookFlight).toHaveBeenCalledTimes(1);
      const [flight, search] = onBookFlight.mock.calls[0];
      expect(flight.flightNumber).toBe("EI512");
      expect(search.passengers).toBe(2);
      expect(search.seatClass).toBe("BUSINESS");
    });
  });

  describe("failures", () => {
    it("says the assistant is off rather than 'try again' on a 503", async () => {
      vi.mocked(askAssistant).mockRejectedValue({
        isAxiosError: true,
        response: { status: 503 },
      });
      renderChat();
      await ask();

      expect(await screen.findByText(/assistant is unavailable/i)).toBeInTheDocument();
    });

    it("keeps the transcript when one message fails", async () => {
      vi.mocked(askAssistant).mockRejectedValue(new Error("boom"));
      renderChat();
      await ask("hello there");

      expect(await screen.findByText("hello there")).toBeInTheDocument();
      expect(screen.getByText(/something went wrong/i)).toBeInTheDocument();
    });
  });

  describe("composer", () => {
    it("will not send an empty message", async () => {
      renderChat();
      await screen.findByLabelText("Describe your trip");

      expect(screen.getByRole("button", { name: "Send" })).toBeDisabled();
      expect(askAssistant).not.toHaveBeenCalled();
    });

    it("sends a suggestion when one is clicked", async () => {
      renderChat();
      const suggestion = await screen.findByRole("button", {
        name: /Weekend return to Barcelona/,
      });
      await userEvent.click(suggestion);

      await waitFor(() =>
        expect(askAssistant).toHaveBeenCalledWith(
          "Weekend return to Barcelona",
          null,
        ),
      );
    });

    it("clears the input after sending", async () => {
      renderChat();
      await ask();

      await waitFor(() =>
        expect(screen.getByLabelText("Describe your trip")).toHaveValue(""),
      );
    });
  });
});
