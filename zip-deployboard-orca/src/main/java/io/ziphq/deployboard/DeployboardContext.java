package io.ziphq.deployboard;

import lombok.Data;

@Data
public class DeployboardContext {
    private String branch;
    private String image;
    private Boolean success;
}
