# Required Model Files for Android Native Game Automation

This folder needs the following files to be fully functional:

## Apache OpenNLP Models (Required for NLP Processing)
Download from: https://opennlp.apache.org/models.html

Create folder: `opennlp/`
- `opennlp/en-sent.bin` - English sentence detection model
- `opennlp/en-token.bin` - English tokenization model  
- `opennlp/en-pos-maxent.bin` - English part-of-speech tagging model
- `opennlp/en-ner-person.bin` - English person name entity recognition model
- `opennlp/en-ner-location.bin` - English location name entity recognition model

## TensorFlow Lite Models (Required for Gesture Recognition)
These need to be trained or obtained from a gesture recognition dataset:

- `gesture_classifier.tflite` - Main gesture classification model
- `hand_landmark_model.tflite` - Hand landmark detection model (optional, using OpenCV fallback)

## Custom Action Mappings (Optional)
- `custom_action_mappings.json` - Game-specific action vocabulary extensions

## Example custom_action_mappings.json:
```json
{
  "action_mappings": {
    "boost": "ACTIVATE_POWERUP",
    "brake": "SLIDE", 
    "accelerate": "JUMP",
    "turn": "MOVE_LEFT"
  }
}
```

## How to obtain these files:

### OpenNLP Models:
1. Visit https://opennlp.apache.org/models.html
2. Download the English models listed above
3. Place them in the `assets/opennlp/` folder

### TensorFlow Lite Models:
1. Train your own gesture recognition model using TensorFlow
2. Convert to TensorFlow Lite format (.tflite)
3. Or use pre-trained models from MediaPipe or similar frameworks

### Note:
The application will work without these files but with reduced functionality:
- NLP will fall back to basic keyword matching
- Gesture recognition will use simplified OpenCV-based detection