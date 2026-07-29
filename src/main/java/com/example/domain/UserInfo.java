package com.example.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class UserInfo {
    private String username;
    @JsonIgnore
    private String password;
    private int age;
    private String  token;
    private String id;
}
