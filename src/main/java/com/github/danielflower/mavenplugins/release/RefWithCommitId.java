package com.github.danielflower.mavenplugins.release;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

public final class RefWithCommitId
{
    private final Ref ref;
    private final ObjectId commitId;

    public RefWithCommitId(Ref ref, ObjectId commitId) {
        Guard.notNull("ref", ref);
        Guard.notNull("commitId", commitId);
        this.ref = ref;
        this.commitId = commitId;
    }

    public Ref getRef() {
        return ref;
    }



    public ObjectId getCommitObjectId() {
        return commitId;
    }

    @Override
    public String toString() {
        return "RefWithCommitId{" +
            "ref='" + ref.toString() + '\'' +
            ", commitId=" + commitId.toString() +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefWithCommitId that = (RefWithCommitId) o;
        return ref.equals(that.ref);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }
}
