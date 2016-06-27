class FlySpaceShip {

	static void tinyMain() {

		SpaceShip shuttle = new SpaceShip();
		Destination dest = new Destination(100);
		int a = 2 * shuttle.spacePortPositions[0];

		shuttle.position = a;
		shuttle.speed = 20;
		shuttle.moveTo(12, 28);
	}
}
