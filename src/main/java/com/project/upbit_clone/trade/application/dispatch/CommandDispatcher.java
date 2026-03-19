package com.project.upbit_clone.trade.application.dispatch;

public interface CommandDispatcher {

    void dispatch(CommandMessage message);
}
