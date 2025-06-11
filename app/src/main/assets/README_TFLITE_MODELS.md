# TensorFlow Lite Models for Neural Network RL Agents

## Required Model Files

The following TensorFlow Lite models need to be placed in the `assets/` directory for the RL agents to function with real neural networks:

### DQN Agent Models
- `dqn_q_network.tflite` - Main Q-network for action-value estimation
- `dqn_target_network.tflite` - Target network for stable training

### PPO Agent Models  
- `ppo_actor_network.tflite` - Actor network for policy (action probabilities)
- `ppo_critic_network.tflite` - Critic network for value estimation

## Model Architecture Specifications

### DQN Networks
- **Input**: 55-dimensional state vector (float32)
- **Output**: 9-dimensional Q-values (float32)
- **Architecture**: Dense layers with ReLU activation
- **Expected shape**: [1, 55] → [1, 9]

### PPO Actor Network
- **Input**: 55-dimensional state vector (float32)
- **Output**: 9-dimensional logits (float32) 
- **Architecture**: Dense layers with final softmax for probabilities
- **Expected shape**: [1, 55] → [1, 9]

### PPO Critic Network
- **Input**: 55-dimensional state vector (float32)
- **Output**: Single value estimate (float32)
- **Architecture**: Dense layers with linear output
- **Expected shape**: [1, 55] → [1, 1]

## State Vector Components (55 features)

1. **Player Position** (2): Normalized x, y coordinates
2. **Game State** (5): Score, lives, active status, velocity, time
3. **Object Counts** (4): Coins, obstacles, powerups, enemies
4. **Distance Features** (3): Nearest obstacle, coin, powerup distances
5. **Movement Capabilities** (4): Can move left/right/jump/slide
6. **Spatial Grid** (37): 5x5 grid analysis around player

## Action Space (9 actions)

1. JUMP - Jump over obstacles
2. MOVE_LEFT - Move left lane
3. MOVE_RIGHT - Move right lane  
4. SLIDE - Slide under obstacles
5. COLLECT - Collect coins/items
6. AVOID - General avoidance maneuver
7. ACTIVATE_POWERUP - Use powerup
8. WAIT - No action
9. TAP - Generic tap action

## Creating Models

The models can be created using TensorFlow/Keras and converted to TensorFlow Lite format:

```python
# Example DQN model creation
import tensorflow as tf

def create_dqn_model():
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(128, activation='relu', input_shape=(55,)),
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dense(9, activation='linear')  # Q-values
    ])
    return model

# Convert to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save model
with open('dqn_q_network.tflite', 'wb') as f:
    f.write(tflite_model)
```

## Fallback Behavior

If the TensorFlow Lite models are not found:
- DQN Agent falls back to random action selection
- PPO Agent falls back to uniform random policy
- StrategyProcessor switches to object-detection-only mode
- System logs warnings but continues functioning

## Training Integration

The neural networks can be trained using:
- Real gameplay data from object detection
- Reward signals based on game performance
- Experience replay (DQN) and policy gradients (PPO)
- TensorFlow Lite GPU delegate for faster inference (optional)