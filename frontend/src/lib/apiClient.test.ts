import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AxiosAdapter } from "axios";
import { apiClient, setSessionExpiredHandler } from "./apiClient";
import { getAuthToken, setAuthToken } from "./authToken";

/**
 * Replaces the network with a canned status, so the interceptors are exercised
 * for real rather than stubbed out.
 */
function respondWith(status: number): AxiosAdapter {
  return (config) =>
    status >= 400
      ? Promise.reject(
          Object.assign(new Error(`Request failed with status ${status}`), {
            isAxiosError: true,
            config,
            response: { status, data: {}, statusText: "", headers: {}, config },
          }),
        )
      : Promise.resolve({
          data: {},
          status,
          statusText: "OK",
          headers: {},
          config,
        });
}

describe("apiClient", () => {
  beforeEach(() => {
    setAuthToken(null);
    setSessionExpiredHandler(null);
  });

  describe("request interceptor", () => {
    it("attaches the bearer token when one is stored", async () => {
      setAuthToken("a.b.c");
      let seen: string | undefined;
      apiClient.defaults.adapter = (config) => {
        seen = config.headers?.Authorization as string | undefined;
        return respondWith(200)(config);
      };

      await apiClient.get("/flights");

      expect(seen).toBe("Bearer a.b.c");
    });

    it("sends no Authorization header when signed out", async () => {
      let seen: unknown;
      apiClient.defaults.adapter = (config) => {
        seen = config.headers?.Authorization;
        return respondWith(200)(config);
      };

      await apiClient.get("/flights");

      expect(seen).toBeUndefined();
    });
  });

  describe("session expiry", () => {
    it("clears the token and notifies when a request with a token is rejected", async () => {
      setAuthToken("stale.token.here");
      const onExpired = vi.fn();
      setSessionExpiredHandler(onExpired);
      apiClient.defaults.adapter = respondWith(401);

      await expect(apiClient.get("/bookings")).rejects.toThrow();

      expect(getAuthToken()).toBeNull();
      expect(onExpired).toHaveBeenCalledTimes(1);
    });

    /**
     * A 401 from the auth endpoints means those credentials were wrong, which
     * the sign-in form reports itself. Treating it as an expired session would
     * clear state the user never had and reset the form under them.
     */
    it("does not treat a failed sign-in as an expired session", async () => {
      const onExpired = vi.fn();
      setSessionExpiredHandler(onExpired);
      apiClient.defaults.adapter = respondWith(401);

      await expect(
        apiClient.post("/auth/login", { email: "a@b.c", password: "wrong" }),
      ).rejects.toThrow();

      expect(onExpired).not.toHaveBeenCalled();
    });

    it("ignores a 401 when there was no token to invalidate", async () => {
      const onExpired = vi.fn();
      setSessionExpiredHandler(onExpired);
      apiClient.defaults.adapter = respondWith(401);

      await expect(apiClient.get("/bookings")).rejects.toThrow();

      expect(onExpired).not.toHaveBeenCalled();
    });

    /**
     * 403 means the caller is known but not permitted — a customer opening an
     * admin link. Signing them out for that would be wrong, which is why the
     * API distinguishes the two statuses.
     */
    it("leaves the session alone on a 403", async () => {
      setAuthToken("good.token.here");
      const onExpired = vi.fn();
      setSessionExpiredHandler(onExpired);
      apiClient.defaults.adapter = respondWith(403);

      await expect(apiClient.get("/bookings/status/CONFIRMED")).rejects.toThrow();

      expect(getAuthToken()).toBe("good.token.here");
      expect(onExpired).not.toHaveBeenCalled();
    });

    it("still rejects the promise so the caller can show its own error", async () => {
      setAuthToken("stale.token.here");
      setSessionExpiredHandler(vi.fn());
      apiClient.defaults.adapter = respondWith(401);

      await expect(apiClient.get("/bookings")).rejects.toMatchObject({
        response: { status: 401 },
      });
    });
  });
});
