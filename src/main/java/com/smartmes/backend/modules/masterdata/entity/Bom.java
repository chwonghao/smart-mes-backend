package com.smartmes.backend.modules.masterdata.entity;

import com.smartmes.backend.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "bom")
@Getter
@Setter
public class Bom extends BaseEntity {

    // Parent item (The finished product to be manufactured)
    // FetchType.LAZY: Only load data from DB when explicitly called, optimizes performance.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", nullable = false)
    private ItemMaster parentItem;

    // Child item (The raw material or sub-assembly needed)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_item_id", nullable = false)
    private ItemMaster childItem;

    // Quantity of child item required to make 1 unit of parent item
    // Using BigDecimal to prevent floating-point calculation errors
    @Column(name = "quantity", nullable = false, precision = 10, scale = 4)
    private BigDecimal quantity;

    // Allowed scrap/waste percentage (e.g., 0.05 means 5% waste allowed)
    @Column(name = "scrap_factor", precision = 5, scale = 4)
    private BigDecimal scrapFactor = BigDecimal.ZERO;

    // Unit of measurement (Copied from ItemMaster for faster query without JOIN)
    @Column(name = "unit", length = 20)
    private String unit;
}