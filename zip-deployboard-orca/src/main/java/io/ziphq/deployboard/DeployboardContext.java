package io.ziphq.deployboard;

import lombok.Data;

@Data
public class DeployboardContext {
    private final String branch;
    private final String image;
    private final Boolean success;
}
