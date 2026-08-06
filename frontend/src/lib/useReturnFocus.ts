import { useEffect, useState } from "react";

/**
 * Returns focus to whatever was focused before an overlay opened.
 *
 * <p>Radix restores focus itself when a dialog closes while its `Root` stays
 * mounted. These dialogs are instead unmounted by their parent the instant
 * they close — `{isOpen && <Modal />}` — so the close transition Radix would
 * restore focus on never happens, and focus falls back to `<body>`. A
 * keyboard user then has to tab from the top of the page again.
 *
 * <p>The opener is captured in a `useState` initialiser rather than an effect
 * because effects run child-first: by the time this component's effect ran,
 * Radix would already have moved focus into the dialog and the original
 * element would be lost.
 */
export function useReturnFocus(): void {
  const [opener] = useState(() =>
    typeof document === "undefined"
      ? null
      : (document.activeElement as HTMLElement | null),
  );

  useEffect(
    () => () => {
      // The opener can itself be gone — a row's edit button, when the row was
      // deleted by the very dialog that is closing.
      if (opener?.isConnected) opener.focus();
    },
    [opener],
  );
}
