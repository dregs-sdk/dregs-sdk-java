package com.dregs.sdk;

import com.dregs.sdk.model.Device;
import com.dregs.sdk.model.Page;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The async twin of {@link Devices}.
 *
 * <p>Reach one through {@link AsyncDregs#devices()}. Each method does what the synchronous one
 * documents; the failures it would throw arrive as the future's cause instead.
 */
public final class AsyncDevices {

    private final AsyncDregs client;

    AsyncDevices(AsyncDregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of devices. See {@link Devices#list()}.
     *
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Device>> list() {
        return list(null);
    }

    /**
     * Returns one page of devices. See {@link Devices#list(DeviceListParams)}.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Device>> list(DeviceListParams params) {
        ListCall<Device> call = Calls.devicesList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every device. See {@link #listAll(DeviceListParams)}.
     *
     * @return a future carrying the devices
     */
    public CompletableFuture<List<Device>> listAll() {
        return listAll(null);
    }

    /**
     * Every device that matches, from every page.
     *
     * <p>The pages are fetched one after another and the future completes with all of them.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return a future carrying the devices
     */
    public CompletableFuture<List<Device>> listAll(DeviceListParams params) {
        return client.walk(Calls.devicesList(params));
    }

    /**
     * Returns the device with this fingerprint.
     *
     * @param fingerprint the device's fingerprint
     * @return a future carrying the device
     */
    public CompletableFuture<Device> get(String fingerprint) {
        return client.run(Calls.devicesGet(fingerprint));
    }

    /**
     * Sets whether the device is disregarded, and returns it.
     *
     * @param fingerprint the device's fingerprint
     * @param disregarded true to disregard the device, false to count it again
     * @return a future carrying the device, as it is now
     */
    public CompletableFuture<Device> update(String fingerprint, boolean disregarded) {
        return client.run(Calls.devicesUpdate(fingerprint, disregarded));
    }
}
