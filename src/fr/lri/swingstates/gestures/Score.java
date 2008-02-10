package fr.lri.swingstates.gestures;

public class Score {

	String name;
	double score;
	
	public Score(String name, double score) {
		super();
		this.name = name;
		this.score = score;
	}

	public double getScore() {
		return score;
	}

	public String getName() {
		return name;
	}
	
}
