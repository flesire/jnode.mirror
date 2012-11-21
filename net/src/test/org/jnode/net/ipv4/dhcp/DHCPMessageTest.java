package org.jnode.net.ipv4.dhcp;

import static org.junit.Assert.assertNotNull;

import java.net.DatagramPacket;

import org.jnode.net.ipv4.bootp.BOOTPHeader;
import org.junit.Test;
import org.mockito.Mockito;

public class DHCPMessageTest {

    @Test
    public void testToDatagramPacket() {
        BOOTPHeader header = Mockito.mock(BOOTPHeader.class);
        DHCPMessage message = new DHCPMessage(header, DHCPMessage.DHCPDISCOVER);
        DatagramPacket packet = message.toDatagramPacket();
        assertNotNull(packet);
    }

}
