package io.github.wax911.retgraph.model;

public class Repository {

    private String full_name;
    private String name;
    private User owner;
    private int stargazers_count;

    public String getFull_name() {
        return full_name;
    }

    public String getName() {
        return name;
    }

    public User getOwner() {
        return owner;
    }

    public int getStargazers_count() {
        return stargazers_count;
    }
}
