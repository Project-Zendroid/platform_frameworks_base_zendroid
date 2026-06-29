/*
 * Copyright (C) 2026 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.location;

import android.Manifest;
import android.content.Context;

import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.location.provider.StaticLocationProvider;

import android.location.ILocationControlService;

/**
 * Service for controlling static location provider.
 * Allows system/privileged apps to update the location.
 *
 * @hide
 */
public final class LocationControlService extends SystemService {

    private static final String SERVICE_NAME = "location_control";

    private final LocationHalWrapper mHalWrapper = new LocationHalWrapper();
    private StaticLocationProvider mStaticLocationProvider;

    public LocationControlService(Context context) {
        super(context);
    }

    @Override
    public void onStart() {
        publishBinderService(SERVICE_NAME, new LocationControlBinder());
        LocalServices.addService(LocationControlService.class, this);
    }

    /**
     * Called by LocationManagerService to set the static location provider reference.
     */
    public void setStaticLocationProvider(StaticLocationProvider provider) {
        mStaticLocationProvider = provider;
    }

    private StaticLocationProvider getStaticLocationProvider() {
        if (mStaticLocationProvider == null) {
            mStaticLocationProvider = LocalServices.getService(StaticLocationProvider.class);
        }
        return mStaticLocationProvider;
    }

    /**
     * Binder service implementation for ILocationControlService.
     */
    private final class LocationControlBinder extends ILocationControlService.Stub {

        @Override
        public void setLocation(double lat, double lon) throws android.os.RemoteException {
            getContext().enforceCallingPermission(
                    Manifest.permission.LOCATION_HARDWARE,
                    "Permission denied for setLocation");

            validateCoordinates(lat, lon);

            mHalWrapper.setLocation(lat, lon);

            StaticLocationProvider provider = getStaticLocationProvider();
            if (provider != null) {
                provider.refreshLocation();
            }
        }

        private void validateCoordinates(double lat, double lon)
                throws android.os.RemoteException {
            if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                throw new android.os.RemoteException(
                        "Latitude and longitude must be finite values");
            }
            if (lat < -90.0 || lat > 90.0) {
                throw new android.os.RemoteException(
                        "Latitude out of range [-90.0, 90.0]: " + lat);
            }
            if (lon < -180.0 || lon > 180.0) {
                throw new android.os.RemoteException(
                        "Longitude out of range [-180.0, 180.0]: " + lon);
            }
        }
    }
}
