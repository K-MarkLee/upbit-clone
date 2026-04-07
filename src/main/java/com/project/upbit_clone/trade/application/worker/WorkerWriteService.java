package com.project.upbit_clone.trade.application.worker;

import com.project.upbit_clone.trade.application.dispatch.CommandMessage;
import com.project.upbit_clone.trade.application.engine.EngineResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkerWriteService {

    @Transactional
    public void writePlace(CommandMessage.Place message, EngineResult.PlaceResult result) {

    }
}
