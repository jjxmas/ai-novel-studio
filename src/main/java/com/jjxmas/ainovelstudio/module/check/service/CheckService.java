package com.jjxmas.ainovelstudio.module.check.service;

import com.jjxmas.ainovelstudio.module.check.dto.CheckRequest;
import com.jjxmas.ainovelstudio.module.check.dto.CheckResponse;

public interface CheckService {

    CheckResponse runCheck(CheckRequest request);
}

