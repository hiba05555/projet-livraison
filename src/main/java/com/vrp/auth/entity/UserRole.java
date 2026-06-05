package com.vrp.auth.entity;

/**
 * VRP platform user roles aligned with Keycloak realm roles.
 *
 * <ul>
 *   <li>ADMIN      – full platform access (enterprise admin)</li>
 *   <li>DISPATCHER – manages routes, vehicles, and fleet operations</li>
 *   <li>DRIVER     – delivery driver; views itinerary, validates deliveries</li>
 *   <li>USER       – grocery store client (épicier); places orders, tracks parcels</li>
 * </ul>
 */
public enum UserRole {
    ADMIN,
    DISPATCHER,
    DRIVER,
    USER
}
