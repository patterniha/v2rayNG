package com.v2ray.ang.core

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.V2rayConfig.OutboundBean
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for CoreOutboundBuilder.applyDialMode: dialMode must land in
 * streamSettings.sockopt without discarding the other sockopt options.
 */
class CoreOutboundBuilderTest {

    private fun createProfile(mode: String?): ProfileItem =
        ProfileItem.create(EConfigType.VLESS).apply { dialMode = mode }

    @Test
    fun test_applyDialMode_setsSockoptDialMode() {
        val outbound = OutboundBean(protocol = "vless", streamSettings = OutboundBean.StreamSettingsBean())

        CoreOutboundBuilder.applyDialMode(outbound, createProfile("code-1"))

        assertEquals("code-1", outbound.streamSettings?.sockopt?.dialMode)
        assertEquals(AppConfig.DEFAULT_NETWORK, outbound.streamSettings?.network)
    }

    @Test
    fun test_applyDialMode_keepsExistingSockoptOptions() {
        val outbound = OutboundBean(
            protocol = "vless",
            streamSettings = OutboundBean.StreamSettingsBean(
                sockopt = OutboundBean.StreamSettingsBean.SockoptBean(
                    dialerProxy = "hop-1",
                    domainStrategy = "UseIP"
                )
            )
        )

        CoreOutboundBuilder.applyDialMode(outbound, createProfile("code-1"))

        val sockopt = outbound.streamSettings?.sockopt
        assertEquals("code-1", sockopt?.dialMode)
        assertEquals("hop-1", sockopt?.dialerProxy)
        assertEquals("UseIP", sockopt?.domainStrategy)
    }

    @Test
    fun test_applyDialMode_ignoresBlankDialMode() {
        val outbound = OutboundBean(protocol = "vless", streamSettings = OutboundBean.StreamSettingsBean())

        CoreOutboundBuilder.applyDialMode(outbound, createProfile(" "))
        CoreOutboundBuilder.applyDialMode(outbound, createProfile(null))

        assertNull(outbound.streamSettings?.sockopt)
    }

    @Test
    fun test_applyDialMode_wireguardWithoutStreamSettings_getsSockoptWithoutNetwork() {
        // wireguard outbounds are created without streamSettings; Xray still dials their
        // endpoint through the system dialer, so a sockopt-only streamSettings is added
        val outbound = OutboundBean(protocol = "wireguard")

        CoreOutboundBuilder.applyDialMode(outbound, createProfile("code-1"))

        assertNotNull(outbound.streamSettings)
        assertNull(outbound.streamSettings?.network)
        assertEquals("code-1", outbound.streamSettings?.sockopt?.dialMode)
    }
}
