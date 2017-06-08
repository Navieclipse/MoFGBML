package time;

public class TimeWatcher {

	public TimeWatcher(){}

	double start;
	double end;
	double time = 0.0;

	public void start(){
		start = System.nanoTime();
	}

	public void end(){
		end = System.nanoTime();
		time += (end - start);
	}

	public double getSec(){
		return (time/1000000000.0);
	}

	public double getNano(){
		return time;
	}

}
