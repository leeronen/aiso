package com.aios.platform.kb.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class KbBaseTreeNode {

    private Long key;
    private String title;
    private Integer documentCount;
    private List<KbBaseTreeNode> children = new ArrayList<>();
}
