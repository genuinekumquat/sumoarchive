package com.torikumilab.sumoarchive.domain.entity.constant;

public enum MatchType {
	EXACT(0),
	PREFIX(1),
	CONTAINS(2),
	HISTORY(3);
	
	private final int rank;
	
	MatchType(int rank) {
		this.rank = rank;
	}
	
	public int getRank() {
		return rank;
	}
	
	public static MatchType fromRank(int rank) {
		return switch (rank) {
			case 0 -> EXACT;
			case 1 -> PREFIX;
			case 2 -> CONTAINS;
			case 3 -> HISTORY;
			default -> throw new IllegalArgumentException("Unknown match rank: " + rank);
		};
	}
}