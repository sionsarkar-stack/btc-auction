package com.btc.btc_auction.model;

import java.util.List;

public class TargetsReadyResponse {
    private boolean allSubmitted;
    private List<String> missingSecretTargets;
    private List<String> missingReverseTargets;
    private int bountyCount;
    private int goldenBountyCount;

    public TargetsReadyResponse(boolean allSubmitted, List<String> missingSecretTargets,
            List<String> missingReverseTargets, int bountyCount, int goldenBountyCount) {
        this.allSubmitted = allSubmitted;
        this.missingSecretTargets = missingSecretTargets;
        this.missingReverseTargets = missingReverseTargets;
        this.bountyCount = bountyCount;
        this.goldenBountyCount = goldenBountyCount;
    }

    public boolean isAllSubmitted() {
        return allSubmitted;
    }

    public void setAllSubmitted(boolean allSubmitted) {
        this.allSubmitted = allSubmitted;
    }

    public List<String> getMissingSecretTargets() {
        return missingSecretTargets;
    }

    public void setMissingSecretTargets(List<String> missingSecretTargets) {
        this.missingSecretTargets = missingSecretTargets;
    }

    public List<String> getMissingReverseTargets() {
        return missingReverseTargets;
    }

    public void setMissingReverseTargets(List<String> missingReverseTargets) {
        this.missingReverseTargets = missingReverseTargets;
    }

    public int getBountyCount() {
        return bountyCount;
    }

    public void setBountyCount(int bountyCount) {
        this.bountyCount = bountyCount;
    }

    public int getGoldenBountyCount() {
        return goldenBountyCount;
    }

    public void setGoldenBountyCount(int goldenBountyCount) {
        this.goldenBountyCount = goldenBountyCount;
    }
}
