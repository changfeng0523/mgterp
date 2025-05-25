package com.mogutou.erp.nli;

import com.mogutou.erp.nli.model.NLIRequest;
import com.mogutou.erp.nli.model.NLIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/nli")
public class NLIController {

    @Autowired
    private NLIService nliService;

    @PostMapping("/parse")
    public NLIResponse parse(@RequestBody NLIRequest request) {
        return nliService.parseInput(request.getInput(), request.isConfirmed());
    }
}
