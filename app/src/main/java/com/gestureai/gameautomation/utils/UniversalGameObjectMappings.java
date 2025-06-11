package com.gestureai.gameautomation.utils;


public class UniversalGameObjectMappings {

    /**
     * Universal game object detection and action mapping system
     * Supports 300+ game objects across all genres with gesture integration
     */

    public static String getActionForObject(String customObjectType, String gestureType) {
        // Priority 1: Hand Gestures (from MediaPipeHandGestureProcessor)
        if (gestureType != null && customObjectType.startsWith("hand_gesture_")) {
            return mapHandGestureToAction(gestureType);
        }

        // Priority 2: Game Objects
        return mapGameObjectToAction(customObjectType);
    }

    private static String mapHandGestureToAction(String gestureType) {
        switch (gestureType) {
            // Basic Movement Gestures
            case "swipe_left":  return "MOVE_LEFT";
            case "swipe_right": return "MOVE_RIGHT";
            case "swipe_up":    return "JUMP";
            case "swipe_down":  return "SLIDE";
            case "fist":        return "ATTACK";
            case "open_hand":   return "PAUSE";
            case "point":       return "SELECT";
            case "peace":       return "SPECIAL_MOVE";
            case "thumbs_up":   return "CONFIRM";

            // Advanced Gestures
            case "swipe_up_left":    return "JUMP_LEFT";
            case "swipe_up_right":   return "JUMP_RIGHT";
            case "swipe_down_left":  return "SLIDE_LEFT";
            case "swipe_down_right": return "SLIDE_RIGHT";
            case "pinch":            return "ZOOM_OUT";
            case "spread":           return "ZOOM_IN";
            case "circle_clockwise": return "ROTATE_RIGHT";
            case "circle_counter":   return "ROTATE_LEFT";
            case "wave":             return "CELEBRATE";
            case "grab":             return "PICKUP";
            case "release":          return "DROP";

            default: return "NONE";
        }
    }

    private static String mapGameObjectToAction(String customObjectType) {
        // RUNNER GAMES (Subway Surfers, Temple Run, etc.)
        if (customObjectType.startsWith("runner_")) {
            switch (customObjectType) {
                case "runner_train_left":      return "MOVE_RIGHT";
                case "runner_train_right":     return "MOVE_LEFT";
                case "runner_train_center":    return "JUMP";
                case "runner_barrier_low":     return "JUMP";
                case "runner_barrier_high":    return "SLIDE";
                case "runner_coin_left":       return "MOVE_LEFT_COLLECT";
                case "runner_coin_center":     return "COLLECT";
                case "runner_coin_right":      return "MOVE_RIGHT_COLLECT";
                case "runner_jetpack":         return "ACTIVATE_JETPACK";
                case "runner_magnet":          return "ACTIVATE_MAGNET";
                case "runner_multiplier":      return "ACTIVATE_MULTIPLIER";
                case "runner_super_sneakers":  return "ACTIVATE_SPEED";
                case "runner_coin_trail":      return "FOLLOW_TRAIL";
                case "runner_ramp":            return "USE_RAMP";
                case "runner_tunnel":          return "ENTER_TUNNEL";
                case "runner_helicopter":      return "AVOID_HELICOPTER";
                default: return "COLLECT";
            }
        }

        // PUZZLE GAMES (Candy Crush, Match-3, etc.)
        if (customObjectType.startsWith("puzzle_")) {
            switch (customObjectType) {
                case "puzzle_candy_red":       return "SELECT_RED";
                case "puzzle_candy_blue":      return "SELECT_BLUE";
                case "puzzle_candy_green":     return "SELECT_GREEN";
                case "puzzle_candy_yellow":    return "SELECT_YELLOW";
                case "puzzle_candy_orange":    return "SELECT_ORANGE";
                case "puzzle_candy_purple":    return "SELECT_PURPLE";
                case "puzzle_striped_candy":   return "ACTIVATE_STRIPED";
                case "puzzle_wrapped_candy":   return "ACTIVATE_WRAPPED";
                case "puzzle_color_bomb":      return "ACTIVATE_COLOR_BOMB";
                case "puzzle_chocolate":       return "CLEAR_CHOCOLATE";
                case "puzzle_jelly":           return "CLEAR_JELLY";
                case "puzzle_blocker":         return "BREAK_BLOCKER";
                case "puzzle_match_3":         return "MAKE_MATCH_3";
                case "puzzle_match_4":         return "MAKE_MATCH_4";
                case "puzzle_match_5":         return "MAKE_MATCH_5";
                case "puzzle_l_shape":         return "MAKE_L_MATCH";
                case "puzzle_t_shape":         return "MAKE_T_MATCH";
                case "puzzle_bomb":            return "TRIGGER_BOMB";
                case "puzzle_rocket":          return "TRIGGER_ROCKET";
                case "puzzle_fish":            return "TRIGGER_FISH";
                default: return "SELECT";
            }
        }

        // SHOOTER GAMES (PUBG, Call of Duty, etc.)
        if (customObjectType.startsWith("shooter_")) {
            switch (customObjectType) {
                case "shooter_enemy_visible":   return "AIM_AND_SHOOT";
                case "shooter_enemy_hidden":    return "SEARCH_AND_DESTROY";
                case "shooter_sniper":          return "TAKE_COVER";
                case "shooter_grenade":         return "THROW_GRENADE";
                case "shooter_health_pack":     return "PICKUP_HEALTH";
                case "shooter_armor":           return "PICKUP_ARMOR";
                case "shooter_ammo":            return "PICKUP_AMMO";
                case "shooter_weapon_ar":       return "PICKUP_ASSAULT_RIFLE";
                case "shooter_weapon_sniper":   return "PICKUP_SNIPER";
                case "shooter_weapon_shotgun":  return "PICKUP_SHOTGUN";
                case "shooter_scope":           return "PICKUP_SCOPE";
                case "shooter_cover":           return "TAKE_COVER";
                case "shooter_vehicle":         return "ENTER_VEHICLE";
                case "shooter_supply_drop":     return "MOVE_TO_SUPPLY";
                case "shooter_zone_edge":       return "MOVE_TO_SAFE_ZONE";
                case "shooter_teammate":        return "REVIVE_TEAMMATE";
                case "shooter_door":            return "OPEN_DOOR";
                case "shooter_window":          return "PEEK_WINDOW";
                default: return "SHOOT";
            }
        }

        // STRATEGY GAMES (Clash of Clans, Age of Empires, etc.)
        if (customObjectType.startsWith("strategy_")) {
            switch (customObjectType) {
                case "strategy_enemy_base":     return "ATTACK_BASE";
                case "strategy_resource_gold":  return "COLLECT_GOLD";
                case "strategy_resource_wood":  return "COLLECT_WOOD";
                case "strategy_resource_stone": return "COLLECT_STONE";
                case "strategy_unit_warrior":   return "DEPLOY_WARRIOR";
                case "strategy_unit_archer":    return "DEPLOY_ARCHER";
                case "strategy_unit_mage":      return "DEPLOY_MAGE";
                case "strategy_unit_tank":      return "DEPLOY_TANK";
                case "strategy_building_tower": return "BUILD_TOWER";
                case "strategy_building_wall":  return "BUILD_WALL";
                case "strategy_spell_heal":     return "CAST_HEAL";
                case "strategy_spell_rage":     return "CAST_RAGE";
                case "strategy_spell_freeze":   return "CAST_FREEZE";
                case "strategy_trap":           return "SET_TRAP";
                case "strategy_upgrade":        return "UPGRADE_BUILDING";
                default: return "BUILD";
            }
        }

        // RPG GAMES (Final Fantasy, Pokemon, etc.)
        if (customObjectType.startsWith("rpg_")) {
            switch (customObjectType) {
                case "rpg_enemy_weak":          return "BASIC_ATTACK";
                case "rpg_enemy_strong":        return "SPECIAL_ATTACK";
                case "rpg_boss":                return "ULTIMATE_ATTACK";
                case "rpg_treasure_chest":      return "OPEN_CHEST";
                case "rpg_healing_potion":      return "USE_HEALING_POTION";
                case "rpg_mana_potion":         return "USE_MANA_POTION";
                case "rpg_weapon":              return "EQUIP_WEAPON";
                case "rpg_armor":               return "EQUIP_ARMOR";
                case "rpg_npc":                 return "TALK_TO_NPC";
                case "rpg_quest_giver":         return "ACCEPT_QUEST";
                case "rpg_shop":                return "ENTER_SHOP";
                case "rpg_inn":                 return "REST_AT_INN";
                case "rpg_dungeon_entrance":    return "ENTER_DUNGEON";
                case "rpg_exit":                return "EXIT_AREA";
                case "rpg_save_point":          return "SAVE_GAME";
                case "rpg_level_up":            return "LEVEL_UP";
                default: return "INTERACT";
            }
        }

        // RACING GAMES (Need for Speed, Asphalt, etc.)
        if (customObjectType.startsWith("racing_")) {
            switch (customObjectType) {
                case "racing_turn_left":        return "STEER_LEFT";
                case "racing_turn_right":       return "STEER_RIGHT";
                case "racing_nitro":            return "ACTIVATE_NITRO";
                case "racing_brake":            return "BRAKE";
                case "racing_ramp":             return "USE_RAMP";
                case "racing_opponent":         return "OVERTAKE";
                case "racing_checkpoint":       return "PASS_CHECKPOINT";
                case "racing_finish_line":      return "CROSS_FINISH";
                case "racing_oil_spill":        return "AVOID_HAZARD";
                case "racing_shortcut":         return "TAKE_SHORTCUT";
                default: return "ACCELERATE";
            }
        }

        // PLATFORM GAMES (Mario, Sonic, etc.)
        if (customObjectType.startsWith("platform_")) {
            switch (customObjectType) {
                case "platform_goomba":         return "JUMP_ON_ENEMY";
                case "platform_koopa":          return "KICK_SHELL";
                case "platform_coin":           return "COLLECT_COIN";
                case "platform_power_up":       return "COLLECT_POWER_UP";
                case "platform_mushroom":       return "COLLECT_MUSHROOM";
                case "platform_fire_flower":    return "COLLECT_FIRE_FLOWER";
                case "platform_star":           return "COLLECT_STAR";
                case "platform_pipe":           return "ENTER_PIPE";
                case "platform_flag":           return "REACH_FLAG";
                case "platform_moving_platform": return "JUMP_ON_PLATFORM";
                case "platform_spring":         return "USE_SPRING";
                case "platform_pit":            return "AVOID_PIT";
                default: return "JUMP";
            }
        }

        // CARD GAMES (Hearthstone, Magic, etc.)
        if (customObjectType.startsWith("card_")) {
            switch (customObjectType) {
                case "card_playable":           return "PLAY_CARD";
                case "card_creature":           return "SUMMON_CREATURE";
                case "card_spell":              return "CAST_SPELL";
                case "card_artifact":           return "PLAY_ARTIFACT";
                case "card_land":               return "PLAY_LAND";
                case "card_hand":               return "DRAW_CARD";
                case "card_deck":               return "SHUFFLE_DECK";
                case "card_graveyard":          return "RESURRECT";
                case "card_opponent":           return "ATTACK_OPPONENT";
                case "card_defend":             return "BLOCK_ATTACK";
                default: return "PLAY_CARD";
            }
        }

        // TOWER DEFENSE GAMES
        if (customObjectType.startsWith("tower_")) {
            switch (customObjectType) {
                case "tower_spot_available":    return "BUILD_TOWER";
                case "tower_upgrade_available": return "UPGRADE_TOWER";
                case "tower_cannon":            return "BUILD_CANNON_TOWER";
                case "tower_archer":            return "BUILD_ARCHER_TOWER";
                case "tower_magic":             return "BUILD_MAGIC_TOWER";
                case "tower_ice":               return "BUILD_ICE_TOWER";
                case "tower_fire":              return "BUILD_FIRE_TOWER";
                case "tower_enemy_wave":        return "PREPARE_DEFENSE";
                case "tower_boss":              return "FOCUS_FIRE";
                default: return "BUILD_TOWER";
            }
        }

        // MOBA GAMES (League of Legends, DOTA, etc.)
        if (customObjectType.startsWith("moba_")) {
            switch (customObjectType) {
                case "moba_enemy_champion":     return "ENGAGE_CHAMPION";
                case "moba_minion":             return "LAST_HIT_MINION";
                case "moba_jungle_monster":     return "CLEAR_JUNGLE";
                case "moba_tower":              return "ATTACK_TOWER";
                case "moba_ward_spot":          return "PLACE_WARD";
                case "moba_bush":               return "CHECK_BUSH";
                case "moba_dragon":             return "SECURE_DRAGON";
                case "moba_baron":              return "SECURE_BARON";
                case "moba_teammate":           return "SUPPORT_TEAMMATE";
                case "moba_item_shop":          return "BUY_ITEMS";
                default: return "ATTACK";
            }
        }

        // FIGHTING GAMES (Street Fighter, Tekken, etc.)
        if (customObjectType.startsWith("fight_")) {
            switch (customObjectType) {
                case "fight_opponent_close":    return "COMBO_ATTACK";
                case "fight_opponent_far":      return "PROJECTILE_ATTACK";
                case "fight_opponent_jumping":  return "ANTI_AIR";
                case "fight_opponent_blocking": return "THROW_ATTACK";
                case "fight_special_meter_full": return "SPECIAL_MOVE";
                case "fight_ultra_meter_full":  return "ULTRA_MOVE";
                case "fight_low_health":        return "DEFENSIVE_PLAY";
                default: return "ATTACK";
            }
        }

        // SIMULATION GAMES (SimCity, Sims, etc.)
        if (customObjectType.startsWith("sim_")) {
            switch (customObjectType) {
                case "sim_building_residential": return "BUILD_RESIDENTIAL";
                case "sim_building_commercial":  return "BUILD_COMMERCIAL";
                case "sim_building_industrial":  return "BUILD_INDUSTRIAL";
                case "sim_road":                 return "BUILD_ROAD";
                case "sim_power_plant":          return "BUILD_POWER_PLANT";
                case "sim_water_tower":          return "BUILD_WATER_TOWER";
                case "sim_fire_station":         return "BUILD_FIRE_STATION";
                case "sim_police_station":       return "BUILD_POLICE_STATION";
                case "sim_hospital":             return "BUILD_HOSPITAL";
                case "sim_school":               return "BUILD_SCHOOL";
                case "sim_park":                 return "BUILD_PARK";
                case "sim_disaster":             return "HANDLE_DISASTER";
                default: return "BUILD";
            }
        }

        // SPORTS GAMES (FIFA, NBA, etc.)
        if (customObjectType.startsWith("sports_")) {
            switch (customObjectType) {
                case "sports_ball":              return "CONTROL_BALL";
                case "sports_goal":              return "SHOOT_GOAL";
                case "sports_pass_teammate":     return "PASS_BALL";
                case "sports_tackle_opponent":   return "TACKLE";
                case "sports_sprint":            return "SPRINT";
                case "sports_defend":            return "DEFEND";
                case "sports_free_kick":         return "TAKE_FREE_KICK";
                case "sports_penalty":           return "TAKE_PENALTY";
                case "sports_corner_kick":       return "TAKE_CORNER";
                case "sports_throw_in":          return "THROW_IN";
                default: return "PLAY";
            }
        }

        // GENERAL GAME OBJECTS
        switch (customObjectType) {
            // Common Collectibles
            case "coin": case "gold": case "money":                    return "COLLECT";
            case "gem": case "diamond": case "crystal":                return "COLLECT_VALUABLE";
            case "star": case "trophy": case "medal":                  return "COLLECT_BONUS";
            case "key": case "keycard": case "access_card":            return "PICKUP_KEY";
            case "health": case "heart": case "life":                  return "HEAL";
            case "energy": case "mana": case "magic":                  return "RESTORE_ENERGY";
            case "shield": case "protection": case "armor":            return "ACTIVATE_SHIELD";
            case "weapon": case "sword": case "gun":                   return "PICKUP_WEAPON";
            case "ammo": case "bullets": case "arrows":                return "PICKUP_AMMO";
            case "food": case "apple": case "bread":                   return "EAT";
            case "potion": case "elixir": case "medicine":             return "DRINK_POTION";

            // Common Obstacles
            case "wall": case "barrier": case "fence":                 return "AVOID";
            case "spike": case "trap": case "mine":                    return "AVOID_TRAP";
            case "fire": case "lava": case "acid":                     return "AVOID_HAZARD";
            case "enemy": case "monster": case "zombie":               return "ATTACK";
            case "boss": case "dragon": case "giant":                  return "BOSS_FIGHT";
            case "pit": case "hole": case "cliff":                     return "JUMP_OVER";
            case "water": case "river": case "ocean":                  return "SWIM";
            case "ice": case "slippery": case "snow":                  return "CAREFUL_MOVEMENT";

            // Interactive Objects
            case "door": case "gate": case "entrance":                 return "OPEN";
            case "button": case "switch": case "lever":                return "ACTIVATE";
            case "chest": case "box": case "container":                return "OPEN_CONTAINER";
            case "ladder": case "stairs": case "elevator":             return "CLIMB";
            case "rope": case "vine": case "chain":                    return "SWING";
            case "teleporter": case "portal": case "warp":             return "TELEPORT";
            case "checkpoint": case "save_point": case "flag":         return "ACTIVATE_CHECKPOINT";

            // Vehicle Objects
            case "car": case "vehicle": case "truck":                  return "ENTER_VEHICLE";
            case "boat": case "ship": case "raft":                     return "BOARD_BOAT";
            case "plane": case "helicopter": case "aircraft":          return "BOARD_AIRCRAFT";
            case "train": case "subway": case "locomotive":            return "BOARD_TRAIN";
            case "bike": case "motorcycle": case "scooter":            return "RIDE_BIKE";

            // Technology Objects
            case "computer": case "terminal": case "console":          return "USE_COMPUTER";
            case "phone": case "radio": case "communicator":           return "COMMUNICATE";
            case "camera": case "scanner": case "detector":            return "SCAN";
            case "robot": case "android": case "drone":                return "CONTROL_ROBOT";
            case "laser": case "beam": case "ray":                     return "ACTIVATE_LASER";

            // Nature Objects
            case "tree": case "bush": case "plant":                    return "INTERACT_NATURE";
            case "rock": case "stone": case "boulder":                 return "MOVE_ROCK";
            case "flower": case "herb": case "grass":                  return "PICK";
            case "cave": case "tunnel": case "burrow":                 return "ENTER_CAVE";
            case "mountain": case "hill": case "peak":                 return "CLIMB_MOUNTAIN";

            // Building Objects
            case "house": case "building": case "structure":           return "ENTER_BUILDING";
            case "tower": case "castle": case "fortress":              return "ENTER_TOWER";
            case "shop": case "store": case "market":                  return "ENTER_SHOP";
            case "inn": case "hotel": case "tavern":                   return "REST";
            case "temple": case "church": case "shrine":               return "PRAY";

            // Special Effects
            case "explosion": case "blast": case "boom":               return "TAKE_COVER";
            case "lightning": case "thunder": case "storm":            return "SEEK_SHELTER";
            case "earthquake": case "tremor": case "shake":            return "BRACE";
            case "tornado": case "hurricane": case "cyclone":          return "EVACUATE";
            case "flood": case "tsunami": case "wave":                 return "CLIMB_HIGH";

            // UI Elements
            case "menu": case "options": case "settings":              return "OPEN_MENU";
            case "pause": case "stop": case "halt":                    return "PAUSE_GAME";
            case "play": case "start": case "begin":                   return "START_GAME";
            case "restart": case "retry": case "again":                return "RESTART";
            case "quit": case "exit": case "leave":                    return "QUIT_GAME";

            default: return "INTERACT";
        }
    }

    /**
     * Get confidence adjustment based on object type
     */
    public static float getConfidenceAdjustment(String customObjectType) {
        if (customObjectType.startsWith("runner_")) return 0.95f;
        if (customObjectType.startsWith("puzzle_")) return 0.90f;
        if (customObjectType.startsWith("shooter_")) return 0.85f;
        if (customObjectType.startsWith("strategy_")) return 0.80f;
        if (customObjectType.startsWith("rpg_")) return 0.85f;
        if (customObjectType.startsWith("racing_")) return 0.90f;
        if (customObjectType.startsWith("platform_")) return 0.95f;
        return 0.75f; // Default confidence for general objects
    }

    /**
     * Get action priority (higher number = higher priority)
     */
    public static int getActionPriority(String action) {
        switch (action) {
            case "AVOID": case "AVOID_TRAP": case "AVOID_HAZARD": return 10;
            case "BOSS_FIGHT": case "ATTACK": return 9;
            case "COLLECT_VALUABLE": case "PICKUP_KEY": return 8;
            case "HEAL": case "RESTORE_ENERGY": return 7;
            case "COLLECT": case "PICKUP_WEAPON": return 6;
            case "JUMP": case "MOVE_LEFT": case "MOVE_RIGHT": return 5;
            case "INTERACT": case "ACTIVATE": return 4;
            case "PAUSE_GAME": case "START_GAME": return 3;
            default: return 1;
        }
    }
}
