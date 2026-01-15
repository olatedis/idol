package com.bit.idol.userservice.dto.idol;

import com.bit.idol.userservice.entity.IdolStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class IdolStatusChangeRequest {

    private IdolStatus status;
}

