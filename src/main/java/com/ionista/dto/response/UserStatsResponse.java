package com.ionista.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsResponse {

    private long activeUsers;
    private long newSignups;
}
