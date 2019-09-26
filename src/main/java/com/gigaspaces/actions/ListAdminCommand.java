package com.gigaspaces.actions;

public class ListAdminCommand extends AdminCommand {
    public ListAdminCommand(String requester) {
        super(requester);
    }

    @Override
    public String toString() {
        return "ListAdminCommand{}";
    }
}
