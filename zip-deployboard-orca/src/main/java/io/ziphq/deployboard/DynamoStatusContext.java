package io.ziphq.deployboard;

import lombok.Data;

@Data
public class DynamoStatusContext {
    private String branch;
    private String image;
    private Boolean success;
}
