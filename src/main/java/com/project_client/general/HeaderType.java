package com.project_client.general;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HeaderType {
    REQUEST((byte)0x01),
    RESPONSE((byte)0x02);

    final private byte value;
}
