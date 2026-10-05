package com.example.aicustomerservice.dao;

import com.example.aicustomerservice.entity.CustomerProfile;
import java.util.Optional;

public interface CustomerDao {

    Optional<CustomerProfile> findByCustomerId(String customerId);
}
