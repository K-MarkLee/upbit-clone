package com.project.upbit_clone.trade.application.ingress;

import com.project.upbit_clone.trade.infrastructure.persistence.vo.CommandType;

public interface OrderCommand {

    Long userId();

    Long marketId();

    String clientOrderId();

    CommandType commandType();
}
