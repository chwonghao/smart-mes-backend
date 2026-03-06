package com.smartmes.backend.modules.inventory.entity;

import com.smartmes.backend.core.common.BaseEntity;
import com.smartmes.backend.modules.masterdata.entity.ItemMaster;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory")
@Getter
@Setter
public class Inventory extends BaseEntity {

    // Reference to the Item (Raw material, Semi-finished, or Finished good)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemMaster item;

    // Current quantity available in stock
    @Column(name = "on_hand_quantity", nullable = false, precision = 18, scale = 2)
    private BigDecimal onHandQuantity = BigDecimal.ZERO;

    // Minimum stock level for alerts (Safety Stock)
    @Column(name = "min_stock_level", precision = 18, scale = 2)
    private BigDecimal minStockLevel = BigDecimal.ZERO;

    // Location within the warehouse (e.g., A-01-05)
    @Column(name = "location_code", length = 50)
    private String locationCode;
}