package com.videoservice.manager.external.naver;

import java.util.List;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NaverBookResponse {
    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<Item> items;
}