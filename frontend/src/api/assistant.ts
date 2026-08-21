import { apiClient } from "../lib/apiClient";
import type {
  AssistantResponseDto,
  AssistantStatusDto,
} from "../types/assistant";

/**
 * Asks the assistant for flights in plain English.
 *
 * <p>Answers 503 when no API key is configured, which is why callers should
 * check {@link getAssistantStatus} first rather than offering the feature and
 * discovering it is off on the traveller's first message.
 */
export async function askAssistant(
  message: string,
  originHint: string | null = null,
): Promise<AssistantResponseDto> {
  const { data } = await apiClient.post<AssistantResponseDto>("/assistant", {
    message,
    originHint,
  });
  return data;
}

/** Whether the assistant is configured at all. Never throws for "off". */
export async function getAssistantStatus(): Promise<AssistantStatusDto> {
  const { data } = await apiClient.get<AssistantStatusDto>("/assistant/status");
  return data;
}
