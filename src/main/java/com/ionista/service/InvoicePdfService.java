package com.ionista.service;

import com.ionista.entity.Order;

public interface InvoicePdfService {

    byte[] generateInvoice(Order order);
}
