package com.classroom.dto;

import lombok.Data;

import java.util.List;

@Data
public class VoteSubmitRequest {

    private String optionKey;

    private List<String> optionKeys;
}


