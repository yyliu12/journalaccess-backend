package com.info25.journalindex.models;

import java.math.BigDecimal;
import java.util.List;

import com.info25.generated.tables.pojos.Files;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JooqFile extends Files {
    List<BigDecimal> locationIds = List.of();
    List<BigDecimal> tagIds = List.of();
    Boolean hasParent = null;
}
