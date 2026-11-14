package org.example.eshop.commerce.port;

import java.util.List;
import java.util.Optional;

/** Read-only bridge to the owning customer's TV1 address book. */
public interface CheckoutAddressPort {
    record Address(Long id,String label,String recipientName,String phone,String email,String addressLine1,String addressLine2,String wardName,String districtName,String provinceName,String postalCode,String countryCode,String companyName,String taxCode) {}
    List<Address> findByUserId(long userId);
    Optional<Address> findOwned(long userId,long addressId);
}
