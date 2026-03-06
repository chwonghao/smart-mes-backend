package com.smartmes.backend.modules.masterdata.dto;

import lombok.Data;
import java.util.Map;

@Data // Tự động sinh Getter/Setter
public class ItemMasterDto {
    private String itemCode;
    private String itemName;
    private String itemType; // Sẽ nhận string "RAW_MATERIAL", "FINISHED_GOOD"...
    private String unit;
    private Map<String, Object> customAttributes; // Nhận JSON từ Frontend
}