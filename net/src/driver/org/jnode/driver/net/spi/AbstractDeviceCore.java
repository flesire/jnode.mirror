/*
 * $Id$
 *
 * Copyright (C) 2003-2012 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jnode.driver.net.spi;

import org.apache.log4j.Logger;
import org.jnode.driver.Device;
import org.jnode.driver.DriverException;
import org.jnode.driver.bus.pci.PCIDevice;
import org.jnode.driver.bus.pci.PCIHeaderType0;
import org.jnode.driver.net.NetDeviceResource;
import org.jnode.driver.net.rtl8139.RTL8139Flags;
import org.jnode.net.HardwareAddress;
import org.jnode.net.SocketBuffer;
import org.jnode.util.TimeoutException;

/**
 * This abstract class is not intended for any external purpose. It only serves
 * as a voluntary guide for driver implementation of network cards.
 * 
 * @author epr
 */
public abstract class AbstractDeviceCore {

    /**
     * My logger
     */
    protected final Logger log = Logger.getLogger(getClass());

    protected NetDeviceResource resources = null;

    /**
     * Reads a 8-bit NIC register
     * 
     * @param reg
     */
    protected final int getReg8(int reg) {
        return resources.getReg8(reg);
    }

    /**
     * Reads a 16-bit NIC register
     * 
     * @param reg
     */
    protected final int getReg16(int reg) {
        return resources.getReg16(reg);
    }

    /**
     * Reads a 32-bit NIC register
     * 
     * @param reg
     */

    protected final int getReg32(int reg) {
        return resources.getReg32(reg);
    }

    /**
     * Writes a 8-bit NIC register
     * 
     * @param reg
     * @param value
     */

    protected final void setReg8(int reg, int value) {
        resources.setReg8(reg, value);
    }

    /**
     * Writes a 16-bit NIC register
     * 
     * @param reg
     * @param value
     */

    protected final void setReg16(int reg, int value) {
        resources.setReg16(reg, value);
    }

    /**
     * Writes a 32-bit NIC register
     * 
     * @param reg
     * @param value
     */

    public final void setReg32(int reg, int value) {
        resources.setReg32(reg, value);
    }

    /**
     * Gets the hardware address of this device
     */
    public abstract HardwareAddress getHwAddress();

    /**
     * Initialize the device
     */
    public abstract void initialize() throws DriverException;

    /**
     * Disable the device
     */
    public abstract void disable();

    /**
     * Release all resources
     */
    public void release() {
        resources.release();
    }

    /**
     * Transmit the given buffer
     * 
     * @param buf
     * @param timeout
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public abstract void transmit(SocketBuffer buf, HardwareAddress destination, long timeout)
        throws InterruptedException, TimeoutException;
}
