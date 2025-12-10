package com.project_client.menu;

import com.project_client.Utils;
import lombok.Builder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Builder
public class MenuRegisterRequestDTO {

    private final Long restId;
    private final String menuName;
    private final Integer standardPrice;
    private final Integer studentPrice;
    private final Integer defaultAmount;
    private final LocalDate startSalesAt;
    private final LocalDate endSalesAt;
    private final Long menuTypeId;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public byte[] toBytes() {
        byte[] menuNameBytes = menuName.getBytes(StandardCharsets.UTF_8);
        byte[] startSalesAtBytes = startSalesAt != null ? startSalesAt.format(DATE_FORMATTER).getBytes(StandardCharsets.UTF_8) : new byte[0];
        byte[] endSalesAtBytes = endSalesAt != null ? endSalesAt.format(DATE_FORMATTER).getBytes(StandardCharsets.UTF_8) : new byte[0];

        int totalSize = 8 + 2 + menuNameBytes.length + 4 + 4 + 4 + 2 + startSalesAtBytes.length + 2 + endSalesAtBytes.length + 8;
        byte[] data = new byte[totalSize];
        int offset = 0;

        offset = Utils.longToBytes(restId, data, offset);

        offset = Utils.shortToBytes((short) menuNameBytes.length, data, offset);
        System.arraycopy(menuNameBytes, 0, data, offset, menuNameBytes.length);
        offset += menuNameBytes.length;

        offset = Utils.intToBytes(standardPrice, data, offset);
        offset = Utils.intToBytes(studentPrice, data, offset);
        offset = Utils.intToBytes(defaultAmount, data, offset);

        offset = Utils.shortToBytes((short) startSalesAtBytes.length, data, offset);
        System.arraycopy(startSalesAtBytes, 0, data, offset, startSalesAtBytes.length);
        offset += startSalesAtBytes.length;

        offset = Utils.shortToBytes((short) endSalesAtBytes.length, data, offset);
        System.arraycopy(endSalesAtBytes, 0, data, offset, endSalesAtBytes.length);
        offset += endSalesAtBytes.length;

        Utils.longToBytes(menuTypeId, data, offset);

        return data;
    }
}
