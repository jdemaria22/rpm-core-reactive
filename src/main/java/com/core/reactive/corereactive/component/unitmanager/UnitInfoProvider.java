package com.core.reactive.corereactive.component.unitmanager;

import com.core.reactive.corereactive.component.unitmanager.model.Unit;

import java.util.List;
import java.util.function.Function;

public interface UnitInfoProvider<UNIT extends Unit> {
    List<Function<FunctionInfo<UNIT>, UNIT>> getUnitInfo();
}
