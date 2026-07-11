package com.ionista.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailSendResult {
    private final int successCount;
    private final int failureCount;
}
