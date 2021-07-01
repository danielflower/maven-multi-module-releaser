package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;

public class AnnotatedTag {
    public static final String VERSION = "version";
    public static final String BUILD_NUMBER = "buildNumber";
    public static final String TAG_PREFIX = "refs/tags/";
    private final String name;
    private final JSONObject message;
    private Ref ref;
    private RevCommit tagCommit;

    private AnnotatedTag(Ref ref, RevCommit tagCommit, String name, JSONObject message) {
        Guard.notBlank("tag name", name);
        Guard.notNull("tag message", message);
        this.ref = ref;
        this.name = name;
        this.message = message;
        this.tagCommit = tagCommit;
    }

    public ObjectId getObjectId() {
        return tagCommit.getId();
    }

    public static AnnotatedTag create(String name, String version, long buildNumber) {
        JSONObject message = new JSONObject();
        message.put(VERSION, version);
        message.put(BUILD_NUMBER, String.valueOf(buildNumber));
        return new AnnotatedTag(null, null, name, message);
    }

    public static AnnotatedTag fromRef(Repository repository, Ref gitTag) throws IOException, IncorrectObjectTypeException {
        Guard.notNull("gitTag", gitTag);

        RevWalk walk = new RevWalk(repository);
        JSONObject message;
        RevCommit tagCommit;
        try {
            ObjectId tagId = gitTag.getObjectId();
            RevTag tag = walk.parseTag(tagId);
            tagCommit = walk.parseCommit(tag);
            message = (JSONObject) JSONValue.parse(tag.getFullMessage());
        } finally {
            walk.dispose();
        }
        if (message == null) {
            message = new JSONObject();
            message.put(VERSION, "0");
            message.put(BUILD_NUMBER, "0");
        }
        return new AnnotatedTag(gitTag, tagCommit, stripRefPrefix(gitTag.getName()), message);
    }


    public static AnnotatedTag fromRefCommit(Repository repository, Ref gitTag, RevCommit commit) throws IOException, IncorrectObjectTypeException {
        Guard.notNull("gitTag", gitTag);

        RevWalk walk = new RevWalk(repository);
        JSONObject message;
        try {
            ObjectId tagId = gitTag.getObjectId();
            RevTag tag = walk.parseTag(tagId);
            message = (JSONObject) JSONValue.parse(tag.getFullMessage());
        } finally {
            walk.dispose();
        }
        if (message == null) {
            message = new JSONObject();
            message.put(VERSION, "0");
            message.put(BUILD_NUMBER, "0");
        }
        return new AnnotatedTag(gitTag, commit, stripRefPrefix(gitTag.getName()), message);
    }

    static String stripRefPrefix(String refName) {
        assert refName.startsWith(TAG_PREFIX);
        return refName.substring(TAG_PREFIX.length());
    }

    public String name() {
        return name;
    }

    public String version() {
        return String.valueOf(message.get(VERSION));
    }

    public long buildNumber() {
        return Long.parseLong(String.valueOf(message.get(BUILD_NUMBER)));
    }

    public Ref saveAtHEAD(Git git) throws GitAPIException {
        String json = message.toJSONString();
        ref = git.tag().setName(name()).setAnnotated(true).setMessage(json).call();
        tagCommit = git.log().setMaxCount(1).call().iterator().next();
        return ref;
    }

    @Override
    public String toString() {
        return "AnnotatedTag{" +
            "name='" + name + '\'' +
            ", version=" + version() +
            ", buildNumber=" + buildNumber() +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotatedTag that = (AnnotatedTag) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Ref ref() {
        return ref;
    }
}
