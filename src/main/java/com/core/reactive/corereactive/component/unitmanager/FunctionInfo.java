package com.core.reactive.corereactive.component.unitmanager;

import com.core.reactive.corereactive.component.unitmanager.model.Unit;
import com.sun.jna.Memory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class FunctionInfo<UNIT extends Unit> {
    private UNIT unit;
    private Memory memory;
}
