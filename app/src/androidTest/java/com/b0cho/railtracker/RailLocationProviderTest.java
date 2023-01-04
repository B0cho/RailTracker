package com.b0cho.railtracker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * DO NOT FORGET TO SET RailTracker AS MOCK LOCATION APP IN YOUR DEVICE DEVELOPER SETTINGS!
 * Otherwise tests cannot be run and will fail!
 */
@RunWith(MockitoJUnitRunner.class)
public class RailLocationProviderTest extends TestCase {
    static Context appContext;
    static long elapsedRealtimeNanos;
    static RailLocationProvider.ISystemClock systemClock;
    FusedLocationProviderClient fusedLocationProviderClient;

    @Mock
    Context context;
    @Mock
    LocationCallback locationCallback;
    @Mock
    IMyLocationConsumer locationConsumer;

    @BeforeClass
    public static void onBeforeClass() {
        Log.d("TEST_CLASS", "DO NOT FORGET TO SET RailTracker AS MOCK LOCATION APP IN YOUR DEVICE DEVELOPER SETTINGS!");
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        systemClock = SystemClock::elapsedRealtimeNanos;
        elapsedRealtimeNanos = systemClock.getElapsedTimeNanos();
    }

    @Before
    public void onBefore (){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext);
        CountDownLatch latch = new CountDownLatch(1);

        fusedLocationProviderClient.setMockMode(true).addOnCompleteListener(onTaskCompleted(latch, "Fused location provider set mock failed"));
        awaitLatch(latch, "Fused location provider set mockMode failed");
    }

    @Test
    public void testCheckPermissions() {
        RailLocationProvider railLocationProvider = new RailLocationProvider(context, fusedLocationProviderClient, systemClock);
        // testing context without needed permissions
        when(context.checkSelfPermission(anyString())).thenReturn(PackageManager.PERMISSION_DENIED);
        assertFalse(railLocationProvider.checkPermissions(context));
        // testing context with permissions
        when(context.checkSelfPermission(argThat(argument -> Arrays.asList(RailLocationProvider.PERMISSIONS).contains(argument)))).thenReturn(PackageManager.PERMISSION_GRANTED);
        assertTrue(railLocationProvider.checkPermissions(context));
    }

    @Test(expected = IllegalStateException.class)
    public void testRequestSingleLocationUpdate_illegalState() {
        when(context.checkSelfPermission(anyString())).thenReturn(PackageManager.PERMISSION_DENIED);

        RailLocationProvider railLocationProvider = new RailLocationProvider(context, fusedLocationProviderClient, systemClock);
        railLocationProvider.requestSingleLocationUpdate(locationCallback);
    }

    @Test
    public void testRequestSingleLocationUpdate_noLastLocation() {
        // mocking last location
        Location location = new Location("MOCK1");
        location.setElapsedRealtimeNanos(systemClock.getElapsedTimeNanos());
        location.setAccuracy(1);
        RailLocationProvider railLocationProvider = new RailLocationProvider(context, fusedLocationProviderClient, systemClock);

        final Task<Void> result = testRequestSingleLocationUpdate(location, railLocationProvider);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        verify(locationCallback).onLocationResult(any(LocationResult.class));
        verify(locationConsumer).onLocationChanged(argThat(argument -> RailLocationProviderTest.LocationEquals(argument, location)), eq(railLocationProvider));
        assertTrue(RailLocationProviderTest.LocationEquals(railLocationProvider.getLastKnownLocation(), location));
    }

    @Test
    public void testRequestSingleLocationUpdate_validLastLocation() {
        Location lastLocation = new Location("MOCK1");
        lastLocation.setAccuracy(1);
        lastLocation.setElapsedRealtimeNanos(systemClock.getElapsedTimeNanos() - 22 * 1_000_000_000L);
        Location newLocation = new Location("MOCK2");
        newLocation.setAccuracy(1);
        newLocation.setElapsedRealtimeNanos(systemClock.getElapsedTimeNanos());
        RailLocationProvider railLocationProvider = new RailLocationProvider(context, fusedLocationProviderClient, systemClock, lastLocation);

        final Task<Void> result = testRequestSingleLocationUpdate(newLocation, railLocationProvider);

        assertNull(result);
        verify(locationCallback).onLocationResult(any(LocationResult.class));
        verify(locationConsumer).onLocationChanged(argThat(argument -> RailLocationProviderTest.LocationEquals(argument, lastLocation)), eq(railLocationProvider));
        assertTrue(RailLocationProviderTest.LocationEquals(railLocationProvider.getLastKnownLocation(), lastLocation));
        assertFalse(RailLocationProviderTest.LocationEquals(railLocationProvider.getLastKnownLocation(), newLocation));
    }

    @Test
    public void testRequestSingleLocationUpdate_expiredLastLocation() {
        Location lastLocation = new Location("MOCK1");
        lastLocation.setAccuracy(1);
        lastLocation.setElapsedRealtimeNanos(systemClock.getElapsedTimeNanos() - 42 * 1_000_000_000L);
        Location newLocation = new Location("MOCK2");
        newLocation.setAccuracy(1);
        newLocation.setElapsedRealtimeNanos(systemClock.getElapsedTimeNanos() - 4 * 1_000_000_000L);

        RailLocationProvider railLocationProvider = new RailLocationProvider(context, fusedLocationProviderClient, systemClock, lastLocation);

        final Task<Void> result = testRequestSingleLocationUpdate(newLocation, railLocationProvider);

        assertNotNull(result);
        verify(locationCallback).onLocationResult(any(LocationResult.class));
        verify(locationConsumer).onLocationChanged(argThat(argument -> RailLocationProviderTest.LocationEquals(argument, newLocation)), eq(railLocationProvider));
        assertTrue(RailLocationProviderTest.LocationEquals(railLocationProvider.getLastKnownLocation(), newLocation));
        assertFalse(RailLocationProviderTest.LocationEquals(railLocationProvider.getLastKnownLocation(), lastLocation));
    }

    /*
    Location.equals() is purely implemented from Object.equals().
    Only reference to the same object is checked, no further comparison of Location objects is performed
    - method is invalid for comparing even copied Locations.
    Therefore, a test-purpose LocationEquals method was created.
     */
    private static boolean LocationEquals(@NonNull Location object1, @NonNull Location object2) {
        return object1.equals(object2) ||
                (object1.getElapsedRealtimeNanos() == object2.getElapsedRealtimeNanos() &&
                        object1.getLatitude() == object2.getLatitude() &&
                        object1.getLongitude() == object2.getLongitude() &&
                        object1.getAltitude() == object2.getAltitude());
    }

    private void awaitLatch(@NonNull CountDownLatch latch, String onTimeoutMessage) {
        try {
            boolean result = latch.await(5, TimeUnit.SECONDS);
            if(!result)
                fail(onTimeoutMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail(onTimeoutMessage);
        }
    }

    private OnCompleteListener<Void> onTaskCompleted(CountDownLatch latchToCountDown, String onFailMessage) {
        return task -> {
            if (task.isSuccessful())
                latchToCountDown.countDown();
            else
                fail(onFailMessage);
        };
    }

    private Task<Void> testRequestSingleLocationUpdate(Location newLocation, RailLocationProvider railLocationProvider) {
        CountDownLatch mockLatch = new CountDownLatch(1);
        CountDownLatch testLatch = new CountDownLatch(1);
        fusedLocationProviderClient.setMockLocation(newLocation).addOnCompleteListener(onTaskCompleted(mockLatch, "Setting last mocked location failed"));
        awaitLatch(mockLatch, "Setting last mocked location failed");

        // mocking callback
        doAnswer(invocation -> {testLatch.countDown(); return null;}).when(locationCallback).onLocationResult(any());


        // requesting location - test
        railLocationProvider.startLocationProvider(locationConsumer);
        Task<Void> task = railLocationProvider.requestSingleLocationUpdate(locationCallback);
        awaitLatch(testLatch, "Callback failed to be invoked by requestSingleLocationUpdate");
        return task;
    }
}