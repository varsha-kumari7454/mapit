package exceptions;

public class CandidateAlreadyExist extends Exception {
	private static final long serialVersionUID = 1L;

	public CandidateAlreadyExist(String string) {
		super(string);
	}
}
