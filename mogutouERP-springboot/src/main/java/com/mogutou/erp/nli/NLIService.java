package com.mogutou.erp.nli;

import com.mogutou.erp.nli.model.NLIResponse;

public interface NLIService {
    NLIResponse parseInput(String input, boolean confirmed);
}
