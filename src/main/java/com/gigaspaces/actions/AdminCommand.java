package com.gigaspaces.actions;

public abstract class AdminCommand {
    private String requester;

    @SuppressWarnings("WeakerAccess")
    public AdminCommand(String requester) {
        this.requester = requester;
    }

    public String getRequester() {
        return requester;
    }

}
