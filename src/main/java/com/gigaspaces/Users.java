package com.gigaspaces;

import io.vavr.collection.List;
import org.ini4j.Profile;
import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("unused")
class Users {
    private List<User> users;

    private Users(List<User> users) {
        this.users = users;
    }

    public static Users readFrom(String path) throws IOException {
        //noinspection MismatchedQueryAndUpdateOfCollection
        final Wini ini = new Wini(new File(path));
        java.util.List<User> l = new ArrayList<>();
        for (String key : ini.keySet()) {
            Profile.Section section = ini.get(key);
            User user = new User();
            section.to(user);
            l.add(user);
        }

        return new Users(List.ofAll(l));
    }

    public List<User> admin(){
        return users.filter(User::isAdmin);
    }

    public List<User> monitored(){
        return users.filter(User::isMonitored);
    }
    public List<User> notified(){
        return users.filter(User::isNotified);
    }
}

