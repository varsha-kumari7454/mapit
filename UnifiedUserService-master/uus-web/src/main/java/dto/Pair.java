package dto;

public class Pair {
	private Object a;
	private Object b;
	public Pair(Object a, Object b) {
		super();
		this.a = a;
		this.b = b;
	}
	public Pair() {
		// TODO Auto-generated constructor stub
	}
	public Object getA() {
		return a;
	}
	public Object getB() {
		return b;
	}
	public void setA(Object a) {
		this.a = a;
	}
	public void setB(Object b) {
		this.b = b;
	}
}
