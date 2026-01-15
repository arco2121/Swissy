package com.arco2121.swissy.Tools.Ruler;

import com.arco2121.swissy.Tools.ToolListener;

public interface RulerListener extends ToolListener {
    void onMeasure(float mm, float cm, float inch, float px);
}
