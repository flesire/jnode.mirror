package org.jnode.net.arp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.jnode.net.InvalidLayerException;
import org.jnode.net.LayerAlreadyRegisteredException;
import org.jnode.net.NoSuchProtocolException;
import org.jnode.net.TransportLayer;
import org.junit.Before;
import org.junit.Test;

public class ARPNetworkLayerTest {
    private ARPNetworkLayer networkLayer;

    @Before
    public void setUp() {
        networkLayer = new ARPNetworkLayer();
    }

    @Test
    public void testGetName() {
        assertEquals("arp", networkLayer.getName());
    }

    @Test
    public void testGetProtocolID() {
        assertEquals(0x806, networkLayer.getProtocolID());
    }

    @Test
    public void testIsAllowedForDevice() {
        assertTrue(networkLayer.isAllowedForDevice(null));
    }

    @Test(expected = InvalidLayerException.class)
    public void testRegisterTransportLayer()
        throws LayerAlreadyRegisteredException, InvalidLayerException {
        networkLayer.registerTransportLayer(null);
    }

    @Test
    public void testGetTransportLayers() {
        Collection<TransportLayer> layers = networkLayer.getTransportLayers();
        assertNotNull(layers);
        assertEquals(0, layers.size());
    }

    @Test(expected = NoSuchProtocolException.class)
    public void testGetTransportLayer() throws NoSuchProtocolException {
        networkLayer.getTransportLayer(0);
    }

}
