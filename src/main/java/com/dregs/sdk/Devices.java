package com.dregs.sdk;

import com.dregs.sdk.model.Device;
import com.dregs.sdk.model.Page;

/**
 * Reading the devices Dregs has seen, and disregarding them.
 *
 * <p>Reach one through {@link Dregs#devices()}.
 */
public final class Devices {

    private final Dregs client;

    Devices(Dregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of devices, most recently seen first.
     *
     * @return the page
     */
    public Page<Device> list() {
        return list(null);
    }

    /**
     * Returns one page of devices.
     *
     * <p>Use {@link #listAll(DeviceListParams)} to walk every page.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return the page
     */
    public Page<Device> list(DeviceListParams params) {
        ListCall<Device> call = Calls.devicesList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every device, fetching pages as the iteration reaches them.
     *
     * @return the devices
     */
    public Iterable<Device> listAll() {
        return listAll(null);
    }

    /**
     * Every device that matches, fetching pages as the iteration reaches them.
     *
     * <p>Nothing is fetched until the loop starts, and each new loop starts a fresh walk.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return the devices
     */
    public Iterable<Device> listAll(DeviceListParams params) {
        return client.walk(Calls.devicesList(params));
    }

    /**
     * Returns the device with this fingerprint.
     *
     * @param fingerprint the device's fingerprint
     * @return the device
     * @throws com.dregs.sdk.exception.NotFoundException when no device has this fingerprint
     * @throws IllegalArgumentException when the fingerprint is empty
     */
    public Device get(String fingerprint) {
        return client.run(Calls.devicesGet(fingerprint));
    }

    /**
     * Sets whether the device is disregarded, and returns it.
     *
     * <p>A disregarded device, such as a shared office machine, no longer links the identities seen
     * on it.
     *
     * @param fingerprint the device's fingerprint
     * @param disregarded true to disregard the device, false to count it again
     * @return the device, as it is now
     * @throws com.dregs.sdk.exception.NotFoundException when no device has this fingerprint
     * @throws IllegalArgumentException when the fingerprint is empty
     */
    public Device update(String fingerprint, boolean disregarded) {
        return client.run(Calls.devicesUpdate(fingerprint, disregarded));
    }
}
