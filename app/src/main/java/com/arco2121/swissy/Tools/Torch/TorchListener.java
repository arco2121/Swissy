package com.arco2121.swissy.Tools.Torch;

import com.arco2121.swissy.Tools.ToolListener;

public interface TorchListener extends ToolListener {
    void onTorchMoment(float brightness, float lum, boolean torchOn);
}
