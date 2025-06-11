# Required Assets

This directory should contain the following files for the app to function properly:

## OpenNLP Models (for NLPProcessor)
Place these files in `app/src/main/assets/opennlp/`:
- en-sent.bin (sentence detection model)
- en-token.bin (tokenization model)  
- en-pos-maxent.bin (part-of-speech tagging model)
- en-ner-person.bin (person name recognition model)
- en-ner-location.bin (location name recognition model)

## TensorFlow Lite Models (for MLModelManager)
Place these files in `app/src/main/assets/`:
- hand_landmark_full.tflite (hand landmark detection model)

## Game Training Data (for ObjectDetectionEngine)
Place game screenshots in `app/src/main/assets/subway_surfers_frames/`:
- Various PNG/JPG screenshots for training object detection

## Custom Configuration Files (optional)
- custom_action_mappings.json (custom NLP action mappings)
- custom_object_templates.json (custom game object templates)

Download these models from their respective official sources:
- OpenNLP models: https://opennlp.apache.org/models.html
- MediaPipe hand model: https://github.com/google/mediapipe