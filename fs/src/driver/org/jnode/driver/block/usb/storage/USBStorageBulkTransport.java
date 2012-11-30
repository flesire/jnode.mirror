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

package org.jnode.driver.block.usb.storage;

import org.apache.log4j.Logger;
import org.jnode.driver.bus.scsi.CDB;
import org.jnode.driver.bus.usb.SetupPacket;
import org.jnode.driver.bus.usb.USBDataPipe;
import org.jnode.driver.bus.usb.USBDevice;
import org.jnode.driver.bus.usb.USBException;
import org.jnode.driver.bus.usb.USBRequest;
import org.jnode.util.NumberUtils;

final class USBStorageBulkTransport implements UsbStorageTransport, USBStorageConstants {

    private static final int US_BULK_GET_MAX_LUN = 0xFE;

    private static final Logger log = Logger.getLogger(USBStorageBulkTransport.class);

    private static final int US_BULK_RESET_REQUEST = 0xFF;
    /** */
    private final USBStorageDeviceData storageDeviceData;

    /**
     * @param storageDeviceData
     */
    public USBStorageBulkTransport(USBStorageDeviceData storageDeviceData) {
        this.storageDeviceData = storageDeviceData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jnode.driver.block.usb.storage.UsbStorageTransport#transport(org.
     * jnode.driver.bus.scsi.CDB, long)
     */
    public void transport(CDB cdb, long timeout) {
        try {
            byte[] scsiCmd = cdb.toByteArray();
            // Setup command wrapper
            CBW cbw = getCBW(cdb, scsiCmd);
            // Sent CBW to device
            USBDataPipe outPipe = ((USBDataPipe) storageDeviceData.getBulkOutEndPoint().getPipe());
            USBRequest req = outPipe.createRequest(cbw);
            outPipe.syncSubmit(req, timeout);
            //
            CSW csw = getCSW();
            USBDataPipe inPipe = ((USBDataPipe) storageDeviceData.getBulkInEndPoint().getPipe());
            USBRequest resp = inPipe.createRequest(csw);
            inPipe.syncSubmit(resp, timeout);
            log.debug(csw.toString());
            switch (csw.getStatus()) {
                case 0:
                    // OK
                    break;
                case 1:
                    // FAIL
                    break;
                case 2:
                    // PHASE
                    break;
            }
        } catch (USBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Bulk-Only mass storage reset.
     */
    public void reset() throws USBException {
        SetupPacket setupPacket =
                new SetupPacket(US_BULK_RESET_REQUEST, USB_TYPE_CLASS | USB_RECIP_INTERFACE, 0, 0,
                        0);
        final USBRequest req = storageDeviceData.getSendControlPipe().createRequest(setupPacket);
        storageDeviceData.getSendControlPipe().syncSubmit(req, GET_TIMEOUT);
    }

    private CSW getCSW() {
        CSW csw = new CSW();
        csw.setSignature(US_BULK_CS_SIGN);
        return csw;
    }

    private CBW getCBW(CDB cdb, byte[] scsiCmd) {
        CBW cbw = new CBW();
        cbw.setSignature(US_BULK_CB_SIGN);
        cbw.setTag(1);
        cbw.setDataTransferLength((byte) cdb.getDataTransfertCount());
        cbw.setFlags((byte) 0);
        cbw.setLun((byte) 0);
        cbw.setLength((byte) scsiCmd.length);
        cbw.setCdb(scsiCmd);
        log.debug(cbw.toString());
        return cbw;
    }

    /**
     * Get max logical unit allowed by device. Device not support multiple LUN
     * <i>may</i> stall.
     * 
     * @param usbDev
     * @throws USBException
     */
    public void getMaxLun(USBDevice usbDev) throws USBException {
        log.info("*** Get max lun ***");
        SetupPacket setupPacket =
                new SetupPacket(USB_DIR_IN | USB_TYPE_CLASS | USB_RECIP_INTERFACE,
                        US_BULK_GET_MAX_LUN, 0, 0, 1);
        USBDataPipe pipe = storageDeviceData.getReceiveControlPipe();
        final USBRequest req = pipe.createRequest(setupPacket);
        pipe.syncSubmit(req, GET_TIMEOUT);
        log.debug("*** Request data     : " + req.toString());
        log.debug("*** Request status   : 0x" + NumberUtils.hex(req.getStatus(), 4));
        if (req.getStatus() == USBREQ_ST_COMPLETED) {
            storageDeviceData.setMaxLun(setupPacket.getData()[0]);
        } else if (req.getStatus() == USBREQ_ST_STALLED) {
            storageDeviceData.setMaxLun((byte) 0);
        } else {
            throw new USBException("Request status   : 0x" + NumberUtils.hex(req.getStatus(), 4));
        }
    }
}
