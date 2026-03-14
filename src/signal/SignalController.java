package signal;

import model.Track;
import model.Train;

public class SignalController {

	public boolean isSpeedValid(Train train, Track track) {
		if (train.getSpeed() > track.getMaxSpeedLimit()) {
			System.out.println("WARNING: Train " + train.getId()
				+ " exceeded max speed on track " + track.getId()
				+ " (speed=" + train.getSpeed()
				+ ", max=" + track.getMaxSpeedLimit() + ")"
				+ " | forcing signal RED");
			return false;
		}

		if (train.getSpeed() < track.getMinSpeedLimit()
			&& track.getId().equals(train.getTrackOnUse())) {
			System.out.println("WARNING: Train " + train.getId()
				+ " below min speed after entering track " + track.getId()
				+ " (speed=" + train.getSpeed()
				+ ", min=" + track.getMinSpeedLimit() + ")");
		}

		return train.getSpeed() >= track.getMinSpeedLimit()
			&& train.getSpeed() <= track.getMaxSpeedLimit();
	}
}
