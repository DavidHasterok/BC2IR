import java.util.Vector;

class FrameStack {

	private Vector<Frame> frames;

	FrameStack() {

		frames = new Vector<Frame>();
	}

	void pushFrame(Frame f) {

		f.opStack.beginFrame();
		frames.add(0, f);
	}

	int popFrame(Frame f, boolean returnValue) {

		int returnInt = 0;

		if(returnValue) {

			returnInt = f.opStack.pop().value;
		}

		//still frames left to execute
		if(frames.size() > 1) {

			f.opStack.endFrame();

		//no more frames to execute
		} else {

			f.opStack.endExecution();
			
		}

		//stop executing the current frame
		f.pc = f.codeArray.length;
		frames.remove(0);
		return returnInt;
	}
}