CREATE TABLE tracks (
	track_id          VARCHAR(20) PRIMARY KEY,
	track_name        VARCHAR(100),
	start_node        VARCHAR(20),
	end_node          VARCHAR(20),
	distance          INT,
	min_speed_limit   DECIMAL(6,2),
	max_speed_limit   DECIMAL(6,2),
	in_use            BOOLEAN DEFAULT FALSE
);
