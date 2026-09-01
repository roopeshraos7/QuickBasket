package com.quickbasket.dto;

/**
 * Categorization of fulfillment and shipping speeds:
 * INSTANT  : Sub-hour hyper-local delivery (e.g. 10-30 minutes).
 * EXPRESS  : Same-day or next-day rapid delivery.
 * STANDARD : Multi-day standard courier delivery.
 */
public enum DeliveryType {
    INSTANT,
    EXPRESS,
    STANDARD
}
