package io.github.wax911.retgraph.model.parent;

import io.github.wax911.retgraph.model.Repository;
import io.github.wax911.retgraph.model.User;
import io.github.wax911.retgraph.model.Vote;

public class Entry {

    private long id;
    private Vote vote;
    private double score;
    private User postedBy;
    private double hotScore;
    private Repository repository;

    public long getId() {
        return id;
    }

    public Vote getVote() {
        return vote;
    }

    public double getScore() {
        return score;
    }

    public User getPostedBy() {
        return postedBy;
    }

    public double getHotScore() {
        return hotScore;
    }

    public Repository getRepository() {
        return repository;
    }
}
