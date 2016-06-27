class SpaceShip {

	int position;
	int speed;
	final static int MAX_SPEED = 1000000;
	static int[] spacePortPositions;

	SpaceShip() {

		position = 0;
		speed = 0;
		spacePortPositions = new int[2];
		spacePortPositions[0] = 5;
		spacePortPositions[1] = 8 * spacePortPositions[0];
	}

	void move() {

		if(speed <= MAX_SPEED) {

			position = position + speed;

		} else {

			speed = 0;
		}
	}

	void moveTo(int pos, int travelSpeed) {

		position = pos;
		int saveSpeed = speed;
		speed = travelSpeed;
		move();
		speed = saveSpeed;
	}
}