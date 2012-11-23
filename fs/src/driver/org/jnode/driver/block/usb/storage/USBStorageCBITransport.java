package org.jnode.driver.block.usb.storage;

import org.apache.log4j.Logger;
import org.jnode.driver.bus.scsi.CDB;
import org.jnode.driver.bus.usb.SetupPacket;
import org.jnode.driver.bus.usb.USBException;
import org.jnode.driver.bus.usb.USBRequest;

public class USBStorageCBITransport implements UsbStorageTransport, USBStorageConstants {

    private static final Logger log = Logger.getLogger(USBStorageCBITransport.class);
    private static final int US_CBI_ADSC = 0;
    private static final int CB_RESET_COMMAND_SIZE = 12;
    /** */
    private final USBStorageDeviceData storageDeviceData;

    public USBStorageCBITransport(USBStorageDeviceData storageDeviceData) {
        this.storageDeviceData = storageDeviceData;
    }

    @Override
    public void transport(CDB cdb, long timeout) {
        SetupPacket packet =
                new SetupPacket(US_CBI_ADSC, USB_TYPE_CLASS | USB_RECIP_INTERFACE, 0, 0,
                        cdb.getDataTransfertCount());
        storageDeviceData.getSendControlPipe().createRequest(packet);

    }

    @Override
    public void reset() throws USBException {
        SetupPacket setupPacket =
                new SetupPacket(US_CBI_ADSC, USB_TYPE_CLASS | USB_RECIP_INTERFACE, 0, 0,
                        CB_RESET_COMMAND_SIZE);
        final USBRequest req = storageDeviceData.getSendControlPipe().createRequest(setupPacket);
        storageDeviceData.getSendControlPipe().syncSubmit(req, GET_TIMEOUT);

    }

}
