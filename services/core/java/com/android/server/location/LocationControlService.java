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
import android.util.Log;

import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.location.provider.StaticLocationProvider;

import android.location.ILocationControlService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Service for controlling static location provider.
 * Allows system/privileged apps to update the location.
 *
 * @hide
 */
public final class LocationControlService extends SystemService {

    private static final String TAG = "LocationControlService";
    private static final String SERVICE_NAME = "location_control";
    private static final String CONFIG_PATH = "/data/vendor/location/location.conf";

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

            writeConfig(lat, lon);

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

        private void writeConfig(double lat, double lon) {
            FileOutputStream fos = null;
            OutputStreamWriter writer = null;

            try {
                File configFile = new File(CONFIG_PATH);
                File parentDir = configFile.getParentFile();
                if (parentDir == null) {
                    throw new IOException("Config path has no parent directory: " + CONFIG_PATH);
                }
                if (!parentDir.exists()) {
                    throw new IOException("Missing config parent directory: "
                            + parentDir + ". It must be created and SELinux-labeled by init or "
                            + "the HAL before LocationControlService writes the config");
                }
                if (!parentDir.isDirectory()) {
                    throw new IOException("Config parent path is not a directory: " + parentDir);
                }

                fos = new FileOutputStream(configFile, false);
                writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

                writer.write("lat=" + lat + "\n");
                writer.write("lon=" + lon + "\n");

                writer.flush();
                fos.flush();
                fos.getFD().sync();

                Log.d(TAG, "Wrote location config");
            } catch (IOException e) {
                Log.e(TAG, "Failed to write location config", e);
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    } else if (fos != null) {
                        fos.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }
}
