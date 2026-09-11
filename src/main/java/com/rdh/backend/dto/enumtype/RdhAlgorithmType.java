package com.rdh.backend.dto.enumtype;

import lombok.Getter;

@Getter
public enum RdhAlgorithmType {
    HISTOGRAM_SHIFTING("Histogram Shifting", "Giấu tin thuận nghịch bằng dịch chuyển biểu đồ tần số"),
    LSB("Least Significant Bit", "Giấu tin đơn giản bằng thay thế bit LSB (Không khôi phục ảnh gốc)");

    private final String name;
    private final String description;

    RdhAlgorithmType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}