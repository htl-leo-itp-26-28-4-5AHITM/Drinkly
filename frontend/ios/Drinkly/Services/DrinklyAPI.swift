import Foundation

/// Talks to the Quarkus backend (`/entries` and the `/events` stream).
struct DrinklyAPI {
    let baseUrl: String

    struct ServerError: LocalizedError {
        let message: String
        var errorDescription: String? { message }
    }

    private struct Record: Decodable {
        let id: Int
        let timestamp: String?
        let drinkingAmount: Double
    }

    private struct NewRecord: Encodable {
        struct User: Encodable { let username: String }
        let user: User
        let drinkingAmount: Double
    }

    func entries(for username: String) async throws -> [DrinkEntry] {
        let data = try await send(request("GET", "/entries/" + encode(username)))
        return try JSONDecoder().decode([Record].self, from: data).map { record in
            DrinkEntry(
                id: record.id,
                timestamp: record.timestamp.flatMap(Self.parseDate) ?? .now,
                liters: record.drinkingAmount
            )
        }
    }

    func add(username: String, milliliters: Int) async throws {
        var req = try request("POST", "/entries")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONEncoder().encode(
            NewRecord(user: .init(username: username), drinkingAmount: Double(milliliters) / 1000)
        )
        _ = try await send(req)
    }

    func delete(id: Int) async throws {
        _ = try await send(request("DELETE", "/entries/\(id)"))
    }

    /// Server-sent events: the backend says "connected" when the stream opens and
    /// "changed" whenever an entry is added or removed. Returns when the stream ends.
    func listen(onEvent: (String) async -> Void) async throws {
        var req = try request("GET", "/events")
        req.setValue("text/event-stream", forHTTPHeaderField: "Accept")
        req.timeoutInterval = 24 * 60 * 60
        let (bytes, response) = try await URLSession.shared.bytes(for: req)
        guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
            throw ServerError(message: "Event stream unavailable")
        }
        for try await line in bytes.lines where line.hasPrefix("data:") {
            await onEvent(line.dropFirst(5).trimmingCharacters(in: .whitespaces))
        }
    }

    private func request(_ method: String, _ path: String) throws -> URLRequest {
        let base = baseUrl.hasSuffix("/") ? String(baseUrl.dropLast()) : baseUrl
        guard let url = URL(string: base + path) else {
            throw ServerError(message: "Invalid server address")
        }
        var req = URLRequest(url: url, timeoutInterval: 10)
        req.httpMethod = method
        return req
    }

    private func send(_ req: URLRequest) async throws -> Data {
        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse else { return data }
        guard (200..<300).contains(http.statusCode) else {
            let text = String(data: data, encoding: .utf8) ?? ""
            throw ServerError(message: text.isEmpty ? "Server answered \(http.statusCode)" : text)
        }
        return data
    }

    private func encode(_ segment: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-._~"))
        return segment.addingPercentEncoding(withAllowedCharacters: allowed) ?? segment
    }

    /// The backend sends e.g. "2026-11-01T08:15:00.000+00:00".
    static func parseDate(_ text: String) -> Date? {
        let withFraction = ISO8601DateFormatter()
        withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return withFraction.date(from: text) ?? ISO8601DateFormatter().date(from: text)
    }
}
