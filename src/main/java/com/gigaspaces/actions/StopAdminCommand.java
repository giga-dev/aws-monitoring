package com.gigaspaces.actions;

import com.gigaspaces.Instance;

public class StopAdminCommand extends AdminCommand {
    private Instance instance;

    public StopAdminCommand(String requester, Instance instance) {
        super(requester);
        this.instance = instance;
    }

    public Instance getInstance() {
        return instance;
    }

    @Override
    public String toString() {
        return "StopAdminCommand{" +
                "instance=" + instance +
                '}';
    }
}
