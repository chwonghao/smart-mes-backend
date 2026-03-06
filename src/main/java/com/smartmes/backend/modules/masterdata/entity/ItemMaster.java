package com.smartmes.backend.modules.masterdata.entity;

import com.smartmes.backend.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "item_master")
@Getter
@Setter
public class ItemMaster extends BaseEntity {

    @Column(name = "item_code", nullable = false, unique = true, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    // Loại vật tư: Nguyên liệu thô, Bán thành phẩm, Thành phẩm
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private ItemType itemType;

    @Column(name = "unit", nullable = false, length = 20) // VD: KG, PCS, METERS
    private String unit;

    // VŨ KHÍ BÍ MẬT: Cấu hình trường dữ liệu động bằng JSONB cho PostgreSQL
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes", columnDefinition = "jsonb")
    private Map<String, Object> customAttributes;

    // Định nghĩa các loại vật tư trong nội bộ class cho gọn
    public enum ItemType {
        RAW_MATERIAL,   // Nguyên vật liệu thô (VD: Gỗ, Thép)
        SEMI_FINISHED,  // Bán thành phẩm (VD: Mặt bàn chưa sơn)
        FINISHED_GOOD   // Thành phẩm (VD: Bàn hoàn chỉnh)
    }
}