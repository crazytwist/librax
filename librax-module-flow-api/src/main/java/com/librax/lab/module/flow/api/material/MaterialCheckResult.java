package com.librax.lab.module.flow.api.material;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 物料核验结果
 */
@Data
public class MaterialCheckResult {

    private boolean passed;
    private List<String> failReasons;

    public static MaterialCheckResult ok() {
        MaterialCheckResult r = new MaterialCheckResult();
        r.passed = true;
        r.failReasons = List.of();
        return r;
    }

    public static MaterialCheckResult fail(List<String> reasons) {
        MaterialCheckResult r = new MaterialCheckResult();
        r.passed = false;
        r.failReasons = reasons != null ? reasons : new ArrayList<>();
        return r;
    }
}
