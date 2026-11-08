package com.glasslauncher

import com.glasslauncher.icons.IOSIconProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IOSIconProviderTest {

    @Test
    fun testExcludedAppsDoNotMapToPhone() {
        // PhonePe, PharmEasy, ColorOS Phone Manager should never map to Phone icon
        assertNull("PhonePe should not map to an iOS icon", IOSIconProvider.getCustomIcon("com.phonepe.app"))
        assertNull("PharmEasy should not map to an iOS icon", IOSIconProvider.getCustomIcon("com.phone.pharmeasy"))
        assertNull("PharmEasy alt package should not map to an iOS icon", IOSIconProvider.getCustomIcon("com.pharmeasy"))
        assertNull("ColorOS Phone Manager should not map to an iOS icon", IOSIconProvider.getCustomIcon("com.coloros.phonemanager"))
        assertNull("Cleaners/optimizers should not map to an iOS icon", IOSIconProvider.getCustomIcon("com.miui.cleanmaster"))
        assertNull("Phone clone should not map to an iOS icon", IOSIconProvider.getCustomIcon("com.oneplus.phoneclone"))
    }

    @Test
    fun testRealPhoneAppsMapCorrectly() {
        assertEquals(R.drawable.ic_ios_phone, IOSIconProvider.getCustomIcon("com.google.android.dialer"))
        assertEquals(R.drawable.ic_ios_phone, IOSIconProvider.getCustomIcon("com.android.phone"))
        assertEquals(R.drawable.ic_ios_phone, IOSIconProvider.getCustomIcon("com.samsung.android.dialer"))
        assertEquals(R.drawable.ic_ios_phone, IOSIconProvider.getCustomIcon("com.simplemobiletools.dialer"))
    }

    @Test
    fun testCommonSystemApps() {
        assertEquals(R.drawable.ic_ios_browser, IOSIconProvider.getCustomIcon("com.android.chrome"))
        assertEquals(R.drawable.ic_playstore, IOSIconProvider.getCustomIcon("com.android.vending"))
        assertEquals(R.drawable.ic_ios_maps, IOSIconProvider.getCustomIcon("com.google.android.apps.maps"))
        assertEquals(R.drawable.ic_ios_photos, IOSIconProvider.getCustomIcon("com.google.android.apps.photos"))
        assertEquals(R.drawable.ic_ios_settings, IOSIconProvider.getCustomIcon("com.android.settings"))
    }
}
