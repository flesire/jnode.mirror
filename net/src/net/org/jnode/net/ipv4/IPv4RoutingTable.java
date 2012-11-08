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

package org.jnode.net.ipv4;

import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jnode.net.ipv4.dhcp.DHCPClient;

/**
 * @author epr
 */
public class IPv4RoutingTable {

	private static final Logger log = Logger.getLogger(IPv4RoutingTable.class);

	/** All entries as instanceof IPv4Route */
	private final Vector<IPv4Route> entries = new Vector<IPv4Route>();

	/**
	 * Create a new instance
	 */
	public IPv4RoutingTable() {
	}

	/**
	 * Gets the number of entries
	 */
	public int getSize() {
		return entries.size();
	}

	/**
	 * Get an entry at a given index
	 * 
	 * @param index
	 */
	public IPv4Route get(int index) {
		return (IPv4Route) entries.get(index);
	}

	/**
	 * Add an entry
	 * 
	 * @param entry
	 */
	public void add(IPv4Route entry) {
		entries.add(entry);
	}

	/**
	 * Remove a given entry
	 * 
	 * @param entry
	 */
	public void remove(IPv4Route entry) {
		entries.remove(entry);
	}

	/**
	 * Get all entries
	 * 
	 * @see IPv4Route
	 * @return a list of IPv4Route entries.
	 */
	public List<IPv4Route> entries() {
		return new ArrayList<IPv4Route>(entries);
	}

	/**
	 * Search for a route to the given destination
	 * 
	 * @param destination
	 * @throws NoRouteToHostException
	 *             No route has been found
	 * @return The route that has been selected.
	 */
	public IPv4Route search(IPv4Address destination)
			throws NoRouteToHostException {
		while (true) {
			try {
				log.debug("# of route registred : " + entries.size());
				Vector<IPv4Route> up = getRouteUp(entries);
				log.debug("# of route up : " + up.size());
				// First search for a matching host-address route
				if (up.size() > 0) {
					for (IPv4Route route : up) {
						if (route.isHost()) {
							if (route.getDestination().equals(destination)) {
								return route;
							}
						}
					}
					// No direct host found, search through the networks
					for (IPv4Route r : up) {
						if (r.isNetwork()) {
							if (r.getDestination().matches(destination,
									r.getSubnetmask())) {
								return r;
							}
						}
					}

					// No network found, search for the default gateway
					for (IPv4Route r : up) {
						if (r.isGateway()) {
							return r;
						}
					}
				}
				// No route found
				throw new NoRouteToHostException(destination.toString());
			} catch (ConcurrentModificationException ex) {
				// The list of entries was modified, while we are searching,
				// Just loop and try it again
			}
		}
	}

	private Vector<IPv4Route> getRouteUp(Vector<IPv4Route> routes) {
		Vector<IPv4Route> filtered = new Vector<IPv4Route>();
		for (IPv4Route route : entries) {
			if (route.isUp()) {
				filtered.add(route);
			}
		}
		return filtered;
	}

	/**
	 * Convert to a String representation
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		final StringBuilder b = new StringBuilder();
		for (IPv4Route r : entries) {
			b.append(r);
			b.append('\n');
		}
		return b.toString();
	}
}
