type NotificationHandlers = {
    onConnected?: () => void;
    onNotification?: (payload: any) => void;
    onIdolMessageStack?: (payload: any) => void;
    onError?: () => void;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

const parseSseChunk = (
    chunk: string,
    handlers: NotificationHandlers
) => {
    const events = chunk.split("\n\n");

    for (const eventBlock of events) {
        if (!eventBlock.trim()) continue;

        let eventName = "";
        const dataLines: string[] = [];

        const lines = eventBlock.split("\n");

        for (const line of lines) {
            if (line.startsWith("event:")) {
                eventName = line.slice(6).trim();
            } else if (line.startsWith("data:")) {
                dataLines.push(line.slice(5).trim());
            }
        }

        const rawData = dataLines.join("\n");

        if (eventName === "connected") {
            handlers.onConnected?.();
            continue;
        }

        if (!rawData) continue;

        try {
            const payload = JSON.parse(rawData);

            if (eventName === "notification") {
                handlers.onNotification?.(payload);
            } else if (eventName === "idol_message_stack") {
                handlers.onIdolMessageStack?.(payload);
            }
        } catch (error) {
            console.error("SSE payload parse error", error);
        }
    }
};

export const connectNotificationSse = async (
    accessToken: string,
    handlers: NotificationHandlers
) => {
    const controller = new AbortController();

    try {
        const response = await fetch(`${API_BASE_URL}/sse/notifications`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${accessToken}`,
                Accept: "text/event-stream",
            },
            signal: controller.signal,
        });

        if (!response.ok || !response.body) {
            handlers.onError?.();
            return {
                close: () => controller.abort(),
            };
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");

        let buffer = "";

        const read = async () => {
            while (true) {
                const { done, value } = await reader.read();

                if (done) break;

                buffer += decoder.decode(value, { stream: true });

                const lastDoubleNewLineIndex = buffer.lastIndexOf("\n\n");

                if (lastDoubleNewLineIndex !== -1) {
                    const completeChunk = buffer.slice(0, lastDoubleNewLineIndex);
                    buffer = buffer.slice(lastDoubleNewLineIndex + 2);

                    parseSseChunk(completeChunk, handlers);
                }
            }
        };

        read().catch((error) => {
            if (!controller.signal.aborted) {
                console.error("SSE read error", error);
                handlers.onError?.();
            }
        });
    } catch (error) {
        if (!controller.signal.aborted) {
            console.error("SSE connection error", error);
            handlers.onError?.();
        }
    }

    return {
        close: () => controller.abort(),
    };
};