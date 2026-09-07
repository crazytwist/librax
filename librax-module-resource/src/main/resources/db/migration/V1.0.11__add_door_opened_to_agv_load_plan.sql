ALTER TABLE lab_agv_load_plan
    ADD COLUMN door_opened BIT(1) NOT NULL DEFAULT b'0'
        COMMENT '当前车次合成站门是否已成功打开，用于提前开门及UNLOAD兜底幂等';
