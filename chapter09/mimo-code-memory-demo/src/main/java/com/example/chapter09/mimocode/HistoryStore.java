package com.example.chapter09.mimocode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * history 是证据层。它和 memory 最大的区别是：history 尽量保存真实发生
 * 过的轨迹，而 memory 是被整理过的知识缓存。
 */
public final class HistoryStore {
    private final DemoWorkspace workspace;
    private final List<HistoryEvent> events = new ArrayList<>();

    public HistoryStore(DemoWorkspace workspace) {
        this.workspace = workspace;
    }

    public HistoryEvent append(String sessionId, HistoryEvent.EventKind kind, String title, String body) throws IOException {
        HistoryEvent event = new HistoryEvent(events.size() + 1L, sessionId, kind, title, body, Instant.now());
        events.add(event);

        Files.createDirectories(workspace.historyFile().getParent());
        String line = "%d | %s | %s | %s | %s%n".formatted(
                event.id(),
                event.createdAt(),
                event.kind(),
                sanitize(event.title()),
                sanitize(event.body())
        );
        Files.writeString(workspace.historyFile(), line, StandardCharsets.UTF_8,
                Files.exists(workspace.historyFile())
                        ? java.nio.file.StandardOpenOption.APPEND
                        : java.nio.file.StandardOpenOption.CREATE);
        return event;
    }

    public List<HistoryEvent> all() {
        return List.copyOf(events);
    }

    public List<HistoryEvent> search(String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        return events.stream()
                .filter(event -> (event.title() + " " + event.body()).toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    public List<HistoryEvent> around(long eventId, int before, int after) {
        int index = -1;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).id() == eventId) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return List.of();
        }
        int start = Math.max(0, index - before);
        int end = Math.min(events.size(), index + after + 1);
        return List.copyOf(events.subList(start, end));
    }

    private static String sanitize(String value) {
        return value.replace('\n', ' ').replace('|', '/');
    }
}
