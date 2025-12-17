package com.arco2121.swissy.Tools.Torch;

public interface TorchListener {
    void onTorchMoment(float brightness, float lum, boolean torchOn);
}
