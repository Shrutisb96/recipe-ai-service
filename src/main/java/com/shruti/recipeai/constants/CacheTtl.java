package com.shruti.recipeai.constants;

import lombok.Getter;

@Getter
public enum CacheTtl {
    DEFAULT(2),      // normal AI call — 2 hours
    PREWARMED(24);   // nightly job — 24 hours

    private final long hours;

    CacheTtl(long hours) { this.hours = hours; }

}
