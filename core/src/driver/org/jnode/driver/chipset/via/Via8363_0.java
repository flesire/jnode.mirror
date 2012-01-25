/*
 * $Id$
 *
 * Copyright (C) 2003-2010 JNode.org
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
 
package org.jnode.driver.chipset.via;

import org.apache.log4j.Logger;
import org.jnode.driver.Driver;
import org.jnode.driver.DriverException;
import org.jnode.driver.bus.pci.PCIDevice;

/**
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public class Via8363_0 extends Driver {

    /**
     * My logger
     */
    private static final Logger log = Logger.getLogger(Via8363_0.class);

    /**
     * Start the device
     *
     * @throws DriverException
     */
    protected void startDevice() throws DriverException {
        ViaQuirks.applyLatencyFix((PCIDevice) getDevice(), log);
    }

    /**
     * Stop the device
     *
     * @throws DriverException
     */
    protected void stopDevice() throws DriverException {
        // Nothing to do here
    }
}
