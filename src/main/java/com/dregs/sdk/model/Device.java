package com.dregs.sdk.model;

import java.time.Instant;
import java.util.Map;

/**
 * A browser or device Dregs has seen, identified by its fingerprint.
 *
 * @param fingerprint the device's fingerprint
 * @param ip the address it was last seen at
 * @param asn the autonomous system that address belongs to, such as {@code "AS15169"}
 * @param asnLabel the autonomous system's name
 * @param userAgent the browser's user agent
 * @param city the city the address places it in
 * @param region the region the address places it in
 * @param country the country the address places it in
 * @param countryCode the two-letter country code
 * @param timezone the time zone the browser reported
 * @param latitude the latitude the address places it at
 * @param longitude the longitude the address places it at
 * @param disregarded whether the device no longer links the identities seen on it
 * @param firstSeenAt when Dregs first saw it
 * @param lastSeenAt when Dregs last saw it
 * @param raw the payload this was built from
 */
public record Device(
        String fingerprint,
        String ip,
        String asn,
        String asnLabel,
        String userAgent,
        String city,
        String region,
        String country,
        String countryCode,
        String timezone,
        Double latitude,
        Double longitude,
        boolean disregarded,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes a defensive copy of the raw payload.
     */
    public Device {
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a device from a parsed {@code GET /api/devices/{fingerprint}} response.
     *
     * @param payload the parsed device object
     * @return the device
     */
    public static Device fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new Device(
                ApiData.string(body, "fingerprint"),
                ApiData.string(body, "ip"),
                ApiData.string(body, "asn"),
                ApiData.string(body, "asnLabel"),
                ApiData.string(body, "userAgent"),
                ApiData.string(body, "city"),
                ApiData.string(body, "region"),
                ApiData.string(body, "country"),
                ApiData.string(body, "countryCode"),
                ApiData.string(body, "timezone"),
                ApiData.decimal(body, "latitude"),
                ApiData.decimal(body, "longitude"),
                ApiData.bool(body, "disregarded"),
                ApiData.instant(body, "firstSeenAt"),
                ApiData.instant(body, "lastSeenAt"),
                body);
    }
}
