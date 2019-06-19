package com.voysis.voysis;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.gson.Gson;
import com.voysis.sdk.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static com.voysis.UtilsKt.getClientVersionInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class NetworkUtilsTest {

    @Mock
    Context context;
    @Mock
    PackageManager packageManager;
    @Mock
    PackageInfo packageInfo;

    @Test
    public void testClientVersionInfoIsConstructedCorrectly() throws Exception {
        doReturn(packageManager).when(context).getPackageManager();
        doReturn("com.test").when(context).getPackageName();
        doReturn(packageInfo).when(packageManager).getPackageInfo("com.test", 0);
        packageInfo.versionName = "1.0";
        String clientInfo = getClientVersionInfo(context);
        @SuppressWarnings("rawtypes")
        Map parsed = new Gson().fromJson(clientInfo, Map.class);
        assertTrue(parsed.containsKey("os"));
        assertTrue(parsed.containsKey("sdk"));
        assertTrue(parsed.containsKey("app"));
        assertTrue(parsed.containsKey("device"));
        // Some of the version info is read from static properties which
        // are null when the test suite runs so we can only assert that
        // the key is present.
        assertEquals("Android", getMap(parsed, "os").get("id"));
        assertTrue(getMap(parsed, "os").containsKey("version"));
        assertEquals("voysis-android", getMap(parsed, "sdk").get("id"));
        assertEquals(BuildConfig.VERSION_NAME, getMap(parsed, "sdk").get("version"));
        assertEquals("com.test", getMap(parsed, "app").get("id"));
        assertEquals("1.0", getMap(parsed, "app").get("version"));
        assertTrue(getMap(parsed, "device").containsKey("manufacturer"));
        assertTrue(getMap(parsed, "device").containsKey("model"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getMap(Map<?, ?> map, String key) {
        return (Map<String, String>) map.get(key);
    }
}