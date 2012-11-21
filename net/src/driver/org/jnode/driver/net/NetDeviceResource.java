package org.jnode.driver.net;

import java.security.PrivilegedExceptionAction;

import javax.naming.NameNotFoundException;

import org.jnode.driver.DriverException;
import org.jnode.driver.bus.pci.PCIBaseAddress;
import org.jnode.driver.bus.pci.PCIDevice;
import org.jnode.driver.bus.pci.PCIHeaderType0;
import org.jnode.naming.InitialNaming;
import org.jnode.system.resource.IOResource;
import org.jnode.system.resource.IRQHandler;
import org.jnode.system.resource.IRQResource;
import org.jnode.system.resource.ResourceManager;
import org.jnode.system.resource.ResourceNotFreeException;
import org.jnode.system.resource.ResourceOwner;
import org.jnode.util.AccessControllerUtils;
import org.jnode.util.NumberUtils;

public class NetDeviceResource {

    private final ResourceManager rm;

    /**
     * Start of IO address space
     */
    private int iobase;

    /**
     * IO address space resource
     */
    private IOResource io;

    /**
     * IRQ
     */
    private IRQResource irq;

    public NetDeviceResource(ResourceOwner owner, IRQHandler irqHandler, PCIDevice device)
            throws ResourceNotFreeException, DriverException {
        try {
            rm = InitialNaming.lookup(ResourceManager.NAME);
        } catch (NameNotFoundException ex) {
            throw new DriverException("Cannot find ResourceManager");
        }
        this.iobase = getIOBase(device, 0);
        final int iolength = getIOLength(device);
        final int irq = getIRQ(device);
        this.irq = rm.claimIRQ(owner, irq, irqHandler, true);
        try {
            io = claimPorts(rm, owner, iobase, iolength);
        } catch (ResourceNotFreeException ex) {
            this.irq.release();
            throw ex;
        }
    }

    /**
     * Release all resources
     */
    public void release() {
        io.release();
        irq.release();
    }

    @Override
    public String toString() {
        return "IO base: 0x" + NumberUtils.hex(iobase) + " IRQ: " + irq.getIRQ();
    }

    public ResourceManager getResourceManager() {
        return rm;
    }

    public int getIobase() {
        return iobase;
    }

    public IOResource getIoResource() {
        return io;
    }

    public IRQResource getIrqResource() {
        return irq;
    }

    /**
     * Reads a 8-bit NIC register
     * 
     * @param reg
     */
    public final int getReg8(int reg) {
        return io.inPortByte(iobase + reg);
    }

    /**
     * Reads a 16-bit NIC register
     * 
     * @param reg
     */
    public final int getReg16(int reg) {
        return io.inPortWord(iobase + reg);
    }

    /**
     * Reads a 32-bit NIC register
     * 
     * @param reg
     */

    public final int getReg32(int reg) {
        return io.inPortDword(iobase + reg);
    }

    /**
     * Writes a 8-bit NIC register
     * 
     * @param reg
     * @param value
     */

    public final void setReg8(int reg, int value) {
        io.outPortByte(iobase + reg, value);
    }

    /**
     * Writes a 16-bit NIC register
     * 
     * @param reg
     * @param value
     */

    public final void setReg16(int reg, int value) {
        io.outPortWord(iobase + reg, value);
    }

    /**
     * Writes a 32-bit NIC register
     * 
     * @param reg
     * @param value
     */

    public final void setReg32(int reg, int value) {
        io.outPortDword(iobase + reg, value);
    }

    // Private methods

    /**
     * Gets the IRQ used by the given device
     * 
     * @param device
     * @param flags
     */
    private int getIRQ(PCIDevice device) {
        final PCIHeaderType0 config = device.getConfig().asHeaderType0();
        return config.getInterruptLine();
    }

    /**
     * Gets the first IO-Address used by the given device
     * 
     * @param device
     * @param flags
     */
    private final int getIOBase(PCIDevice device, int firstAddressIndex) throws DriverException {
        final PCIHeaderType0 config = device.getConfig().asHeaderType0();
        final PCIBaseAddress[] addrs = config.getBaseAddresses();
        if (addrs.length < 1) {
            throw new DriverException("Cannot find iobase: not base addresses");
        }
        if (!addrs[firstAddressIndex].isIOSpace()) {
            throw new DriverException("Cannot find iobase: first address is not I/O");
        }
        return addrs[firstAddressIndex].getIOBase();
    }

    private int getIOLength(PCIDevice device) throws DriverException {
        final PCIHeaderType0 config = device.getConfig().asHeaderType0();
        final PCIBaseAddress[] addrs = config.getBaseAddresses();
        if (addrs.length < 1) {
            throw new DriverException("Cannot find iobase: not base addresses");
        }
        if (!addrs[0].isIOSpace()) {
            throw new DriverException("Cannot find iobase: first address is not I/O");
        }
        return addrs[0].getSize();
    }

    private IOResource claimPorts(final ResourceManager rm, final ResourceOwner owner,
            final int low, final int length) throws ResourceNotFreeException, DriverException {
        try {
            return AccessControllerUtils.doPrivileged(new PrivilegedExceptionAction<IOResource>() {
                public IOResource run() throws ResourceNotFreeException {
                    return rm.claimIOResource(owner, low, length);
                }
            });
        } catch (ResourceNotFreeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DriverException("Unknown exception", ex);
        }

    }

}
