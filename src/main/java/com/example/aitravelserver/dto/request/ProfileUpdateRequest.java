package com.example.aitravelserver.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ProfileUpdateRequest {

    private String nickname;
    private String avatar;
}