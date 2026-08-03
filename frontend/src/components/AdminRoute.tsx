import { Navigate, useLocation } from "react-router-dom";
import { isAdmin, useAuth } from "../lib/auth";
import type { ReactNode } from "react";

/**
 * Restricts a route to admins.
 *
 * Signed-out visitors go to the landing page as they would for any protected
 * route. A signed-in non-admin is sent to their dashboard instead: they are a
 * legitimate user who simply has no business here, and bouncing them to a login
 * screen they are already past would just be confusing.
 *
 * This is a convenience, not the control — the endpoints behind the page are
 * what actually enforce the role.
 */
export function AdminRoute({ children }: { children: ReactNode }) {
  const { user, isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/" replace state={{ from: location.pathname }} />;
  }

  if (!isAdmin(user)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}
