package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.entity.ProductionIssueImplementation;
import com.example.aicustomerservice.service.EnterpriseProblemCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support/production")
public class ProductionController {

    private final EnterpriseProblemCatalog catalog;

    public ProductionController(EnterpriseProblemCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/issues")
    public List<ProductionIssueImplementation> issues() {
        return catalog.list();
    }
}
