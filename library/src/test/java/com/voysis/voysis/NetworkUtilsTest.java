package com.voysis.voysis;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.voysis.sdk.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.voysis.UtilsKt.getUserAgent;
import static org.junit.Assert.assertEquals;
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
    public void getUserAgentSucess() throws Exception {
        doReturn(packageManager).when(context).getPackageManager();
        doReturn("com.test").when(context).getPackageName();
        doReturn(packageInfo).when(packageManager).getPackageInfo("com.test", 0);
        packageInfo.versionName = "1.0";
        String userAgent = getUserAgent(context);
        String expectedUserAgent = getExpectedUserAgent();
        assertEquals(userAgent, expectedUserAgent);
    }

    private String getExpectedUserAgent() {
        StringBuilder stringBuilder = new StringBuilder();
        return stringBuilder.append(System.getProperty("http.agent")).append(" ")
                .append(context.getPackageName()).append("/").append(packageInfo.versionName).append(" ")
                .append(BuildConfig.APPLICATION_ID).append("/").append(BuildConfig.VERSION_NAME).toString();
    }
}